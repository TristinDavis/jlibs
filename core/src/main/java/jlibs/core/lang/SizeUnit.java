/**
 * Copyright 2015 Santhosh Kumar Tekuri
 *
 * The JLibs authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package jlibs.core.lang;

/**
 * @author Santhosh Kumar T
 */
public enum SizeUnit implements Count.Unit{
    BYTES(1024), KB(1024), MB(1024), GB(0);

    private int count;

    private SizeUnit(int count){
        this.count = count;
    }

    @Override
    public int count(){
        return count;
    }
}
