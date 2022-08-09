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

    private final transient Node<E> head = new Node<>(null);
    private transient volatile Node<E> tail = null;

    private boolean removeNodeValue(Node<E> node, E cmp) {
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
            if (t == null || t.value == null) {//means tail not set or remark as removed
                if (setAsNewTail(t, node)) {
                    if (head.next == null) casNodeNext(head, null, node);//link to head
                    break;
                }
            } else if (casNodeNext(t, null, node)) {//append to tail.next
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
        E targetValue = null;
        Node<E> targetNode = null;

        //step1; find out a valid node and break loop
        for (Node<E> curNode = head.next; curNode != null; curNode = curNode.next) {
            targetValue = curNode.value;
            if (targetValue != null && removeNodeValue(curNode, targetValue)) {//remark as removed
                targetNode = curNode;
                break;
            }
        }

        //step2; node handle
        if (targetNode == null) {//not found valid node
            casNodeNext(head, head.next, null);//head link to null;
            setAsNewTail(tail, null);//remove tail
        } else if (casNodeNext(head, head.next, targetNode.next)) {// remove the target node from chain if found
            if (head.next == null) setAsNewTail(tail, null);//remove tail
        }

        return targetValue;
    }

    public boolean remove(Object o) {
        boolean removed = false;//means remark as removed

        for (Node<E> preNode = head, curNode = head.next; curNode != null; preNode = curNode, curNode = curNode.next) {
            E nodeValue = curNode.value;
            if (nodeValue != null && (nodeValue == o || nodeValue.equals(o)) && removeNodeValue(curNode, nodeValue)) {//remark as removed
                removed = true;
                casNodeNext(preNode, curNode, curNode.next);
                break;
            }
        }

        return removed;
    }

    public E peek() {
        for (Node<E> preNode = head, curNode = head.next; curNode != null; preNode = curNode, curNode = curNode.next) {
            E nodeValue = curNode.value;
            if (nodeValue != null) return nodeValue;
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
        volatile Node<E> next;

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
            removeNodeValue(curNode, curNode.value);
        }
    }
}
