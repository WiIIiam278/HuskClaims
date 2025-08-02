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

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.locks.StampedLock;

/**
 * A concurrent map that implements the fastutil {@link Long2ObjectMap} interface.
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
 * {@code values()}, and {@code long2ObjectEntrySet()}&mdash;are
 * <strong>weakly consistent</strong>. They do not provide an atomic, point-in-time
 * snapshot of the map. The returned collections may reflect a state that was
 * assembled by iterating through the buckets one at a time.
 *
 * @param <V> the type of mapped values
 */
public class ConcurrentLong2ObjectMap<V> implements Long2ObjectMap<V> {

    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final Bucket<V>[] buckets;

    private static class Bucket<V> {
        final Long2ObjectOpenHashMap<V> map;
        final StampedLock lock;

        Bucket(int initialCapacity, float loadFactor) {
            this.map = new Long2ObjectOpenHashMap<>(initialCapacity, loadFactor);
            this.lock = new StampedLock();
        }
    }

    @SuppressWarnings("unchecked")
    public ConcurrentLong2ObjectMap(int initialCapacity, float loadFactor) {
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
    public ConcurrentLong2ObjectMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new, empty map with a default initial capacity and load factor.
     */
    public ConcurrentLong2ObjectMap() {
        this(16, DEFAULT_LOAD_FACTOR);
    }

    private Bucket<V> getBucket(long key) {
        int h = Long.hashCode(key);
        int spreadHash = h ^ (h >>> 16);
        int index = (buckets.length - 1) & spreadHash;
        return buckets[index];
    }

    @Override
    public V put(long key, V value) {
        Bucket<V> bucket = getBucket(key);
        long stamp = bucket.lock.writeLock();
        try {
            return bucket.map.put(key, value);
        } finally {
            bucket.lock.unlockWrite(stamp);
        }
    }

    @Override
    public V get(long key) {
        Bucket<V> bucket = getBucket(key);
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
    public V remove(long key) {
        Bucket<V> bucket = getBucket(key);
        long stamp = bucket.lock.writeLock();
        try {
            return bucket.map.remove(key);
        } finally {
            bucket.lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean containsKey(long key) {
        Bucket<V> bucket = getBucket(key);
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
        for (Bucket<V> bucket : buckets) {
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
    public void clear() {
        for (Bucket<V> bucket : buckets) {
            long stamp = bucket.lock.writeLock();
            try {
                bucket.map.clear();
            } finally {
                bucket.lock.unlockWrite(stamp);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        for (Bucket<V> bucket : buckets) {
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
        for (Bucket<V> bucket : buckets) {
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
    public void putAll(@NotNull Map<? extends Long, ? extends V> m) {
        for (Map.Entry<? extends Long, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
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
    public ObjectSet<Entry<V>> long2ObjectEntrySet() {
        ObjectOpenHashSet<Entry<V>> entrySet = new ObjectOpenHashSet<>();
        for (Bucket<V> bucket : buckets) {
            long stamp = bucket.lock.readLock();
            try {
                entrySet.addAll(bucket.map.long2ObjectEntrySet());
            } finally {
                bucket.lock.unlockRead(stamp);
            }
        }
        return entrySet;
    }

    @Override
    @NotNull
    public LongSet keySet() {
        LongOpenHashSet keySet = new LongOpenHashSet();
        for (Bucket<V> bucket : buckets) {
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
        for (Bucket<V> bucket : buckets) {
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