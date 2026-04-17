package userauth.service;

import userauth.model.HomepageAnnouncement;
import userauth.utils.ConsoleUI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class HomepageFileService {
    private static final String FILE_PATH = "User/data/homepage-announcements.txt";

    public List<HomepageAnnouncement> loadAnnouncementsFromFile() {
        List<HomepageAnnouncement> announcements = new ArrayList<>();
        File file = new File(FILE_PATH);

        try {
            ensureFileExists(file);
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    HomepageAnnouncement announcement = parseAnnouncement(line);
                    if (announcement != null) {
                        announcements.add(announcement);
                    }
                }
            }
        } catch (IOException ex) {
            ConsoleUI.printError("Loi doc file homepage-announcements.txt: " + ex.getMessage());
        }

        return announcements;
    }

    public void saveAnnouncementsToFile(List<HomepageAnnouncement> announcements) {
        File file = new File(FILE_PATH);

        try {
            ensureFileExists(file);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (HomepageAnnouncement announcement : announcements) {
                    writer.write(serializeAnnouncement(announcement));
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            ConsoleUI.printError("Loi ghi file homepage-announcements.txt: " + ex.getMessage());
        }
    }

    private HomepageAnnouncement parseAnnouncement(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 9) {
            ConsoleUI.printWarning("Dong announcement khong hop le, bo qua: " + line);
            return null;
        }

        try {
            return new HomepageAnnouncement(
                    Integer.parseInt(parts[0].trim()),
                    decode(parts[1]),
                    decode(parts[2]),
                    decode(parts[3]),
                    decode(parts[4]),
                    Integer.parseInt(parts[5].trim()),
                    Integer.parseInt(parts[6].trim()),
                    Long.parseLong(parts[7].trim()),
                    Long.parseLong(parts[8].trim())
            );
        } catch (Exception ex) {
            ConsoleUI.printError("Khong the parse dong announcement: " + line);
            return null;
        }
    }

    private String serializeAnnouncement(HomepageAnnouncement announcement) {
        return announcement.getId() + "|" +
                encode(announcement.getTitle()) + "|" +
                encode(announcement.getSummary()) + "|" +
                encode(announcement.getDetails()) + "|" +
                encode(announcement.getScheduleText()) + "|" +
                announcement.getLinkedAuctionId() + "|" +
                announcement.getAuthorId() + "|" +
                announcement.getCreatedAt() + "|" +
                announcement.getUpdatedAt();
    }

    private String encode(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.getEncoder().encodeToString(safeValue.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
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
