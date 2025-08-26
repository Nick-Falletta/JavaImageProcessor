package processing;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.Function;

public class Filter {
    public static final Map<String, Function<Color, Color>> FILTER_TYPES = Map.of(
                "grayscale", c -> {
                    int gray = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                    return new Color(gray, gray, gray);
                },
                "funk", c -> new Color(c.getGreen(), c.getBlue(), c.getAlpha())
            );


    public static void apply(BufferedImage image, String transformerName) {
        Function<Color, Color> transformer = FILTER_TYPES.get(transformerName);

        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                image.setRGB(x, y, transformer.apply(color).getRGB());
            }
        }
    }
}