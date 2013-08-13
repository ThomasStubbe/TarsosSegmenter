package be.hogent.tarsos.tarsossegmenter.gui;

import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <p>
 * A graphical user interface displaying the options for image optimizations
 * These options are the booleans for binary image and for line detection and a
 * slider to set the binary threshold
 * </p>
 * @author Thomas Stubbe
 *
 */
public class ImageOptionsGUI extends JPanel implements ChangeListener {

    private JCheckBox enableLineDetection;
    private JCheckBox enableBinary;
    private JSlider binaryTresholdSlider;
    //boolean needsUpdate;

    /**
     * Constructs the ImageOptionsGUI
     *
     */
    
    public ImageOptionsGUI() {
        super();
        //needsUpdate = false;
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        enableLineDetection = new JCheckBox("Line Detection");
        enableLineDetection.setAlignmentX(Component.LEFT_ALIGNMENT);
        enableLineDetection.setSelected(Configuration.getBoolean(ConfKey.enable_line_detection));

        enableBinary = new JCheckBox("Binary Image");
        enableBinary.setAlignmentX(Component.LEFT_ALIGNMENT);
        enableBinary.setSelected(Configuration.getBoolean(ConfKey.enable_binary));

        binaryTresholdSlider = new JSlider();
        binaryTresholdSlider.setMajorTickSpacing(10);
        binaryTresholdSlider.setMinimum(0);
        binaryTresholdSlider.setMaximum(100);
        binaryTresholdSlider.setValue((int) (Configuration.getDouble(ConfKey.binary_treshold) * 100));

        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        sliderPanel.add(new JLabel("Binary threshold: "));
        sliderPanel.add(binaryTresholdSlider);
        sliderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        this.add(enableLineDetection);
        this.add(enableBinary);
        this.add(sliderPanel);

        enableLineDetection.addChangeListener(this);
        enableBinary.addChangeListener(this);
        binaryTresholdSlider.addChangeListener(this);

    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource().equals(enableLineDetection)) {
            if (Configuration.getBoolean(ConfKey.enable_line_detection) != enableLineDetection.isSelected()) {
                Configuration.set(ConfKey.enable_line_detection, enableLineDetection.isSelected());
                //needsUpdate = true;
            }
        } else if (e.getSource().equals(enableBinary)) {
            if (Configuration.getBoolean(ConfKey.enable_binary) != enableBinary.isSelected()) {
                Configuration.set(ConfKey.enable_binary, enableBinary.isSelected());
                //needsUpdate = true;
            }

        } else if (e.getSource().equals(binaryTresholdSlider)) {
            if (Configuration.getDouble(ConfKey.binary_treshold) != ((float) binaryTresholdSlider.getValue()) / 100f) {
                Configuration.set(ConfKey.binary_treshold, ((float) binaryTresholdSlider.getValue()) / 100f);
                //needsUpdate = true;
            }
        }
//        if (needsUpdate){
//            if (AASModel.getInstance().getSimilarityMatrix() != null){
//                AASModel.getInstance().run();
//            }
//            needsUpdate = false;
//        }
    }
    
    /**
     * Updates the ImageOptionsGUI
     *
     */
    
    public void update() {
        enableLineDetection.removeChangeListener(this);
        enableBinary.removeChangeListener(this);
        binaryTresholdSlider.removeChangeListener(this);
        enableLineDetection.setSelected(Configuration.getBoolean(ConfKey.enable_line_detection));
        enableBinary.setSelected(Configuration.getBoolean(ConfKey.enable_binary));
        binaryTresholdSlider.setValue((int) (Configuration.getDouble(ConfKey.binary_treshold) * 100));
        enableLineDetection.addChangeListener(this);
        enableBinary.addChangeListener(this);
        binaryTresholdSlider.addChangeListener(this);
    }
}
