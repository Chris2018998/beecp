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

/**
 * ConcurrentLinkedQueue2,A FIFO unbounded queue impl based on linked nodes,which has a fixed head node and kept tail node(not remove)
 *
 * 1: snapshot at queue creation
 * <pre>
 *  +--------+
 *  |  head  | next --------> null
 *  +--------+
 * </pre>
 *
 * 2:snapshot at queue offer two elements(node box contains element value)
 * <pre>
 *   +------+                +-----+                  +----- +
 *   | head | next --------> | first| next -------->  | tail | next --------> null
 *   +------+                +-----+                  +----- +
 * </pre>
 *
 * 3:snapshot at queue remove tail node (just clear node value and set to null,then kept as empty box node)
 * <pre>
 *   +------+                +-----+                  +-----------------+
 *   | head | next --------> | first| next -------->  | tail(value==null)| next --------> null
 *   +------+                +-----+                  +------------------+
 * </pre>
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

    //***************************************************************************************************************//
    //                                          3: CAS Methods                                                       //
    //***************************************************************************************************************//
    private static void lazySetNext(Node preNode, Node nextNode) {
        U.putOrderedObject(preNode, nextOffSet, nextNode);
    }

    private static boolean setNodeValueToNull(Node node, Object cmp) {
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
            if (t != null) {
                if (casNodeNext(t, null, node)) {//append to tail.next
                    setAsNewTail(this, t, node);//try to set as new tail
                    return true;
                }
            } else if (setAsNewTail(this, null, node)) {
                head.next = node;
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
        for (Node<E> node = head.next; node != null; node = node.next) {//must exists tail
            E value = node.value;
            if (value != null && setNodeValueToNull(node, value)) {//mark as abandon(need removed)
                if (node != tail) casNodeNext(head, head.next, node.next);
                return value;
            }
        }//loop for
        return null;
    }

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public E peek() {
        for (Node<E> node = head.next; node != null; node = node.next) {//must exists tail
            E value = node.value;
            if (value != null) {
                Node<E> headNext = head.next;
                if (headNext != node) casNodeNext(head, headNext, node);//head link to peek node
                return value;
            }
        }//loop for
        return null;
    }

    //***************************************************************************************************************//
    //                                          5: Collection Methods                                                //
    //***************************************************************************************************************//

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
     * <p>
     * if need remove element is null then throws NullPointerException
     */
    public boolean remove(Object o) {
        if (o == null) throw new NullPointerException();
        //if o==tail.value then set value to null and keep in chain，it will be remove util become a empty middle node
		
        //@todo need re-think logic
//        for (Node<E> preNode = head, curNode = head.next; curNode != null; preNode = curNode, curNode = curNode.next) {
//            E value = curNode.value;
//            if (value != null) {
//                if ((value == o || value.equals(o)) && setNodeValueToNull(curNode, value)) {//mark as abandon
//                    if (preNode.value != null) casNodeNext(preNode, curNode, curNode.next);//remove current node
//                    if (head.next == null) tail = null;
//                    return true;
//                }
//            } else if (casNodeNext(head, head.next, curNode.next)) {//remove invalid node
//                if (head.next == null) tail = null;
//            }
//        }

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
        volatile Node<E> next;//null means abandon and need remove from chain,exclude tail

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
            setNodeValueToNull(curNode, curNode.value);
        }
    }
}
