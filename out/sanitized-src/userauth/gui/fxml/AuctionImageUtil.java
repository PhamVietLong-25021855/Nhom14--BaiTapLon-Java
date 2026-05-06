package userauth.gui.fxml;

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

// File note: Utility tải, cache và render ảnh auction từ DB, URL hoặc file local.
// Gom toÃ n bá»™ xá»­ lÃ½ áº£nh Ä‘áº¥u giÃ¡: clip bo gÃ³c, cache, load tá»« URL/file/byte[].
public final class AuctionImageUtil {
    private static final int IMAGE_CACHE_LIMIT = 96;
    private static final Map<String, Image> IMAGE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > IMAGE_CACHE_LIMIT;
                }
            }
    );

    private AuctionImageUtil() {
    }

    public static void installRoundedClip(ImageView imageView, double arcWidth, double arcHeight) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(arcWidth);
        clip.setArcHeight(arcHeight);
        clip.widthProperty().bind(imageView.fitWidthProperty());
        clip.heightProperty().bind(imageView.fitHeightProperty());
        imageView.setClip(clip);
    }

    // NhÃ¡nh tÆ°Æ¡ng thÃ­ch ngÆ°á»£c cho cÃ¡c chá»— cÅ© chá»‰ cÃ³ imageSource.
    public static boolean applyAuctionImage(ImageView imageView, Label fallbackLabel, String imageSource, String fallbackSeed) {
        return applyAuctionImage(imageView, fallbackLabel, null, imageSource, fallbackSeed);
    }

    // NhÃ¡nh Ä‘áº§y Ä‘á»§: Æ°u tiÃªn áº£nh binary trong DB, fallback vá» URL/path náº¿u khÃ´ng cÃ³.
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

    public static String extractInitial(String value) {
        if (value == null || value.isBlank()) {
            return "A";
        }
        return String.valueOf(Character.toUpperCase(value.trim().charAt(0)));
    }

    // Quyáº¿t Ä‘á»‹nh nguá»“n áº£nh nÃ o sáº½ Ä‘Æ°á»£c dÃ¹ng Ä‘á»ƒ render.
    private static Image loadImage(byte[] imageData, String normalizedSource) {
        if (imageData != null && imageData.length > 0) {
            return loadImageFromBytes(imageData);
        }
        return loadImageFromSource(normalizedSource);
    }

    // Cache theo hash bytes Ä‘á»ƒ trÃ¡nh decode láº¡i áº£nh DB nhiá»u láº§n.
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

