package userauth.service;

import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.BidTransaction;
import userauth.utils.ConsoleUI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    AuctionItem auctionItem = parseAuction(line);
                    if (auctionItem != null) {
                        auctions.add(auctionItem);
                    }
                }
            }
        } catch (IOException ex) {
            ConsoleUI.printError("Loi doc file auctions.txt: " + ex.getMessage());
        }

        return auctions;
    }

    public void saveAuctionsToFile(List<AuctionItem> auctions) {
        writeLines(new File(AUCTION_FILE_PATH), auctions.stream().map(AuctionItem::toString).toList(), "auctions.txt");
    }

    public List<BidTransaction> loadBidsFromFile() {
        List<BidTransaction> bids = new ArrayList<>();
        File file = new File(BID_FILE_PATH);

        try {
            ensureFileExists(file);
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    BidTransaction bid = parseBid(line);
                    if (bid != null) {
                        bids.add(bid);
                    }
                }
            }
        } catch (IOException ex) {
            ConsoleUI.printError("Loi doc file bids.txt: " + ex.getMessage());
        }

        return bids;
    }

    public void saveBidsToFile(List<BidTransaction> bids) {
        writeLines(new File(BID_FILE_PATH), bids.stream().map(BidTransaction::toString).toList(), "bids.txt");
    }

    private AuctionItem parseAuction(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 13) {
            ConsoleUI.printWarning("Dong auction khong hop le, bo qua: " + line);
            return null;
        }

        try {
            return new AuctionItem(
                    Integer.parseInt(parts[0].trim()),
                    parts[1].trim(),
                    parts[2].trim(),
                    Double.parseDouble(parts[3].trim()),
                    Double.parseDouble(parts[4].trim()),
                    Long.parseLong(parts[5].trim()),
                    Long.parseLong(parts[6].trim()),
                    parts[7].trim(),
                    Long.parseLong(parts[8].trim()),
                    Long.parseLong(parts[9].trim()),
                    Integer.parseInt(parts[10].trim()),
                    Integer.parseInt(parts[11].trim()),
                    AuctionStatus.valueOf(parts[12].trim().toUpperCase())
            );
        } catch (Exception ex) {
            ConsoleUI.printError("Khong the parse dong auction: " + line);
            return null;
        }
    }

    private BidTransaction parseBid(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
            ConsoleUI.printWarning("Dong bid khong hop le, bo qua: " + line);
            return null;
        }

        try {
            return new BidTransaction(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    Double.parseDouble(parts[3].trim()),
                    Long.parseLong(parts[4].trim()),
                    parts[5].trim()
            );
        } catch (Exception ex) {
            ConsoleUI.printError("Khong the parse dong bid: " + line);
            return null;
        }
    }

    private void writeLines(File file, List<String> lines, String fileName) {
        try {
            ensureFileExists(file);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            ConsoleUI.printError("Loi ghi file " + fileName + ": " + ex.getMessage());
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
