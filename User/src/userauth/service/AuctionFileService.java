package userauth.service;

import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.BidTransaction;
import userauth.utils.ConsoleUI;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionFileService {
    private static final String AUCTION_FILE_PATH = "User/data/auctions.txt";
    private static final String BID_FILE_PATH = "User/data/bids.txt";

    public List<AuctionItem> loadAuctionsFromFile() {
        List<AuctionItem> auctions = new ArrayList<>();
        File file = new File(AUCTION_FILE_PATH);

        try {
            ensureFileExists(file);
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(",", -1);
                    if (parts.length < 13) continue; // Must match toString: 13 parts
                    
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        String name = parts[1].trim();
                        String desc = parts[2].trim();
                        double startPrice = Double.parseDouble(parts[3].trim());
                        double currentHighest = Double.parseDouble(parts[4].trim());
                        long startTime = Long.parseLong(parts[5].trim());
                        long endTime = Long.parseLong(parts[6].trim());
                        String category = parts[7].trim();
                        long createdAt = Long.parseLong(parts[8].trim());
                        long updatedAt = Long.parseLong(parts[9].trim());
                        int sellerId = Integer.parseInt(parts[10].trim());
                        int winnerId = Integer.parseInt(parts[11].trim());
                        AuctionStatus status = AuctionStatus.valueOf(parts[12].trim().toUpperCase());
                        
                        auctions.add(new AuctionItem(id, name, desc, startPrice, currentHighest, startTime, endTime, category, createdAt, updatedAt, sellerId, winnerId, status));
                    } catch (NumberFormatException e) {
                        System.out.println("Lỗi parse dòng auction: " + line);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi đọc file auctions.txt: " + e.getMessage());
        }
        return auctions;
    }

    public void saveAuctionsToFile(List<AuctionItem> auctions) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(AUCTION_FILE_PATH)))) {
            for (AuctionItem a : auctions) {
                bw.write(a.toString());
                bw.newLine();
            }
        } catch (Exception e) {
            System.out.println("Lỗi ghi file auctions.txt: " + e.getMessage());
        }
    }

    public List<BidTransaction> loadBidsFromFile() {
        List<BidTransaction> bids = new ArrayList<>();
        File file = new File(BID_FILE_PATH);

        try {
            ensureFileExists(file);
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(",", -1);
                    if (parts.length < 6) continue;
                    
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        int auctionId = Integer.parseInt(parts[1].trim());
                        int bidderId = Integer.parseInt(parts[2].trim());
                        double amount = Double.parseDouble(parts[3].trim());
                        long timestamp = Long.parseLong(parts[4].trim());
                        String status = parts[5].trim();
                        
                        bids.add(new BidTransaction(id, auctionId, bidderId, amount, timestamp, status));
                    } catch (NumberFormatException e) {
                        System.out.println("Lỗi parse dòng bid: " + line);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi đọc file bids.txt: " + e.getMessage());
        }
        return bids;
    }

    public void saveBidsToFile(List<BidTransaction> bids) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(BID_FILE_PATH)))) {
            for (BidTransaction b : bids) {
                bw.write(b.toString());
                bw.newLine();
            }
        } catch (Exception e) {
            System.out.println("Lỗi ghi file bids.txt: " + e.getMessage());
        }
    }

    private void ensureFileExists(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }
}
