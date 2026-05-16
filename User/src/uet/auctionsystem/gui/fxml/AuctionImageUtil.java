package uet.auctionsystem.gui.fxml;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop AuctionImageUtil; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public final class AuctionImageUtil {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho limit.
    private static final int IMAGE_CACHE_LIMIT = 96;
    private static final Map<String, Image> IMAGE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > IMAGE_CACHE_LIMIT;
                }
            }
    );
    // Ham tao: khoi tao doi tuong AuctionImageUtil voi cac phu thuoc can thiet.
    private AuctionImageUtil() {
    }
    // Phuong thuc: thuc hien chuc nang install rounded clip trong lop AuctionImageUtil.
    public static void installRoundedClip(ImageView imageView, double arcWidth, double arcHeight) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(arcWidth);
        clip.setArcHeight(arcHeight);
        clip.widthProperty().bind(imageView.fitWidthProperty());
        clip.heightProperty().bind(imageView.fitHeightProperty());
        imageView.setClip(clip);
    }

    // NhÃ¡nh tÆ°Æ¡ng thÃ­ch ngÆ°á»£c cho cÃ¡c chá»— cÅ© chá»‰ cÃ³ imageSource.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply auction image.
    public static boolean applyAuctionImage(ImageView imageView, Label fallbackLabel, String imageSource, String fallbackSeed) {
        return applyAuctionImage(imageView, fallbackLabel, null, imageSource, fallbackSeed);
    }

    // NhÃ¡nh Ä‘áº§y Ä‘á»§: Æ°u tiÃªn áº£nh binary trong DB, fallback vá» URL/path náº¿u khÃ´ng cÃ³.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply auction image.
    public static boolean applyAuctionImage(ImageView imageView, Label fallbackLabel, byte[] imageData, String imageSource, String fallbackSeed) {
        if (fallbackLabel != null) {
            fallbackLabel.setText(extractInitial(fallbackSeed));
        }

        String normalizedSource = normalizeImageSource(imageSource);
        Image image = loadImage(imageData, normalizedSource);
        boolean hasImage = image != null;

        if (imageView != null) {
            imageView.setImage(image);
            imageView.setVisible(hasImage);
            imageView.setManaged(hasImage);
        }
        if (fallbackLabel != null) {
            fallbackLabel.setVisible(!hasImage);
            fallbackLabel.setManaged(!hasImage);
        }
        return hasImage;
    }
    // Phuong thuc: thuc hien chuc nang extract initial trong lop AuctionImageUtil.
    public static String extractInitial(String value) {
        if (value == null || value.isBlank()) {
            return "A";
        }
        return String.valueOf(Character.toUpperCase(value.trim().charAt(0)));
    }

    // Quyáº¿t Ä‘á»‹nh nguá»“n áº£nh nÃ o sáº½ Ä‘Æ°á»£c dÃ¹ng Ä‘á»ƒ render.
    // Phuong thuc: lay hoac doc du lieu cho thao tac load image.
    private static Image loadImage(byte[] imageData, String normalizedSource) {
        if (imageData != null && imageData.length > 0) {
            return loadImageFromBytes(imageData);
        }
        return loadImageFromSource(normalizedSource);
    }

    // Cache theo hash bytes Ä‘á»ƒ trÃ¡nh decode láº¡i áº£nh DB nhiá»u láº§n.
    // Phuong thuc: lay hoac doc du lieu cho thao tac load image from bytes.
    private static Image loadImageFromBytes(byte[] imageData) {
        String cacheKey = "db:" + imageData.length + ":" + Arrays.hashCode(imageData);
        Image cachedImage = IMAGE_CACHE.get(cacheKey);
        if (cachedImage != null) {
            return cachedImage.isError() ? null : cachedImage;
        }

        try {
            Image image = new Image(new ByteArrayInputStream(imageData));
            IMAGE_CACHE.put(cacheKey, image);
            return image.isError() ? null : image;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // Cache theo URL/path chuáº©n hÃ³a cho cÃ¡c áº£nh ngoÃ i DB.
    // Phuong thuc: lay hoac doc du lieu cho thao tac load image from source.
    private static Image loadImageFromSource(String normalizedSource) {
        if (normalizedSource == null) {
            return null;
        }

        Image cachedImage = IMAGE_CACHE.get(normalizedSource);
        if (cachedImage != null) {
            return cachedImage.isError() ? null : cachedImage;
        }

        try {
            Image image = new Image(normalizedSource, true);
            IMAGE_CACHE.put(normalizedSource, image);
            return image;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
    // Phuong thuc: thuc hien chuc nang normalize image source trong lop AuctionImageUtil.
    private static String normalizeImageSource(String imageSource) {
        if (imageSource == null) {
            return null;
        }

        String trimmed = imageSource.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file:/")) {
            return trimmed;
        }

        File localFile = new File(trimmed);
        if (localFile.exists()) {
            return localFile.toURI().toString();
        }

        try {
            URI uri = new URI(trimmed);
            return uri.getScheme() == null ? null : trimmed;
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
