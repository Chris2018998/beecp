/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.queue;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

/**
 * ConcurrentLinkedQueue impl
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ConcurrentLinkedQueue2<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {
    private final static Unsafe U;
    private final static long itemOffSet;
    private final static long nextOffSet;
    private final static long tailOffSet;

    static {
        try {
            Field uField = Unsafe.class.getDeclaredField("theUnsafe");
            uField.setAccessible(true);
            U = (Unsafe) uField.get(null);
            itemOffSet = U.objectFieldOffset(Node.class.getDeclaredField("value"));
            nextOffSet = U.objectFieldOffset(Node.class.getDeclaredField("next"));
            tailOffSet = U.objectFieldOffset(ConcurrentLinkedQueue2.class.getDeclaredField("tail"));
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    private transient final Node<E> head = new Node<>(null);
    private transient volatile Node<E> tail = null;

    public static void main(String[] args) {
        ConcurrentLinkedQueue2 queue = new ConcurrentLinkedQueue2();
        for (int i = 1; i < 10; i++) {
            queue.offer(i);
        }
        queue.remove(6);
        queue.remove(9);
        queue.remove(1);
        for (int i = 1; i < 10; i++) {
            System.out.println(queue.poll());
        }
    }

    private boolean abandonNodeValue(Node<E> node, E cmp) {
        return U.compareAndSwapObject(node, itemOffSet, cmp, null);
    }

    private boolean casNodeNext(Node<E> node, Node<E> cmp, Node<E> val) {
        return U.compareAndSwapObject(node, nextOffSet, cmp, val);
    }

    private boolean setAsNewTail(Node<E> curTail, Node<E> newTail) {
        return U.compareAndSwapObject(this, tailOffSet, curTail, newTail);
    }

    public boolean add(E e) {
        return offer(e);
    }

    /**
     * append to tail.next or set as a new tail directly
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        final Node<E> node = new Node<>(e);

        while (true) {
            Node<E> t = tail;
            if (t == null) {//means tail not set
                if (setAsNewTail(t, node)) {
                    head.next = node;
                    break;
                }
            } else if (t.value != null && casNodeNext(t, null, node)) {//append to tail.next
                setAsNewTail(t, node);//try to set as new tail
                break;
            }
        }
        return true;
    }

    /**
     * Lookup a valid node,then remove from chain and return its value
     */
    public E poll() {
        E value = null;
        Node<E> node = null;

        //step1; find out a valid node and break loop
        for (Node<E> curNode = head.next; curNode != null; curNode = curNode.next) {
            value = curNode.value;
            if (value != null && abandonNodeValue(curNode, value)) {//remark as removed
                node = curNode;
                break;
            }
        }

        //step2: node handle
        if (node != null) {//found
            casNodeNext(head, head.next, node.next);//remove from chain
            if (head.next == null) tail = null;
            return value;
        } else if (value != null) {//means all node is invalid then clean
            head.next = null;
            tail = null;
        }
        return null;
    }

    public boolean remove(Object o) {
        if (o == null) throw new NullPointerException();
        for (Node<E> preNode = head, curNode = head.next; curNode != null; preNode = curNode, curNode = curNode.next) {
            E nodeValue = curNode.value;
            if (nodeValue != null && (nodeValue == o || nodeValue.equals(o)) && abandonNodeValue(curNode, nodeValue)) {//remark as abandon
                casNodeNext(preNode, curNode, curNode.next);
                return true;
            }
        }

        return false;
    }

    public boolean contains(Object o) {
        if (o == null) throw new NullPointerException();
        for (Node<E> curNode = head.next; curNode != null; curNode = curNode.next) {
            E nodeValue = curNode.value;
            if (nodeValue != null && (nodeValue == o || nodeValue.equals(o))) return true;
        }
        return false;
    }

    public E peek() {
        for (Node<E> curNode = head.next; curNode != null; curNode = curNode.next) {
            E value = curNode.value;
            if (value != null) return value;
        }
        return null;
    }

    public int size() {
        int size = 0;
        for (Node node = head.next; node.value != null; node = node.next)
            size++;
        return size;
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    private static class Node<E> {
        volatile E value;
        volatile Node<E> next;//null means abandon and need remove

        private Node(E value) {
            this.value = value;
        }
    }

    private class Itr implements Iterator<E> {
        private Node<E> curNode;
        private Node<E> nextNode;

        public boolean hasNext() {
            //@todo
            return false;
        }

        public E next() {
            //@todo
            return null;
        }

        public void remove() {//remark curNode as remove
            abandonNodeValue(curNode, curNode.value);
        }
    }
}
