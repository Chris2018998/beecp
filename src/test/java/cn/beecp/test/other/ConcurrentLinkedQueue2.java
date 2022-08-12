/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.other;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ConcurrentLinkedQueue impl
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ConcurrentLinkedQueue2<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {
    //***************************************************************************************************************//
    //                                           1: CAS Chain info                                                   //
    //***************************************************************************************************************//
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
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private transient final Node<E> head = new Node<E>(null);//fixed head
    private transient volatile Node<E> tail = null;

    //***************************************************************************************************************//
    //                                          2: Constructors                                                      //
    //***************************************************************************************************************//
    public ConcurrentLinkedQueue2() {
        //do nothing
    }

    public ConcurrentLinkedQueue2(Collection<? extends E> c) {
        if (c != null && c.size() > 0) {
            Node<E> preNode = head;
            for (E e : c) {
                if (e == null) throw new NullPointerException();
                Node<E> newNode = new Node<>(e);
                lazySetNext(preNode, newNode);
                preNode = newNode;
            }
            this.tail = preNode;
        }
    }

    public static void main(String[] args) {
        int size = 1000;
        ConcurrentLinkedQueue<Integer> queue1 = new ConcurrentLinkedQueue<Integer>();
        ConcurrentLinkedQueue2<Integer> queue2 = new ConcurrentLinkedQueue2<Integer>();
        long time1 = System.nanoTime();
        for (int i = 1; i < size; i++) {
            queue2.offer(i);
        }
        long time2 = System.nanoTime();
        for (int i = 1; i < size; i++) {
            queue1.offer(i);
        }
        long time3 = System.nanoTime();
        System.out.println("ConcurrentLinkedQueue2 offer time:" + (time2 - time1));
        System.out.println("ConcurrentLinkedQueue1 offer time:" + (time3 - time2));

        long time4 = System.nanoTime();
        for (int i = 1; i < size; i++) {
            queue2.poll();
        }
        long time5 = System.nanoTime();
        for (int i = 1; i < size; i++) {
            queue1.poll();
        }
        long time6 = System.nanoTime();
        System.out.println("ConcurrentLinkedQueue2 poll time:" + (time5 - time4));
        System.out.println("ConcurrentLinkedQueue1 poll time:" + (time6 - time5));
    }

    //***************************************************************************************************************//
    //                                          3: CAS Methods                                                       //
    //***************************************************************************************************************//
    private static void lazySetNext(Node preNode, Node nextNode) {
        U.putOrderedObject(preNode, nextOffSet, nextNode);
    }

    private static boolean abandonNodeValue(Node node, Object cmp) {
        return U.compareAndSwapObject(node, itemOffSet, cmp, null);
    }

    private static boolean casNodeNext(Node preNode, Node next, Node newNext) {
        return U.compareAndSwapObject(preNode, nextOffSet, next, newNext);
    }

    private static boolean setAsNewTail(ConcurrentLinkedQueue2 q, Node curTail, Node newTail) {
        return U.compareAndSwapObject(q, tailOffSet, curTail, newTail);
    }

    //***************************************************************************************************************//
    //                                          4: Queue Methods                                                     //
    //***************************************************************************************************************//

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never throw
     * {@link IllegalStateException} or return {@code false}.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        final Node<E> node = new Node<>(e);

        for (; ; ) {
            Node<E> t = tail;
            if (t == null) {//means tail not set
                if (setAsNewTail(this, null, node)) {
                    head.next = node;
                    return true;
                }
            } else if (t.value != null && casNodeNext(t, null, node)) {//append to tail.next
                setAsNewTail(this, t, node);//try to set as new tail
                return true;
            }
        }
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public E poll() {
        for (Node<E> node = head.next; node != null; node = node.next) {
            E value = node.value;
            if (value != null) {
                if (abandonNodeValue(node, value)) {//mark as removed
                    if (casNodeNext(head, head.next, node.next))//remove current node
                        if (head.next == null) tail = null;
                    return value;
                }
            } else if (casNodeNext(head, head.next, node.next)) {//remove invalid node
                if (head.next == null) tail = null;
            }
        }//for loop
        return null;
    }

    //***************************************************************************************************************//
    //                                          5: Collection Methods                                                //
    //***************************************************************************************************************//

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public E peek() {
        for (Node<E> node = head.next; node != null; node = node.next) {
            E value = node.value;
            if (value != null) {
                return value;
            } else if (casNodeNext(head, head.next, node.next)) {//remove invalid node
                if (head.next == null) tail = null;
            }
        }//for loop
        return null;
    }

    /**
     * @return {@code true} if this queue contains no elements
     */
    public boolean isEmpty() {
        return peek() == null;
    }

    /**
     * Returns valid node size of chain
     */
    public int size() {
        int size = 0;
        for (Node node = head.next; node != null && node.value != null; node = node.next)
            size++;
        return size;
    }

    /**
     * <p>This implementation iterates over the elements in the collection,
     * checking each element in turn for equality with the specified element
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        for (Node<E> node = head.next; node != null; node = node.next) {
            E value = node.value;
            if (value != null && (value == o || o.equals(value))) return true;
        }
        return false;
    }

    /**
     * <p>This implementation iterates over the collection looking for the
     * specified element.  If it finds the element, it removes the element
     * from the collection using the iterator's remove method.
     *
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if the iterator returned by this
     * collection's iterator method does not implement the <tt>remove</tt>
     * method and this collection contains the specified object.
     *
     * @throws NullPointerException
     */
    public boolean remove(Object o) {
        if (o == null) throw new NullPointerException();
        for (Node<E> preNode = head, curNode = head.next; curNode != null; preNode = curNode, curNode = curNode.next) {
            E value = curNode.value;
            if (value != null) {
                if ((value == o || value.equals(o)) && abandonNodeValue(curNode, value)) {//mark as abandon
                    casNodeNext(preNode, curNode, curNode.next);//remove current node
                    if (head.next == null) tail = null;
                    return true;
                }
            } else if (casNodeNext(head, head.next, curNode.next)) {//remove invalid node
                if (head.next == null) tail = null;
            }
        }
        return false;
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this queue, in the order that they are returned by the specified
     * collection's iterator.  Attempts to {@code addAll} of a queue to
     * itself result in {@code IllegalArgumentException}.
     *
     * @param c the elements to be inserted into this queue
     * @return {@code true} if this queue changed as a result of the call
     * @throws NullPointerException     if the specified collection or any
     *                                  of its elements are null
     * @throws IllegalArgumentException if the collection is this queue
     */
    public boolean addAll(Collection<? extends E> c) {
        if (c == this) throw new IllegalArgumentException();
        if (c != null && c.size() > 0) {
            for (E e : c)
                offer(e);
        }
        return true;
    }

    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        List elementList = new LinkedList();
        for (Node node = head.next; node != null; node = node.next) {
            Object value = node.value;
            if (value != null) elementList.add(value);
        }
        return elementList.toArray();
    }

    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array.  If the queue fits in the specified array, it
     * is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this queue.
     *
     * <p>If this queue fits in the specified array with room to spare
     * (i.e., the array has more elements than this queue), the element in
     * the array immediately following the end of the queue is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     * <p>
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this queue
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        if (a == null) throw new NullPointerException();
        int i = 0;
        int arraySize = a.length;
        for (Node node = head.next; node != null; node = node.next) {
            Object value = node.value;
            if (value != null) a[i++] = (T) node.value;
            if (i == arraySize) break;
        }
        return a;
    }

    /**
     * Returns an iterator over the elements contained in this collection.
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    private static class Node<E> {
        volatile E value;
        volatile Node<E> next;//null means abandon and need remove from chain

        Node(E e) {
            this.value = e;
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

        public void remove() {//mark curNode as remove
            abandonNodeValue(curNode, curNode.value);
        }
    }
}
