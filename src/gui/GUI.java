package gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import processing.Filter;
import processing.ImageProcessor;

public class GUI {

    BufferedImage originalImage;

    ImageProcessor imageProcessor;

    JFrame frame;

    JPanel mainPanel,
           imagePanel,
           actionPanel;

    JLabel mainLabel;

    ImageIcon displayedImage;
    JLabel displayedImageContainer;

    JButton selectFileButton,
            applyFilterButton,
            resetButton,
            saveAsButton;
    JComboBox<String> filterSelector;

    JFileChooser fileChooser;


    public GUI() {
        SwingUtilities.invokeLater(() -> {
            imageProcessor = new ImageProcessor();

            frame = new JFrame("Java Image Processor");
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            mainPanel = new JPanel(new BorderLayout());
            mainLabel = new JLabel("Java Image Processor");
            mainLabel.setFont(new Font("Charter", Font.BOLD, 36));
            mainLabel.setHorizontalAlignment(SwingConstants.CENTER);
            mainPanel.add(mainLabel, BorderLayout.NORTH);

            imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            displayedImage = new ImageIcon();
            displayedImageContainer = new JLabel(displayedImage);
            imagePanel.add(displayedImageContainer);

            actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            fileChooser = new JFileChooser();

            selectFileButton = new JButton("Select File");
            selectFileButton.addActionListener(_ -> {
                int result = fileChooser.showOpenDialog(frame);
                if(result == JFileChooser.APPROVE_OPTION) {
                    BufferedImage image = null;
                    try {
                        image = ImageIO.read(fileChooser.getSelectedFile());
                        BufferedImage scaled = ImageProcessor.scaleImage(image, frame.getWidth() * 4, frame.getHeight() * 4);
                        originalImage = ImageProcessor.deepCopy(scaled);
                        frame.setSize(scaled.getWidth() + 200, scaled.getHeight() + 200);
                        imageProcessor.setImage(scaled);
                        displayedImageContainer.setIcon(new ImageIcon(scaled));
                    } catch(IOException ioe) {
                        System.err.println("大変！Something went wrong while loading the image!");
                    }
                }
            });

            filterSelector = new JComboBox<>(Filter.FILTER_TYPES.keySet().toArray(new String[0]));
            filterSelector.addActionListener(_ -> {
                imageProcessor.setFilter((String) filterSelector.getSelectedItem());
            });

            applyFilterButton = new JButton("Apply Filter");
            applyFilterButton.addActionListener(_ -> imageProcessor.execute(displayedImageContainer));

            resetButton = new JButton("Reset");
            resetButton.setForeground(Color.RED);
            resetButton.addActionListener(_ -> {
                BufferedImage copyOfOriginalImage = ImageProcessor.deepCopy(originalImage);
                imageProcessor.setImage(copyOfOriginalImage);
                displayedImageContainer.setIcon(new ImageIcon(copyOfOriginalImage));
            });

            saveAsButton = new JButton("Save As");
            saveAsButton.addActionListener(_ -> {
                System.out.println("Save attempted");
                int result = fileChooser.showSaveDialog(frame);
                if(result == JFileChooser.APPROVE_OPTION) {
                    try {
                        boolean status = ImageIO.write(imageProcessor.getImage(), "PNG", fileChooser.getSelectedFile());
                        System.out.println(status + " save occurred + " + fileChooser.getSelectedFile());
                    } catch(IOException ioe) {
                        System.err.println("大変！Something went wrong while saving the image!");
                    }
                }
            });

            actionPanel.add(resetButton);
            actionPanel.add(selectFileButton);
            actionPanel.add(filterSelector);
            actionPanel.add(applyFilterButton);
            actionPanel.add(saveAsButton);


            mainPanel.add(imagePanel, BorderLayout.CENTER);
            mainPanel.add(actionPanel, BorderLayout.SOUTH);

            frame.add(mainPanel);
            frame.pack();
            frame.setVisible(true);

        });
    }

    public static String[] getFilters() {
        return new String[] {};
    }
}