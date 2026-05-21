package userauth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuctionCacheTest {
    @Test
    void putReturnsValueBeforeTtlExpires() {
        AuctionCache cache = new AuctionCache();

        cache.put("auction_1", "cached auction", 1_000L);

        assertEquals("cached auction", cache.get("auction_1"));
        assertEquals(1, cache.size());
    }

    @Test
    void expiredEntryIsRemovedWhenRead() {
        AuctionCache cache = new AuctionCache();

        cache.put("auction_1", "expired auction", -1L);

        assertNull(cache.get("auction_1"));
        assertEquals(0, cache.size());
    }

    @Test
    void invalidateForAuctionRemovesAuctionListsAndMatchingAuctionKeys() {
        AuctionCache cache = new AuctionCache();
        cache.put("auctions_running", "running list", 1_000L);
        cache.put("seller_5_auction_7", "seller item", 1_000L);
        cache.put("auction_7_detail", "detail", 1_000L);
        cache.put("homepage_announcements", "keep", 1_000L);

        cache.invalidateForAuction(7);

        assertNull(cache.get("auctions_running"));
        assertNull(cache.get("seller_5_auction_7"));
        assertNull(cache.get("auction_7_detail"));
        assertEquals("keep", cache.get("homepage_announcements"));
    }

    @Test
    void clearRemovesAllEntries() {
        AuctionCache cache = new AuctionCache();
        cache.put("first", 1, 1_000L);
        cache.put("second", 2, 1_000L);

        cache.clear();

        assertEquals(0, cache.size());
        assertNull(cache.get("first"));
        assertNull(cache.get("second"));
    }
}
