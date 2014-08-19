/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Collector Core.
 * 
 * Norconex Collector Core is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Collector Core is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Collector Core. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.collector.core.ref.store.impl.mapdb;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

import org.mapdb.DB;

import com.norconex.collector.core.ref.IReference;

public class MappedQueue<T extends IReference> implements Queue<T> {

    private final Queue<String> queue;
    private final Map<String, T> map;

    public MappedQueue(DB db, String name, boolean create) {
        super();
        if (create) {
            queue = db.createQueue(name + "-q", null, true);
            map = db.createHashMap(name + "-m").counterEnable().make();
        } else {
            queue = db.getQueue(name + "-q");
            map = db.getHashMap(name + "-m");
        }
    }
    @Override
    public int size() {
        return map.size();
    }
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
    @Override
    public void clear() {
        queue.clear();
        map.clear();
    }
    @Override
    public boolean offer(T reference) {
        if (queue.offer(reference.getReference())) {
            map.put(reference.getReference(), reference);
            return true;
        }
        return false;
    }
    @Override
    public T remove() {
        String reference = queue.remove();
        return map.remove(reference);
    }
    @Override
    public T poll() {
        String reference = queue.poll();
        if (reference != null) {
            return map.remove(reference);
        }
        return null;
    }
    @Override
    public T element() {
        String reference = queue.element();
        return map.get(reference);
    }
    @Override
    public T peek() {
        String reference = queue.peek();
        if (reference != null) {
            return map.get(reference);
        }
        return null;
    }
    @Override
    public boolean contains(Object o) {
        if (o instanceof String) {
            return map.containsKey((String) o);
        }
        if (o instanceof IReference) {
            return map.containsKey(((IReference) o).getReference());
        }
        return false;
    }
    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("iterator() not supported.");
    }
    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("toArray() not supported.");
    }
    @Override
    public <A> A[] toArray(A[] a) {
        throw new UnsupportedOperationException(
                "toArray(A[] a) not supported.");
    }
    @Override
    public boolean remove(Object o) {
        if (o instanceof String) {
            boolean present = queue.remove(o);
            if (present) {
                map.remove(o);
            }
            return present;
        }
        if (o instanceof IReference) {
            String reference = ((IReference) o).getReference();
            boolean present = queue.remove(reference);
            if (present) {
                map.remove(reference);
            }
            return present;
        }
        return false;
    }
    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }
    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("addAll(...) not supported.");
    }
    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException(
                "removeAll(...) not supported.");
    }
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException(
                "retainAll(...) not supported.");
    }
    @Override
    public boolean add(T e) {
        if (e == null) {
            return false;
        }
        boolean changed = queue.add(e.getReference());
        map.put(e.getReference(), e);
        return changed;
    }
}
