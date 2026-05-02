package userauth.gui.fxml;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static boolean applyAuctionImage(ImageView imageView, Label fallbackLabel, String imageSource, String fallbackSeed) {
        if (fallbackLabel != null) {
            fallbackLabel.setText(extractInitial(fallbackSeed));
        }

        String normalizedSource = normalizeImageSource(imageSource);
        Image image = loadImage(normalizedSource);
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

    private static Image loadImage(String normalizedSource) {
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
