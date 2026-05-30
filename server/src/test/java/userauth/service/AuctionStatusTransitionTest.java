package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionStatusTransitionTest {
    @Test
    void getAllAuctionsPromotesDueOpenAuctionToRunning() {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        AuctionService auctionService = new AuctionService(auctionDAO, new ServiceTestSupport.EmptyAutoBidDAO());

        int auctionId = 401;
        auctionDAO.putAuction(openAuctionReadyToRun(auctionId));

        List<AuctionItem> auctions = auctionService.getAllAuctions();

        assertEquals(AuctionStatus.RUNNING, auctionDAO.findAuctionById(auctionId).getStatus());
        assertEquals(AuctionStatus.RUNNING, auctions.getFirst().getStatus());
    }

    @Test
    void placeBidPromotesDueOpenAuctionBeforeValidatingStatus() throws Exception {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        AuctionService auctionService = new AuctionService(auctionDAO, new ServiceTestSupport.EmptyAutoBidDAO());

        int auctionId = 402;
        int bidderId = 7;
        auctionDAO.putAuction(openAuctionReadyToRun(auctionId));

        auctionService.placeBid(auctionId, bidderId, 150_000.0);

        AuctionItem storedAuction = auctionDAO.findAuctionById(auctionId);
        assertEquals(AuctionStatus.RUNNING, storedAuction.getStatus());
        assertEquals(bidderId, storedAuction.getWinnerId());
        assertEquals(150_000.0, storedAuction.getCurrentHighestBid());
    }

    private AuctionItem openAuctionReadyToRun(int id) {
        long now = System.currentTimeMillis();
        return new AuctionItem(
                id,
                "Auction " + id,
                "Ready auction",
                100_000.0,
                100_000.0,
                now - 1_000L,
                now + 60_000L,
                "Test",
                null,
                null,
                now,
                now,
                5,
                -1,
                AuctionStatus.OPEN,
                0
        );
    }
}
