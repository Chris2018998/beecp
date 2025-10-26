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
package org.stone.test.beecp.objects.listener;

import org.stone.beecp.BeeMethodExecutionListener;
import org.stone.beecp.BeeMethodExecutionListenerFactory;

/**
 * Method execution listener factory, for success test
 *
 * @author Chris Liao
 */
public class MockMethodExecutionListenerFactory1 implements BeeMethodExecutionListenerFactory {

    /**
     * Creates method execution listener.
     *
     * @return created Listener instance
     */
    public BeeMethodExecutionListener create() throws Exception {
        return new MockMethodExecutionListener1();
    }
}
