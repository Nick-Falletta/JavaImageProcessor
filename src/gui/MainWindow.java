// === Main Window (UI Shell, Menus, Toolbar, Status Bar) ===
package gui;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import processing.ImageProcessor;
import processing.Operations;
import util.ImageIOUtils;

public final class MainWindow {

    // === Core State ===
    private JFrame frame;
    private final ImageProcessor document = new ImageProcessor();
    private final ImageCanvas canvas = new ImageCanvas(document);

    // === UI: Status ===
    private final JLabel statusLeft = new JLabel("—");
    private final JLabel statusCenter = new JLabel("—");
    private final JLabel statusRight = new JLabel("—");
    private final JProgressBar progressBar = new JProgressBar();

    // === UI: Controls ===
    private final JFileChooser chooser = new JFileChooser();
    private final JComboBox<String> filterBox = new JComboBox<>(Operations.builtInNames());

    private final JSlider brightness = new JSlider(-100, 100, 0);
    private final JSlider contrast   = new JSlider(-100, 100, 0);
    private final JSlider blurRadius = new JSlider(0, 8, 0);
    private final JButton applyBtn   = new JButton("Apply");
    private final JButton undoBtn    = new JButton("Undo");
    private final JButton redoBtn    = new JButton("Redo");

    // === Actions ===
    private final Action openAction   = new AbstractAction("Open…") {
        @Override public void actionPerformed(ActionEvent e) { doOpen(); }
    };
    private final Action saveAsAction = new AbstractAction("Save As…") {
        @Override public void actionPerformed(ActionEvent e) { doSaveAs(); }
    };
    private final Action exportJpgAction = new AbstractAction("Export JPEG…") {
        @Override public void actionPerformed(ActionEvent e) { doExportJpeg(); }
    };
    private final Action resetAction  = new AbstractAction("Revert") {
        @Override public void actionPerformed(ActionEvent e) { document.revert(); refreshUI(); }
    };
    private final Action quitAction   = new AbstractAction("Quit") {
        @Override public void actionPerformed(ActionEvent e) { frame.dispose(); }
    };
    private final Action rotateLeftAction = new AbstractAction("Rotate ⟲") {
        @Override public void actionPerformed(ActionEvent e) { runAsync(() -> Operations.rotate(-90)); }
    };
    private final Action rotateRightAction = new AbstractAction("Rotate ⟲⟲") {
        @Override public void actionPerformed(ActionEvent e) { runAsync(() -> Operations.rotate(90)); }
    };
    private final Action flipHAction = new AbstractAction("Flip H") {
        @Override public void actionPerformed(ActionEvent e) { runAsync(Operations::flipH); }
    };
    private final Action flipVAction = new AbstractAction("Flip V") {
        @Override public void actionPerformed(ActionEvent e) { runAsync(Operations::flipV); }
    };
    private final Action zoomInAction = new AbstractAction("Zoom In") {
        @Override public void actionPerformed(ActionEvent e) { canvas.zoomStep(+1); updateStatus(); }
    };
    private final Action zoomOutAction = new AbstractAction("Zoom Out") {
        @Override public void actionPerformed(ActionEvent e) { canvas.zoomStep(-1); updateStatus(); }
    };
    private final Action resetZoomAction = new AbstractAction("Actual Size") {
        @Override public void actionPerformed(ActionEvent e) { canvas.resetZoom(); updateStatus(); }
    };

    private final Action cropModeAction = new AbstractAction("Crop Image") {
        @Override public void actionPerformed(ActionEvent e) { canvas.enableCropMode(); }
    };

    private final Action applyCropAction = new AbstractAction("Apply Crop") {
        @Override public void actionPerformed(ActionEvent e) {
            Rectangle rectView = canvas.finishCrop();
            if (rectView != null && rectView.width > 5 && rectView.height > 5) {
                // convert canvas rect → image coordinates
                Point p1 = canvas.toImagePoint(new Point(rectView.x, rectView.y));
                Point p2 = canvas.toImagePoint(new Point(rectView.x + rectView.width, rectView.y + rectView.height));

                int x = Math.max(0, Math.min(p1.x, p2.x));
                int y = Math.max(0, Math.min(p1.y, p2.y));
                int w = Math.abs(p2.x - p1.x);
                int h = Math.abs(p2.y - p1.y);

                Rectangle rectImage = new Rectangle(x, y, w, h);

                runAsync(() -> Operations.crop(rectImage));
            }
        }
    };

