package com.github.jmatss.torc.util.com;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockableHashMap<K, V> extends HashMap<K, V> implements AutoCloseable {
    public static final int DEFAULT_CAPACITY = 16;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final Lock mutex;

    public LockableHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        this.mutex = new ReentrantLock();
    }

    public LockableHashMap() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public LockableHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    // Implements AutoClosable so that it can be locked/unlocked with a "try-with-resources".
    @Override
    public void close() {
        this.mutex.unlock();
    }

    // Returns a AutoClosable instead of the regular void so that it can be used in a "try-with-resources".
    public LockableHashMap<K, V> lock() {
        this.mutex.lock();
        return this;
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return super.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return super.get(key);
    }

    @Override
    public V put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return super.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        super.putAll(m);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public Set<K> keySet() {
        return super.keySet();
    }

    @Override
    public Collection<V> values() {
        return super.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return super.entrySet();
    }
}
