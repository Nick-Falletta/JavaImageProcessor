package processing;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class ImageProcessor {
    private BufferedImage image;
    private String filter;

    public ImageProcessor() {
        this.filter = Filter.FILTER_TYPES.keySet().toArray(new String[0])[0];
    }

    public void execute(JLabel imageContainer) {
        new SwingWorker<Void, BufferedImage>() {
            @Override
            protected Void doInBackground() throws Exception {
                Filter.apply(image, filter);
                return null;
            }

            @Override
            protected void done() {
                imageContainer.setIcon(new ImageIcon(image));
            }
        }.execute();
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        if(image != null) {
            this.image = image;
        }
    }

    public static BufferedImage scaleImage(BufferedImage image, int maxWidth, int maxHeight) {
        int originalWidth = image.getWidth(null);
        int originalHeight = image.getHeight(null);

        double scale = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        Image scaledImage = image.getScaledInstance(newWidth, newHeight, BufferedImage.SCALE_SMOOTH);
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();

        return newImage;
    }

    public static BufferedImage deepCopy(BufferedImage original) {
        BufferedImage copy = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                original.getType()
        );
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();
        return copy;
    }
}