    // === Bootstrap ===
    public static void launch() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainWindow().show());
    }

    private MainWindow() {
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Image Files (PNG/JPG/JPEG)", "png", "jpg", "jpeg"));
        initUI();
        bindShortcuts();
    }

    // === UI Construction ===
    private void initUI() {
        frame = new JFrame("Image Studio");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 650));

        frame.setJMenuBar(buildMenuBar());
        var toolbar = buildToolbar();

        var scroll = new JScrollPane(canvas);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        var status = buildStatusBar();

        var content = new JPanel(new BorderLayout(0, 0));
        content.add(toolbar, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(status, BorderLayout.SOUTH);

        frame.setContentPane(content);

        DropTarget dt = new DropTarget(canvas, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Object transfer = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    if (transfer instanceof java.util.List<?> list && !list.isEmpty()) {
                        File f = (File) list.get(0);
                        openFile(f);
                    }
                } catch (Exception ignored) {}
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateStatus(e); }
            @Override public void mouseDragged(MouseEvent e) { updateStatus(e); }
        });

        filterBox.setSelectedItem("Grayscale");
        applyBtn.addActionListener(_ -> applyControls());
        undoBtn.addActionListener(_ -> { document.undo(); refreshUI(); });
        redoBtn.addActionListener(_ -> { document.redo(); refreshUI(); });

        frame.pack();
    }

    private JMenuBar buildMenuBar() {
        var mb = new JMenuBar();

        var file = new JMenu("File");
        file.add(openAction);
        file.add(saveAsAction);
        file.add(exportJpgAction);
        file.addSeparator();
        file.add(resetAction);
        file.addSeparator();
        file.add(quitAction);

        var edit = new JMenu("Edit");
        edit.add(undoBtn);
        edit.add(redoBtn);

        var view = new JMenu("View");
        view.add(zoomInAction);
        view.add(zoomOutAction);
        view.add(resetZoomAction);

        var image = new JMenu("Image");
        image.add(rotateLeftAction);
        image.add(rotateRightAction);
        image.add(flipHAction);
        image.add(flipVAction);
        image.addSeparator();
        image.add(cropModeAction);
        image.add(applyCropAction);

        mb.add(file); mb.add(edit); mb.add(view); mb.add(image);
        return mb;
    }

    private JToolBar buildToolbar() {
        var tb = new JToolBar();
        tb.setFloatable(false);

        tb.add(new JButton(openAction));
        tb.add(new JButton(saveAsAction));
        tb.add(new JButton(exportJpgAction));
        tb.addSeparator();
        tb.add(undoBtn);
        tb.add(redoBtn);
        tb.addSeparator();
        tb.add(new JButton(rotateLeftAction));
        tb.add(new JButton(rotateRightAction));
        tb.add(new JButton(flipHAction));
        tb.add(new JButton(flipVAction));
        tb.addSeparator();

        tb.add(new JLabel(" Filter: "));
        tb.add(filterBox);
        tb.add(new JLabel("  Brightness "));
        brightness.setPreferredSize(new Dimension(120, 20));
        tb.add(brightness);
        tb.add(new JLabel("  Contrast "));
        contrast.setPreferredSize(new Dimension(120, 20));
        tb.add(contrast);
        tb.add(new JLabel("  Blur "));
        blurRadius.setPreferredSize(new Dimension(90, 20));
        tb.add(blurRadius);
        tb.add(applyBtn);

        return tb;
    }

    private JPanel buildStatusBar() {
        var panel = new JPanel(new BorderLayout(8, 0));
        var left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        var center = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 3));
        var right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));

        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        left.add(statusLeft);
        center.add(statusCenter);
        right.add(progressBar);
        right.add(statusRight);

        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0,0,0,32)));
        panel.add(left, BorderLayout.WEST);
        panel.add(center, BorderLayout.CENTER);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // === Keyboard Shortcuts ===
    private void bindShortcuts() {
        var r = frame.getRootPane();
        var im = r.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var am = r.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, meta()), "open");
        am.put("open", openAction);

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, meta()), "saveAs");
        am.put("saveAs", saveAsAction);

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, meta()), "exportJpg");
        am.put("exportJpg", exportJpgAction);

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, meta()), "undo");
        am.put("undo", undoBtn.getAction());

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, meta()), "redo");
        am.put("redo", redoBtn.getAction());

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, meta()), "zoomIn");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, meta()), "zoomIn2");
        am.put("zoomIn", zoomInAction); am.put("zoomIn2", zoomInAction);

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, meta()), "zoomOut");
        am.put("zoomOut", zoomOutAction);

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, meta()), "resetZoom");
        am.put("resetZoom", resetZoomAction);
    }

    private int meta() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    // === Open / Save ===
    private void doOpen() {
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            openFile(chooser.getSelectedFile());
        }
    }

    private void openFile(File file) {
        Optional<BufferedImage> loaded = ImageIOUtils.readImage(file);
        if (loaded.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Could not open image.", "Open", JOptionPane.ERROR_MESSAGE);
            return;
        }
        document.load(loaded.get(), file);
        canvas.resetViewFor(document.getImage());
        refreshUI();
    }

    private void doSaveAs() {
        if (!ensureImage()) return;
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        var file = chooser.getSelectedFile();
        var ok = ImageIOUtils.writeAuto(document.getImage(), file);
        if (!ok) JOptionPane.showMessageDialog(frame, "Save failed.", "Save As", JOptionPane.ERROR_MESSAGE);
    }

    private void doExportJpeg() {
        if (!ensureImage()) return;

        var quality = askJpegQuality();
        if (quality.isEmpty()) return;

        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        var file = chooser.getSelectedFile();
        var ok = ImageIOUtils.writeJpeg(document.getImage(), file, quality.get());
        if (!ok) JOptionPane.showMessageDialog(frame, "Export failed.", "Export JPEG", JOptionPane.ERROR_MESSAGE);
    }

    private Optional<Float> askJpegQuality() {
        var s = (String) JOptionPane.showInputDialog(
                frame, "JPEG Quality (0.1 - 1.0)", "Export JPEG",
                JOptionPane.QUESTION_MESSAGE, null, null, "0.92");
        try {
            if (s == null) return Optional.empty();
            float v = Float.parseFloat(s);
            if (v < 0.1f || v > 1.0f) throw new IllegalArgumentException();
            return Optional.of(v);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Invalid quality.", "Export JPEG", JOptionPane.WARNING_MESSAGE);
            return Optional.empty();
        }
    }

    // === Processing ===
    private void applyControls() {
        if (!ensureImage()) return;

        final String name = String.valueOf(filterBox.getSelectedItem());
        final int b = brightness.getValue();
        final int c = contrast.getValue();
        final int r = blurRadius.getValue();

        runAsync(() -> Operations.compose(
                Operations.named(name),
                Operations.brightness(b / 100f),
                Operations.contrast((c) / 100f),
                r > 0 ? Operations.gaussianBlur(r) : Operations.none()
        ));
    }

    private void runAsync(Operations.OperationSupplier supplier) {
        if (!ensureImage()) return;

        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        setControlsEnabled(false);

        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() {
                return document.apply(supplier.get());
            }
            @Override protected void done() {
                try {
                    if (!isCancelled()) {
                        canvas.repaint();
                        refreshUI();
                    }
                } finally {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    setControlsEnabled(true);
                }
            }
        }.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        for (var c : new JComponent[]{filterBox, brightness, contrast, blurRadius, applyBtn}) c.setEnabled(enabled);
        undoBtn.setEnabled(enabled && document.canUndo());
        redoBtn.setEnabled(enabled && document.canRedo());
    }

    // === Status ===
    private void refreshUI() {
        undoBtn.setEnabled(document.canUndo());
        redoBtn.setEnabled(document.canRedo());
        updateStatus();
        frame.setTitle(document.windowTitle());
        canvas.revalidate();
        canvas.repaint();
    }

    private void updateStatus() {
        var img = document.getImage();
        if (img == null) {
            statusLeft.setText("No image");
            statusCenter.setText("—");
            statusRight.setText("—");
            return;
        }
        statusLeft.setText(img.getWidth() + "×" + img.getHeight());
        statusCenter.setText(Math.round(canvas.getZoom() * 100) + "%");
        statusRight.setText(document.fileNameOr("Untitled"));
    }

    private void updateStatus(MouseEvent e) {
        updateStatus();
        var p = canvas.toImagePoint(e.getPoint());
        var img = document.getImage();
        if (img == null || p.x < 0 || p.y < 0 || p.x >= img.getWidth() || p.y >= img.getHeight()) {
            statusLeft.setText(statusLeft.getText() + "  |  —");
            return;
        }
        int rgb = img.getRGB(p.x, p.y);
        var r = (rgb >> 16) & 0xFF;
        var g = (rgb >> 8) & 0xFF;
        var b = (rgb) & 0xFF;
        statusLeft.setText(img.getWidth() + "×" + img.getHeight() + "  |  (" + p.x + "," + p.y + ")  RGB " + r + "," + g + "," + b);
    }

    private boolean ensureImage() {
        if (document.getImage() != null) return true;
        JOptionPane.showMessageDialog(frame, "Open an image first.", "Info", JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    private void show() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
