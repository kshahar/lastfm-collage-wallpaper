package org.kwimbo.lastfm;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/** Provides a circular iterator on top of a list, with a bounded size for iteration.
 */
public class RingIterator<T> implements Iterator<T> {
    private List<T> list = null;
    private int listSize;
    private int size;
    private int nextCount = 0;
    private int current = 0;

    protected RingIterator(List<T> list, int size) {
        this.list = list;
        this.listSize = list.size();
        this.size = size;
    }

    public boolean hasNext() {
        return nextCount < size;
    }

    public T next() {
        if (!hasNext())
            throw new NoSuchElementException();
        int i = current++;
        nextCount++;
        current = current % listSize;
        return list.get(i);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
