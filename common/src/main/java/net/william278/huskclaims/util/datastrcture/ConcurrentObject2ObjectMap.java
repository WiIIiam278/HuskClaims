/*
 * This file is part of HuskClaims, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskclaims.util.datastrcture;

import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.locks.StampedLock;

/**
 * A concurrent map that implements the fastutil {@link Object2ObjectMap} interface.
 * <p>
 * This implementation uses a lock striping (sharding) strategy with an array of
 * internal buckets, each protected by its own {@link StampedLock}. This allows for
 * high concurrency levels by reducing lock contention, as threads operating on
 * different keys can often work on separate buckets in parallel.
 * <p>
 * For read operations such as {@code get()} and {@code containsKey()}, this class
 * leverages optimistic locking via {@link StampedLock#tryOptimisticRead()}. This
 * provides very high throughput in read-heavy scenarios by avoiding traditional
 * locking overhead when no concurrent writes are detected. If a write does
 * occur during an optimistic read, the operation safely falls back to a
 * pessimistic read lock.
 * <p>
 * <strong>Consistency Guarantees:</strong> Note that aggregate operations that
 * span multiple buckets&mdash;such as {@code size()}, {@code keySet()},
 * {@code values()}, and {@code object2ObjectEntrySet()}&mdash;are
 * <strong>weakly consistent</strong>. They do not provide an atomic, point-in-time
 * snapshot of the map. The returned collections may reflect a state that was
 * assembled by iterating through the buckets one at a time.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ConcurrentObject2ObjectMap<K, V> implements Object2ObjectMap<K, V> {

    /**
     * The default number of buckets (shards) to use for concurrency.
     * A value of 16 is a good default for most server environments.
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final Bucket<K, V>[] buckets;

    private static class Bucket<K, V> {
        final Object2ObjectOpenHashMap<K, V> map;
        final StampedLock lock;

        Bucket(int initialCapacity, float loadFactor) {
            this.map = new Object2ObjectOpenHashMap<>(initialCapacity, loadFactor);
            this.lock = new StampedLock();
        }
    }

    /**
     * Constructs a new map with a given initial total capacity and load factor.
     *
     * @param initialCapacity the initial total capacity for the map (number of expected elements)
     * @param loadFactor      the load factor for the internal maps
     */
    @SuppressWarnings("unchecked")
    public ConcurrentObject2ObjectMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) throw new IllegalArgumentException("Illegal load factor: " + loadFactor);

        int concurrencyLevel = DEFAULT_CONCURRENCY_LEVEL;
        this.buckets = new Bucket[concurrencyLevel];

        int perBucketCapacity = (initialCapacity / concurrencyLevel) + 1;

        for (int i = 0; i < concurrencyLevel; i++) {
            buckets[i] = new Bucket<>(perBucketCapacity, loadFactor);
        }
    }

    /**
     * Constructs a new map with a given initial total capacity and the default load factor (0.75).
     *
     * @param initialCapacity the initial total capacity for the map
     */
    public ConcurrentObject2ObjectMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new, empty map with a default initial capacity (16) and load factor.
     */
    public ConcurrentObject2ObjectMap() {
        this(16, DEFAULT_LOAD_FACTOR);
    }

    private Bucket<K, V> getBucket(Object key) {
        if (key == null) {
            return buckets[0];
        }
        int h = key.hashCode();
        int spreadHash = h ^ (h >>> 16);
        int index = (buckets.length - 1) & spreadHash;
        return buckets[index];
    }


    @Override
    public V put(K key, V value) {
        Bucket<K, V> bucket = getBucket(key);
        long stamp = bucket.lock.writeLock();
        try {
            return bucket.map.put(key, value);
        } finally {
            bucket.lock.unlockWrite(stamp);
        }
    }

    @Override
    public V get(Object key) {
        Bucket<K, V> bucket = getBucket(key);
        long stamp = bucket.lock.tryOptimisticRead();
        V value = bucket.map.get(key);
        if (!bucket.lock.validate(stamp)) {
            stamp = bucket.lock.readLock();
            try {
                value = bucket.map.get(key);
            } finally {
                bucket.lock.unlockRead(stamp);
            }
        }
        return value;
    }

    @Override
    public V remove(Object key) {
        Bucket<K, V> bucket = getBucket(key);
        long stamp = bucket.lock.writeLock();
        try {
            return bucket.map.remove(key);
        } finally {
            bucket.lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        Bucket<K, V> bucket = getBucket(key);
        long stamp = bucket.lock.tryOptimisticRead();
        boolean exists = bucket.map.containsKey(key);
        if (!bucket.lock.validate(stamp)) {
            stamp = bucket.lock.readLock();
            try {
                exists = bucket.map.containsKey(key);
            } finally {
                bucket.lock.unlockRead(stamp);
            }
        }
        return exists;
    }

    @Override
    public int size() {
        int totalSize = 0;
        for (Bucket<K, V> bucket : buckets) {
            long stamp = bucket.lock.readLock();
            try {
                totalSize += bucket.map.size();
            } finally {
                bucket.lock.unlockRead(stamp);
            }
        }
        return totalSize;
    }

    @Override
    public boolean isEmpty() {
        for (Bucket<K, V> bucket : buckets) {
            long stamp = bucket.lock.tryOptimisticRead();
            boolean bucketEmpty = bucket.map.isEmpty();
            if (!bucket.lock.validate(stamp)) {
                stamp = bucket.lock.readLock();
                try {
                    bucketEmpty = bucket.map.isEmpty();
                } finally {
                    bucket.lock.unlockRead(stamp);
                }
            }
            if (!bucketEmpty) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Bucket<K, V> bucket : buckets) {
            long stamp = bucket.lock.readLock();
            try {
                if (bucket.map.containsValue(value)) {
                    return true;
                }
            } finally {
                bucket.lock.unlockRead(stamp);
            }
        }
        return false;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        for (Bucket<K, V> bucket : buckets) {
            long stamp = bucket.lock.writeLock();
            try {
                bucket.map.clear();
            } finally {
                bucket.lock.unlockWrite(stamp);
            }
        }
    }

    @Override
    public void defaultReturnValue(V rv) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V defaultReturnValue() {
        return null;
    }

    @Override
    public ObjectSet<Entry<K, V>> object2ObjectEntrySet() {
        ObjectOpenHashSet<Entry<K, V>> entrySet = new ObjectOpenHashSet<>();
        for (Bucket<K, V> bucket : buckets) {
            long stamp = bucket.lock.readLock();
            try {
                entrySet.addAll(bucket.map.object2ObjectEntrySet());
            } finally {
                bucket.lock.unlockRead(stamp);
            }
        }
        return entrySet;
    }

    @Override
    @NotNull
    public ObjectSet<K> keySet() {
        ObjectOpenHashSet<K> keySet = new ObjectOpenHashSet<>();
        for (Bucket<K, V> bucket : buckets) {
            long stamp = bucket.lock.readLock();
            try {
                keySet.addAll(bucket.map.keySet());
            } finally {
                bucket.lock.unlockRead(stamp);
            }
        }
        return keySet;
    }

    @Override
    @NotNull
    public ObjectCollection<V> values() {
        ObjectArrayList<V> values = new ObjectArrayList<>();
        for (Bucket<K, V> bucket : buckets) {
            long stamp = bucket.lock.readLock();
            try {
                values.addAll(bucket.map.values());
            } finally {
                bucket.lock.unlockRead(stamp);
            }
        }
        return values;
    }
}