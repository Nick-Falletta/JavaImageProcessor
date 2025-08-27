// === Document Model (Image, Undo/Redo, Apply Operations) ===
package processing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ImageProcessor {

    private BufferedImage image;
    private BufferedImage original;
    private File sourceFile;

    private final Deque<BufferedImage> undo = new ArrayDeque<>();
    private final Deque<BufferedImage> redo = new ArrayDeque<>();

    public void load(BufferedImage img, File file) {
        this.image = Operations.copyOf(img);
        this.original = Operations.copyOf(img);
        this.sourceFile = file;
        undo.clear(); redo.clear();
    }

    public String fileNameOr(String fallback) {
        return sourceFile != null ? sourceFile.getName() : fallback;
    }

    public String windowTitle() {
        return "Image Studio — " + (sourceFile != null ? sourceFile.getName() : "Untitled");
    }

    public BufferedImage getImage() { return image; }

    public BufferedImage apply(Operations.Operation op) {
        if (image == null) return null;
        pushUndo(image);
        image = op.apply(image);
        redo.clear();
        return image;
    }

    public void revert() {
        if (original == null) return;
        pushUndo(image);
        image = Operations.copyOf(original);
        redo.clear();
    }

    public void undo() {
        if (!canUndo()) return;
        redo.push(Operations.copyOf(image));
        image = undo.pop();
    }

    public void redo() {
        if (!canRedo()) return;
        undo.push(Operations.copyOf(image));
        image = redo.pop();
    }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }

    private void pushUndo(BufferedImage img) {
        undo.push(Operations.copyOf(img));
        while (undo.size() > 20) undo.removeLast();
    }
}
