// === Operations (Filters, Transforms, Composition) ===
package processing;

import java.awt.*;
import java.awt.image.*;
import java.util.Arrays;

public final class Operations {

    // === Types ===
    @FunctionalInterface
    public interface Operation { BufferedImage apply(BufferedImage src); }
    @FunctionalInterface
    public interface OperationSupplier { Operation get(); }

    // === Built-ins ===
    public static String[] builtInNames() {
        return new String[]{"None","Grayscale", "Invert", "Sepia", "Funk"};
    }

    public static Operation named(String name) {
        return switch (name) {
            case "Grayscale" -> grayscale();
            case "Invert" -> invert();
            case "Sepia" -> sepia();
            case "Funk" -> funk();
            case "None" -> none();
            default -> none();
        };
    }

    public static Operation none() { return src -> src; }

    public static Operation compose(Operation... ops) {
        var filtered = Arrays.stream(ops).filter(o -> o != null).toList();
        return src -> {
            BufferedImage cur = src;
            for (var op : filtered) cur = op.apply(cur);
            return cur;
        };
    }

    public static BufferedImage copyOf(BufferedImage src) {
        ColorModel cm = src.getColorModel();
        boolean alpha = cm.isAlphaPremultiplied();
        WritableRaster raster = src.copyData(null);
        return new BufferedImage(cm, raster, alpha, null);
    }

    // === Pixelwise ===
    public static Operation grayscale() {
        return src -> {
            BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int argb = src.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = (argb >>> 16) & 0xFF;
                    int g = (argb >>> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int gray = (int)(0.299*r + 0.587*g + 0.114*b);
                    int out = (a << 24) | (gray << 16) | (gray << 8) | gray;
                    dst.setRGB(x, y, out);
                }
            }
            return dst;
        };
    }

    public static Operation invert() {
        return src -> {
            BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int argb = src.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = 255 - ((argb >>> 16) & 0xFF);
                    int g = 255 - ((argb >>> 8) & 0xFF);
                    int b = 255 - (argb & 0xFF);
                    int out = (a << 24) | (r << 16) | (g << 8) | b;
                    dst.setRGB(x, y, out);
                }
            }
            return dst;
        };
    }

    public static Operation sepia() {
        return src -> {
            BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int argb = src.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = (argb >>> 16) & 0xFF;
                    int g = (argb >>> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int tr = clamp((int)(0.393*r + 0.769*g + 0.189*b));
                    int tg = clamp((int)(0.349*r + 0.686*g + 0.168*b));
                    int tb = clamp((int)(0.272*r + 0.534*g + 0.131*b));
                    int out = (a << 24) | (tr << 16) | (tg << 8) | tb;
                    dst.setRGB(x, y, out);
                }
            }
            return dst;
        };
    }

    public static Operation funk() {
        return src -> {
            BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int argb = src.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = (argb >>> 16) & 0xFF;
                    int g = (argb >>> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int out = (a << 24) | (g << 16) | (b << 8) | r;
                    dst.setRGB(x, y, out);
                }
            }
            return dst;
        };
    }

    // === Tone ===
    public static Operation brightness(float delta) { // -1..+1
        return src -> {
            RescaleOp op = new RescaleOp(
                    new float[]{1f,1f,1f,1f},
                    new float[]{255f*delta,255f*delta,255f*delta,0f}, null);
            return op.filter(src, null);
        };
    }

    public static Operation contrast(float amount) { // -1..+1
        return src -> {
            float c = 1f + amount;
            float t = 128f * (1f - c);
            RescaleOp op = new RescaleOp(
                    new float[]{c,c,c,1f},
                    new float[]{t,t,t,0f}, null);
            return op.filter(src, null);
        };
    }

    // === Convolution ===
    public static Operation gaussianBlur(int radius) {
        if (radius <= 0) return none();
        float[] kernel = gaussianKernel(radius);
        return separableConvolution(kernel, kernel);
    }

    public static Operation sharpen() {
        float[] k = {
                0, -1,  0,
               -1,  5, -1,
                0, -1,  0
        };
        Kernel kernel = new Kernel(3,3,k);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return src -> op.filter(src, null);
    }

    private static Operation separableConvolution(float[] h, float[] v) {
        Kernel kh = new Kernel(h.length, 1, h);
        Kernel kv = new Kernel(1, v.length, v);
        ConvolveOp opH = new ConvolveOp(kh, ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp opV = new ConvolveOp(kv, ConvolveOp.EDGE_NO_OP, null);
        return src -> {
            BufferedImage tmp = opH.filter(src, null);
            return opV.filter(tmp, null);
        };
    }

    private static float[] gaussianKernel(int radius) {
        int size = radius * 2 + 1;
        float sigma = radius / 2f + 0.5f;
        float[] k = new float[size];
        float sum = 0f;
        int c = radius;
        for (int i = 0; i < size; i++) {
            int x = i - c;
            float v = (float) Math.exp(-(x*x)/(2*sigma*sigma));
            k[i] = v; sum += v;
        }
        for (int i = 0; i < size; i++) k[i] /= sum;
        return k;
    }

    // === Geometric ===
    public static Operation rotate(int degrees) {
        return src -> {
            double theta = Math.toRadians((degrees % 360 + 360) % 360);
            int w = src.getWidth(), h = src.getHeight();
            double sin = Math.abs(Math.sin(theta)), cos = Math.abs(Math.cos(theta));
            int newW = (int) Math.floor(w * cos + h * sin);
            int newH = (int) Math.floor(h * cos + w * sin);

            BufferedImage dst = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.translate((newW - w) / 2.0, (newH - h) / 2.0);
            g.rotate(theta, w / 2.0, h / 2.0);
            g.drawImage(src, 0, 0, null);
            g.dispose();
            return dst;
        };
    }

    public static Operation flipH() {
        return src -> {
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dst.createGraphics();
            g.scale(-1, 1);
            g.translate(-w, 0);
            g.drawImage(src, 0, 0, null);
            g.dispose();
            return dst;
        };
    }

    public static Operation flipV() {
        return src -> {
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dst.createGraphics();
            g.scale(1, -1);
            g.translate(0, -h);
            g.drawImage(src, 0, 0, null);
            g.dispose();
            return dst;
        };
    }

    public static Operation crop(Rectangle rect) {
        return src -> {
            int x = Math.max(0, rect.x);
            int y = Math.max(0, rect.y);
            int w = Math.min(src.getWidth() - x, rect.width);
            int h = Math.min(src.getHeight() - y, rect.height);

            if (w <= 0 || h <= 0) return src;

            BufferedImage cropped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = cropped.createGraphics();
            g.drawImage(src, 0, 0, w, h, x, y, x + w, y + h, null);
            g.dispose();
            return cropped;
        };
}

    // === Helpers ===
    private static int clamp(int v) { return (v < 0) ? 0 : Math.min(v, 255); }
}
