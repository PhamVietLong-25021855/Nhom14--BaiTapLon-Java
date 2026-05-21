-- Database indexes for auction system optimization
-- Run this script on your MySQL database to add performance indexes

-- Indexes for auctions table
CREATE INDEX idx_auctions_status_time ON auctions(status, start_time, end_time);
CREATE INDEX idx_auctions_seller ON auctions(seller_id);

-- Indexes for bids table
CREATE INDEX idx_bids_auction_time ON bids(auction_id, bid_time);

-- Indexes for auto_bids table
CREATE INDEX idx_auto_bids_auction ON auto_bids(auction_id);
CREATE INDEX idx_auto_bids_bidder ON auto_bids(bidder_id);
CREATE UNIQUE INDEX ux_auto_bids_auction_bidder ON auto_bids(auction_id, bidder_id);

-- Indexes for topup_transactions table
CREATE INDEX idx_topup_user_time ON topup_transactions(user_id, transaction_time DESC);

-- Indexes for wallet_transactions table (audit log)
CREATE INDEX idx_wallet_tx_user_time ON wallet_transactions(user_id, created_at DESC);
