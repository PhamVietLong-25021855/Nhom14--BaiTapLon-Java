package userauth.util;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Utility class để tối ưu hóa hình ảnh.
 *
 * Chiến lược:
 * 1. Compress images trước khi lưu vào database (dùng DEFLATE)
 * 2. Decompress khi cần hiển thị
 * 3. Giảm kích thước từ ~500KB xuống ~100-150KB
 *
 * Lưu ý:
 * - Chỉ nên dùng cho auctions list (không cần full resolution)
 * - Full quality images nên lưu riêng (S3, file storage)
 */
public class ImageCompressionUtil {

    /**
     * Compress hình ảnh bằng DEFLATE algorithm.
     * Giảm kích thước hình ảnh ~70% mà vẫn giữ được chất lượng tốt.
     *
     * @param imageData hình ảnh gốc
     * @return compressed image data
     */
    public static byte[] compressImage(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return imageData;
        }

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(imageData);
        deflater.finish();

        byte[] buffer = new byte[1024];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(imageData.length);

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }

        deflater.end();
        return outputStream.toByteArray();
    }

    /**
     * Decompress hình ảnh đã được compress.
     *
     * @param compressedData compressed image data
     * @return original image data
     * @throws IllegalStateException nếu decompression thất bại
     */
    public static byte[] decompressImage(byte[] compressedData) {
        if (compressedData == null || compressedData.length == 0) {
            return compressedData;
        }

        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);

        byte[] buffer = new byte[1024];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedData.length);

        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decompress image data.", e);
        } finally {
            inflater.end();
        }

        return outputStream.toByteArray();
    }

    /**
     * Tính toán tỷ lệ nén.
     * Dùng để monitoring và analytics.
     *
     * @param originalSize kích thước hình ảnh gốc
     * @param compressedSize kích thước sau nén
     * @return compression ratio (0-100%)
     */
    public static double getCompressionRatio(long originalSize, long compressedSize) {
        if (originalSize == 0) {
            return 0;
        }
        return ((double) (originalSize - compressedSize) / originalSize) * 100;
    }
}

