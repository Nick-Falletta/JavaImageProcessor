// === Safe Image I/O (Open/Save PNG/JPEG) ===
package util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

public final class ImageIOUtils {
    private ImageIOUtils() {}

    public static Optional<BufferedImage> readImage(File file) {
        try { return Optional.ofNullable(ImageIO.read(file)); }
        catch (Exception e) { return Optional.empty(); }
    }

    public static boolean writeAuto(BufferedImage image, File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return writePng(image, file);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return writeJpeg(image, file, 0.92f);
        return writePng(image, ensureExtension(file, ".png"));
    }

    public static boolean writePng(BufferedImage image, File file) {
        try { return ImageIO.write(image, "png", ensureExtension(file, ".png")); }
        catch (Exception e) { return false; }
    }

    public static boolean writeJpeg(BufferedImage image, File file, float quality) {
        file = ensureExtension(file, ".jpg");
        try {
            Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
            if (!it.hasNext()) return false;
            ImageWriter w = it.next();
            ImageWriteParam p = w.getDefaultWriteParam();
            if (p.canWriteCompressed()) {
                p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                p.setCompressionQuality(quality);
            }
            try (var out = new FileImageOutputStream(file)) {
                w.setOutput(out);
                w.write(null, new IIOImage(image, null, null), p);
                w.dispose();
                return true;
            }
        } catch (Exception e) { return false; }
    }

    private static File ensureExtension(File f, String ext) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(ext) ? f : new File(f.getParentFile(), f.getName() + ext);
    }
}
