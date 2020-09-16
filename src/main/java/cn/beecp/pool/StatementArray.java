/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.pool;

/**
 * Statement array ,Logic copy from <code>java.util.ArrayList</code>
 *
 * @author Chris.Liao
 */
class StatementArray {
    private int size;
    private int initSize;
    private ProxyStatementBase[] elements;

    public StatementArray(int initSize) {
        elements = new ProxyStatementBase[this.initSize = initSize];
    }

    public int size() {
        return size;
    }

    public void add(ProxyStatementBase e) {
        if (size >= elements.length) {
            ProxyStatementBase[] newArray = new ProxyStatementBase[elements.length + initSize];
            System.arraycopy(elements, 0, newArray, 0, elements.length);
            elements = newArray;
        }
        elements[size++] = e;
    }

    public void remove(ProxyStatementBase o) {
        for (int i = 0; i < size; i++)
            if (o == elements[i]) {
                int m = size - i - 1;
                if (m > 0) System.arraycopy(elements, i + 1, elements, i, m);
                elements[--size] = null; // clear to let GC do its work
                return;
            }
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            if (elements[i] != null) {
                elements[i].setAsClosed();
                elements[i] = null;
            }
        }
        if (size > initSize) elements = new ProxyStatementBase[initSize];
    }
}
