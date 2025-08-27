# Image Studio

Image Studio is a lightweight Java Swing application for image processing and editing.  
It provides a responsive desktop interface with support for file operations, filters, transformations, cropping, undo/redo, and drag-and-drop.

---

## Features

- **File Support**
  - Open PNG and JPEG images
  - Save As (PNG or JPEG with adjustable quality)
  - Drag and drop image files directly onto the window

- **Filters and Adjustments**
  - Grayscale, Invert, Sepia, Funk
  - Brightness and contrast controls
  - Gaussian blur and sharpen filters

- **Image Editing**
  - Crop tool with rectangle selection
  - Rotate (left, right)
  - Flip (horizontal, vertical)
  - Revert to original image

- **Workflow Tools**
  - Undo and redo (20 steps)
  - Zoom and pan with mouse wheel or toolbar
  - Status bar with size, zoom level, cursor coordinates, and RGB values

- **User Interface**
  - System look and feel
  - Toolbar, menus, and status bar
  - Drag-and-drop integration
  - Asynchronous processing for responsive UI

---

## Usage

1. Launch the application.
2. Open an image using **File → Open…** or drag and drop an image onto the window.
3. Apply filters or transformations using the toolbar or menus.
4. Use the crop tool:

   * Select **Image → Start Crop** and drag a rectangle over the image.
   * Apply the crop with **Image → Apply Crop**.
5. Save the result using **File → Save As…** or **Export JPEG…**.

---

## Project Structure

```
src/
├── Main.java                 # Entry point
├── gui/
│   ├── MainWindow.java       # Main application window and UI
│   ├── ImageCanvas.java      # Canvas with zoom, pan, and cropping
├── processing/
│   ├── ImageProcessor.java   # Image state, undo/redo, apply operations
│   └── Operations.java       # Filters, transformations, adjustments
└── util/
    └── ImageIOUtils.java     # File I/O utilities for PNG and JPEG
```

