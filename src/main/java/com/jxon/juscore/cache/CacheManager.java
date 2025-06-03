// src/main/java/com/jxon/juscore/cache/CacheManager.java
package com.jxon.juscore.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.LinkedHashMap;

public class CacheManager {
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final AtomicLong totalMemoryUsage = new AtomicLong(0);
    private final long memoryLimitBytes;

    public CacheManager(int memoryLimitMB) {
        this.memoryLimitBytes = memoryLimitMB * 1024L * 1024L;
    }

    public byte[][][] getModel(String cacheKey) {
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null) {
            entry.lastAccessTime = System.currentTimeMillis();
            entry.referenceCount++;
            return entry.modelData;
        }
        return null;
    }

    public void putModel(String cacheKey, byte[][][] modelData) {
        if (modelData == null) return;

        long modelSize = calculateModelSize(modelData);

        // 检查是否需要清理缓存
        while (totalMemoryUsage.get() + modelSize > memoryLimitBytes) {
            evictLRU();
        }

        CacheEntry entry = new CacheEntry(modelData, modelSize);
        cache.put(cacheKey, entry);
        totalMemoryUsage.addAndGet(modelSize);
    }

    private void evictLRU() {
        if (cache.isEmpty()) return;

        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            CacheEntry cacheEntry = entry.getValue();
            if (cacheEntry.referenceCount == 0 && cacheEntry.lastAccessTime < oldestTime) {
                oldestTime = cacheEntry.lastAccessTime;
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            CacheEntry removed = cache.remove(oldestKey);
            if (removed != null) {
                totalMemoryUsage.addAndGet(-removed.memorySize);
            }
        }
    }

    private long calculateModelSize(byte[][][] modelData) {
        return (long) modelData.length * modelData[0].length * modelData[0][0].length;
    }

    public void releaseReference(String cacheKey) {
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && entry.referenceCount > 0) {
            entry.referenceCount--;
        }
    }

    public long getMemoryUsage() {
        return totalMemoryUsage.get();
    }

    public int getCacheSize() {
        return cache.size();
    }

    private static class CacheEntry {
        final byte[][][] modelData;
        final long memorySize;
        volatile long lastAccessTime;
        volatile int referenceCount;

        CacheEntry(byte[][][] modelData, long memorySize) {
            this.modelData = modelData;
            this.memorySize = memorySize;
            this.lastAccessTime = System.currentTimeMillis();
            this.referenceCount = 1;
        }
    }
}