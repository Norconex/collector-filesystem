/* Copyright 2017 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.collector.fs.crawler;

import java.util.Iterator;

import org.apache.commons.collections4.iterators.ObjectArrayIterator;

public class MockStartPathsProvider implements IStartPathsProvider {

    @Override
    public Iterator<String> provideStartPaths() {
        return new ObjectArrayIterator<>(
                "/path1/",
                "/path2/",
                "/path3/");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return MockStartPathsProvider.class.equals(obj.getClass());
    }
}
