/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.gui;

import be.hogent.tarsos.tarsossegmenter.util.io.ConfigurationFileFilter;
import be.hogent.tarsos.tarsossegmenter.util.io.ConfigurationParser;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

/**
 * <p>
 * A graphical user interface to see and edit the options.
 * This contains a ConfigurationPanel
 * </p>
 * @author Thomas Stubbe
 */
public class OptionsGUI extends JFrame implements ActionListener {

    private JButton loadButton;
    private JButton saveAsButton;

    public OptionsGUI() {
        this.setTitle("Options");
        //this.setLayout(new FlowLayout());
        this.setMinimumSize(new Dimension(350, 550));
        //this.setResizable(false);
        this.setLocationRelativeTo(TarsosSegmenterGui.getInstance());
        this.setVisible(true);
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        JScrollPane sp = new JScrollPane(new ConfigurationPanel());
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        loadButton = new JButton("Load...");
        saveAsButton = new JButton("Save As...");
        loadButton.addActionListener(this);
        saveAsButton.addActionListener(this);
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new FlowLayout());
        southPanel.add(loadButton);
        southPanel.add(saveAsButton);
        contentPane.add(sp, BorderLayout.CENTER);
        contentPane.add(southPanel, BorderLayout.SOUTH);
        this.setContentPane(contentPane);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        //setters aanroepen en conversies uitvoeren

        if (e.getSource() == loadButton) {
            JFileChooser fc = new JFileChooser();
            fc.setAcceptAllFileFilterUsed(false);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.addChoosableFileFilter(new ConfigurationFileFilter());
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                try {
                    ConfigurationParser.loadConfigurationFile(file);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Couldn't parse the configuration file. Lines in the configuration file should be of the form x=y", "ERROR: I/O Exception", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JFileChooser fc = new JFileChooser();
            fc.setAcceptAllFileFilterUsed(false);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.addChoosableFileFilter(new ConfigurationFileFilter());
            int returnVal = fc.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                String filePath = file.getPath();
                if (!filePath.toLowerCase().endsWith(".ini")) {
                    file = new File(filePath + ".ini");
                }
                try {
                    ConfigurationParser.saveConfigurationFile(file);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Couldn't parse the configuration file. Do you have access to the file?", "ERROR: I/O Exception", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
