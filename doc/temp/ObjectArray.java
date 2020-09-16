import com.zaxxer.hikari.util.FastList;

import java.util.ArrayList;

public class ObjectArray {
    private int size;
    private int initSize;
    private Object[] elements;

    public ObjectArray(int initSize) {
        elements = new Object[this.initSize = initSize];
    }

    public int size() {
        return size;
    }

    public void add(Object e) {
        if (size >= elements.length) {
            Object[] newArray = new Object[elements.length + initSize];
            System.arraycopy(elements, 0, newArray, 0, elements.length);
            elements = newArray;
        }
        elements[size++] = e;
    }

    public void remove(Object o) {
        for (int i = 0; i < size; i++)
            if (o == elements[i]) {
                int m = size - i - 1;
                if (m > 0) System.arraycopy(elements, i + 1, elements, i, m);
                elements[--size] = null; // clear to let GC do its work
                return;
            }
    }

    public void clear() {
        for (int i = 0; i < size; i++)
            elements[i] = null;
        if (size > initSize) elements = new Object[initSize];
    }


    public static void main(String[] args) {
        int loop = 1000000;
        ArrayList<Object> arrayList = new ArrayList<Object>();
        FastList<Object> fastList = new FastList(Object.class);
        ObjectArray array = new ObjectArray(16);

        Object[] values = new Object[50];
        for (int i = 0; i < values.length; i++) {
            values[i] = "a" + i;
        }

        long time1 = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            for (Object o : values) {
                arrayList.add(o);
            }
            for (Object o : values) {
                arrayList.remove(o);
            }
        }

        long time2 = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            for (Object o : values) {
                fastList.add(o);
            }
            for (Object o : values) {
                fastList.remove(o);
            }
        }
        long time3 = System.currentTimeMillis();

        for (int i = 0; i < loop; i++) {
            for (Object o : values) {
                array.add(o);
            }
            for (Object o : values) {
                array.remove(o);
            }
        }
        long time4 = System.currentTimeMillis();

        System.out.println("Add count:" + loop + ",remove count:" + loop);
        System.out.println("ArrayList Time:" + (time2 - time1));
        System.out.println("FastList Time:" + (time3 - time2));
        System.out.println("ObjectArray Time:" + (time4 - time3));
    }
}
