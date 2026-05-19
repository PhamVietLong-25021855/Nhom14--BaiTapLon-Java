package userauth.service;

import userauth.model.AuctionItem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Cache layer dành cho các dữ liệu auction ít thay đổi.
 *
 * Mục đích:
 * - Cache dữ liệu announcement/metadata có TTL ngắn (1-3 giây)
 * - Giảm database queries cho dữ liệu ít thay đổi
 * - Cải thiện performance cho các danh sách từ repository
 *
 * Chiến lược cache:
 * - OPEN/RUNNING auctions: cache 2 giây (thay đổi khi có bid hoặc status change)
 * - FINISHED auctions: cache 5 giây (ít thay đổi)
 * - Auctions by seller: cache 3 giây (ít thay đổi)
 */
public class AuctionCache {
    private static class CacheEntry<T> {
        private final T data;
        private final long expiresAt;

        CacheEntry(T data, long ttlMs) {
            this.data = data;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final ConcurrentHashMap<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Lưu dữ liệu vào cache với TTL (time-to-live) theo milliseconds.
     */
    public <T> void put(String key, T data, long ttlMs) {
        lock.writeLock().lock();
        try {
            cache.put(key, new CacheEntry<>(data, ttlMs));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lấy dữ liệu từ cache, trả về null nếu hết hạn hoặc không tồn tại.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        lock.readLock().lock();
        try {
            CacheEntry<?> entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                // Upgrade to write lock để xóa expired entry
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // Double-check lại vì có thể thread khác đã xóa
                    if (cache.get(key) == entry) {
                        cache.remove(key);
                    }
                    return null;
                } finally {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                }
            }
            return (T) entry.data;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Xóa tất cả các entry liên quan tới một auction (khi auction bị thay đổi)
     * để trigger làm mới cache.
     */
    public void invalidateForAuction(int auctionId) {
        lock.writeLock().lock();
        try {
            cache.keySet().removeIf(key ->
                key.startsWith("auctions_") ||
                key.contains("auction_" + auctionId)
            );
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Xóa tất cả các entry trong cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lấy kích thước cache hiện tại (chỉ để monitoring).
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}

