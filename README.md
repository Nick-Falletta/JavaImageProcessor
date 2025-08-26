# Java Image Processor

## Overview

The **Java Image Processor** is a desktop application built with **Java Swing** that allows users to load images, apply filters, reset changes, and save edited versions.
It provides a simple graphical interface and demonstrates how Java can be used for image manipulation, GUI programming, and modular project structure.

---

## Features

* **File Selection**: Load images from your computer with a file chooser.
* **Image Display**: View the currently loaded image within the application.
* **Filter Application**: Apply color filters such as grayscale or “funk” effects.
* **Reset Function**: Restore the original image without reloading.
* **Save Function**: Save the edited image as a PNG file.
* **Extensible Design**: Filters are defined in a centralized `Filter` class, making it easy to add more.

---

Using the Application

1. Click **Select File** to load an image.
2. Choose a filter from the dropdown menu.
3. Click **Apply Filter** to transform the image.
4. Use **Reset** to restore the original version.
5. Click **Save As** to export the edited image.

---

## Adding New Filters

To add a filter:

1. Open `processing/Filter.java`.
2. Add a new entry to the `FILTER_TYPES` map:

   ```java
   "invert", c -> new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue())
   ```
3. The filter will automatically appear in the filter dropdown in the GUI.

---

## Example Filters

* **Grayscale**: Converts colors to shades of gray.
* **Funk**: Swaps color channels for a stylized effect.

---

## License

This project is provided for educational and demonstration purposes. You are free to use, modify, and extend it.
