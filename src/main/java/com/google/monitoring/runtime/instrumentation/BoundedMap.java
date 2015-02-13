package com.google.monitoring.runtime.instrumentation;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BoundedMap<K, V> {

    private static final ThreadLocal<Random> RANDOM = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };

    private final ConcurrentHashMap<K, V> delegate;
    private final int threshold;

    public BoundedMap(int initialCapacity, double pruneRatio) {
        delegate = new ConcurrentHashMap<K, V>(initialCapacity);
        threshold = (int) (initialCapacity * pruneRatio);
    }

    // The approximate current size of the map
    private AtomicInteger approxSize = new AtomicInteger();

    public V put(K key, V value) {
        if (approxSize.get() >= threshold) {
            // if we have too many elements, delete ~10%.
            // expensive, but keeps the map bounded
            // randomness avoids spinning on add/delete (to a certain extent)
            Random random = RANDOM.get();
            for (Iterator<K> it = delegate.keySet().iterator(); it.hasNext();) {
                it.next();
                if (random.nextDouble() < 0.1) {
                    it.remove();
                }
            }

            // correct approximateSize every once in a while
            // also expensive
            approxSize.set(delegate.size());
        }

        approxSize.incrementAndGet();
        return delegate.put(key, value);
    }

    public V get(K key) {
        return delegate.get(key);
    }
}
