package userauth.gui.fxml;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URLDecoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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
        Image image = loadImage(normalizedSource, imageView);
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
        return loadImage(normalizedSource, null);
    }

    private static Image loadImage(String normalizedSource, ImageView imageView) {
        if (normalizedSource == null) {
            return null;
        }

        String cacheKey = buildCacheKey(normalizedSource, imageView);
        Image cachedImage = IMAGE_CACHE.get(cacheKey);
        if (cachedImage != null) {
            return cachedImage.isError() ? null : cachedImage;
        }

        try {
            Image image = createImage(normalizedSource, imageView);
            if (image == null || image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return null;
            }
            IMAGE_CACHE.put(cacheKey, image);
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
            if (trimmed.startsWith("file:/")) {
                String decoded = decodeFileUri(trimmed);
                File fromUri = new File(decoded);
                if (fromUri.exists()) {
                    return fromUri.toURI().toString();
                }
            }
            return trimmed;
        }

        File localFile = new File(trimmed);
        if (localFile.exists()) {
            return localFile.toURI().toString();
        }

        File decodedFile = new File(URLDecoder.decode(trimmed, StandardCharsets.UTF_8));
        if (decodedFile.exists()) {
            return decodedFile.toURI().toString();
        }

        var resourceUrl = AuctionImageUtil.class.getResource(trimmed.startsWith("/") ? trimmed : "/" + trimmed);
        if (resourceUrl != null) {
            return resourceUrl.toExternalForm();
        }

        try {
            URI uri = new URI(trimmed);
            return uri.getScheme() == null ? null : trimmed;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private static Image createImage(String normalizedSource, ImageView imageView) {
        double requestedWidth = resolveRequestedSize(imageView == null ? 0 : imageView.getFitWidth());
        double requestedHeight = resolveRequestedSize(imageView == null ? 0 : imageView.getFitHeight());
        return new Image(normalizedSource, requestedWidth, requestedHeight, true, true, false);
    }

    private static String buildCacheKey(String normalizedSource, ImageView imageView) {
        int width = (int) Math.round(resolveRequestedSize(imageView == null ? 0 : imageView.getFitWidth()));
        int height = (int) Math.round(resolveRequestedSize(imageView == null ? 0 : imageView.getFitHeight()));
        return normalizedSource + "#" + width + "x" + height;
    }

    private static double resolveRequestedSize(double size) {
        return size > 0 ? size : 0;
    }

    private static String decodeFileUri(String source) {
        try {
            return new File(URI.create(source)).getAbsolutePath();
        } catch (IllegalArgumentException ex) {
            return source;
        }
    }
}
