// === Image Canvas (Zoom, Pan, Checkerboard) ===
package gui;

import processing.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

final class ImageCanvas extends JComponent {
    private final ImageProcessor doc;
    private double zoom = 1.0;
    private int offsetX = 0, offsetY = 0;
    private Point dragStart;
    private boolean cropping = false;
    private Rectangle cropRect;
    private Point cropStart;

    ImageCanvas(ImageProcessor doc) {
        this.doc = doc;
        setBackground(new Color(0xF3F3F3));
        setOpaque(true);

        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            double oldZoom = zoom;
            zoom *= Math.pow(1.1, -notches);
            zoom = Math.max(0.05, Math.min(zoom, 32.0));
            double scale = zoom / oldZoom;
            Point p = e.getPoint();
            offsetX = (int) (scale * (p.x - offsetX) + offsetX - p.x);
            offsetY = (int) (scale * (p.y - offsetY) + offsetY - p.y);
            revalidate(); repaint();
        });

        var mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (cropping) {
                    cropStart = e.getPoint();
                    cropRect = new Rectangle(cropStart);
                } else {
                    dragStart = e.getPoint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (cropping && cropStart != null) {
                    int x = Math.min(cropStart.x, e.getX());
                    int y = Math.min(cropStart.y, e.getY());
                    int w = Math.abs(e.getX() - cropStart.x);
                    int h = Math.abs(e.getY() - cropStart.y);
                    cropRect = new Rectangle(x, y, w, h);
                    repaint();
                } else if (!cropping && dragStart != null) {
                    offsetX += e.getX() - dragStart.x;
                    offsetY += e.getY() - dragStart.y;
                    dragStart = e.getPoint();
                    repaint();
                }
            }
            @Override public void mouseReleased(MouseEvent e) { dragStart = null; }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { resetZoom(); repaint(); }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    @Override public Dimension getPreferredSize() {
        BufferedImage img = doc.getImage();
        if (img == null) return new Dimension(800, 600);
        return new Dimension((int) (img.getWidth() * zoom), (int) (img.getHeight() * zoom));
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        paintCheckerboard(g2);

        BufferedImage img = doc.getImage();
        if (img != null) {
            AffineTransform at = new AffineTransform();
            at.translate(offsetX, offsetY);
            at.scale(zoom, zoom);
            g2.drawRenderedImage(img, at);
        }
        if (cropping && cropRect != null) {
            g2.setColor(new Color(0, 120, 215, 80));
            g2.fill(cropRect);
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            g2.draw(cropRect);
        }
        g2.dispose();
    }

    private void paintCheckerboard(Graphics2D g2) {
        int size = 10;
        Color a = new Color(220, 220, 220);
        Color b = new Color(240, 240, 240);
        int w = getWidth(), h = getHeight();
        for (int y = 0; y < h; y += size) {
            for (int x = 0; x < w; x += size) {
                g2.setColor(((x + y) / size) % 2 == 0 ? a : b);
                g2.fillRect(x, y, size, size);
            }
        }
    }

    void resetViewFor(BufferedImage img) {
        zoom = img == null ? 1.0 : Math.min(1.0, Math.min( (getWidth()-40) / (double) Math.max(1, img.getWidth()),
                                                            (getHeight()-40) / (double) Math.max(1, img.getHeight())));
        offsetX = offsetY = 20;
        revalidate(); repaint();
    }

    void zoomStep(int direction) {
        double factor = (direction > 0) ? 1.1 : 1.0 / 1.1;
        zoom = Math.max(0.05, Math.min(zoom * factor, 32.0));
        revalidate(); repaint();
    }

    void resetZoom() {
        zoom = 1.0;
        offsetX = offsetY = 0;
        revalidate();
    }

    double getZoom() { return zoom; }

    Point toImagePoint(Point viewPoint) {
        int x = (int) ((viewPoint.x - offsetX) / zoom);
        int y = (int) ((viewPoint.y - offsetY) / zoom);
        return new Point(x, y);
    }

    void enableCropMode() {
        cropping = true;
        cropRect = null;
        repaint();
    }

    Rectangle finishCrop() {
        cropping = false;
        repaint();
        return cropRect;
    }

}
