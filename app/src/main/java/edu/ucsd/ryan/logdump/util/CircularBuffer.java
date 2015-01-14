package edu.ucsd.ryan.logdump.util;

import java.util.AbstractCollection;
import java.util.Iterator;

/**
 * Created by ryan on 1/13/15.
 */
public class CircularBuffer extends AbstractCollection {
    private int begin;
    private int count;
    private int maxSize;
    private Object[] buffer;

    public CircularBuffer(int size) {
        maxSize = size;
        buffer = new Object[size];
        begin = 0;
        count = 0;
    }

    @Override
    public boolean add(Object item) {
        if (isFull()) {
            remove();
        }
        buffer[circularIndex(begin + count)] = item;
        count++;
        return true;
    }

    public Object remove() {
        if (isEmpty())
            return null;
        Object item = buffer[begin];
        if (item != null) {
            buffer[begin] = null;
            begin = circularIndex(begin + 1);
            count--;
        }
        return item;
    }

    public void clear() {
        int end = begin + count;
        for (int i = begin; begin < end; ++i) {
            buffer[circularIndex(i)] = null;
        }
        begin = 0;
        count = 0;
    }

    public Object head() {
        return head(0);
    }

    public Object tail(int index) {
        return head(count - 1 - index);
    }

    public Object head(int index) {
        index += begin;
        if (inRange(index))
            return buffer[circularIndex(index)];
        return null;
    }

    public int circularIndex(int absIndex) {
        return absIndex % maxSize;
    }

    public boolean inRange(int absIndex) {
        return absIndex >= begin && absIndex < begin + count;
    }

    public boolean isFull() {
        return count == maxSize;
    }

    private class CircularIterator implements Iterator {
        private int index = begin - 1;

        @Override
        public boolean hasNext() {
            int n = index + 1;
            int distance = (begin + count - n);
            return  count != 0 && distance != maxSize;
        }

        @Override
        public Object next() {
            if (!hasNext())
                return null;
            index = circularIndex(index + 1);
            return buffer[index];
        }

        @Override
        public void remove() {
            //TODO: we might consider support this
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator iterator() {
        return new CircularIterator();
    }

    @Override
    public int size() {
        return count;
    }
}

