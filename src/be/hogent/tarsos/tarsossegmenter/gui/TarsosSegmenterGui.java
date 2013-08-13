/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import be.hogent.tarsos.tarsossegmenter.util.io.SegmentationFileFilter;
import be.hogent.tarsos.tarsossegmenter.util.io.SegmentationFileParser;
import be.hogent.tarsos.tarsossegmenter.util.io.SongFileFilter;
import be.hogent.tarsos.tarsossegmenter.controller.listeners.AASModelListener;
import be.hogent.tarsos.tarsossegmenter.controller.listeners.AudioFileListener;
import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;

/**
 * <p>
 * The main graphical user interface. This contains all other GUI's.
 * </p>
 *
 * @author Thomas Stubbe
 *
 */
public class TarsosSegmenterGui extends JFrame implements AASModelListener, AudioFileListener, ItemListener {

    private Top top;
    private SegmentedWaveFormWrapper waveFormWrapper;
    private MatrixGUI mg;
    private JPanel mgPanel;
    private AASModel model;
    private JSplitPane vSplitPane;
    private boolean isCalculating;
    private SegmentationPartsEditor spe;
    private ImageOptionsGUI imageOptionsGui;
    private static TarsosSegmenterGui instance;
    private JCheckBoxMenuItem toggleMatrix;

    public static TarsosSegmenterGui getInstance() {
        if (instance == null) {
            instance = new TarsosSegmenterGui();
            instance.initialise();
        }
        return instance;
    }

    /**
     * Constructs the main graphical user interface
     *
     */
    private TarsosSegmenterGui() {
        super();
    }

    private void initialise() {
        this.model = AASModel.getInstance();
        this.model.addModelListener(this);
        this.model.addAudioFileChangedListener(this);
        isCalculating = false;
        construct();
    }

    private void construct() {

        this.setMinimumSize(new Dimension(900, 400));
        this.setPreferredSize(new Dimension(900, 800));
        this.getContentPane().setLayout(new BorderLayout());
        this.setTitle("Tarsos - Automatic Audio Segmentation");
        this.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("be/hogent/tarsos/aas/gui/resources/tarsos_logo_small.png")).getImage());
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        vSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        vSplitPane.setDividerLocation(150);
        vSplitPane.setDividerSize(3);
        vSplitPane.setMaximumSize(new Dimension(300, 300));

        waveFormWrapper = new SegmentedWaveFormWrapper();

        mg = new MatrixGUI();

        mgPanel = new JPanel();
        mgPanel.setLayout(new BorderLayout());
        mgPanel.add(mg, BorderLayout.CENTER);
        mgPanel.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 7));
        mgPanel.setVisible(false);

        vSplitPane.setTopComponent(waveFormWrapper);
        vSplitPane.setBottomComponent(mgPanel);

        this.setJMenuBar(createMenu());


        top = new Top();
        //this.getContentPane().add(top, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        imageOptionsGui = new ImageOptionsGUI();
        spe = new SegmentationPartsEditor();
        leftPanel.add(top, BorderLayout.NORTH);
        leftPanel.add(spe, BorderLayout.CENTER);
        leftPanel.add(imageOptionsGui, BorderLayout.SOUTH);

        this.getContentPane().add(leftPanel, BorderLayout.WEST);

        this.getContentPane().add(vSplitPane, BorderLayout.CENTER);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    @Override
    public void calculationStarted() {
        toggleMatrix.setSelected(false);
        toggleMatrix.setEnabled(false);
        isCalculating = true;
    }

    @Override
    public void calculationDone() {
        isCalculating = false;
        toggleMatrix.setEnabled(true);
        toggleMatrix.setSelected(true);
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getStateChange() == ItemEvent.DESELECTED) {
            mgPanel.setVisible(false);
        } else {
            if (AASModel.getInstance().getSimilarityMatrix() != null && !isCalculating) {
                mgPanel.setVisible(true);
                
                vSplitPane.setDividerLocation(Math.min(vSplitPane.getHeight() / 2, 220));
            } else {
                toggleMatrix.setSelected(false);
            }
        }
    }

    /**
     * Updates the GUI containing the visualization of the segmentation, the GUI
     * containing the waveform and the GUI to edit the segmentationparts
     *
     */
    public void update() {
        waveFormWrapper.updateAudioStructureGUI();
        waveFormWrapper.updateWaveForm();
        spe.update();
    }

    @Override
    public void audioFileChanged() {
        toggleMatrix.setSelected(false);
        toggleMatrix.setEnabled(false);
        isCalculating = true;
    }

    /**
     * Updates the GUI showing the options for image processing.
     */
    public void updateImageOptionsGUI() {
        imageOptionsGui.update();
    }

    public void updateAudioStructureGUI() {
        waveFormWrapper.updateAudioStructureGUI();
    }

    public void updateWaveFormGUI() {
        waveFormWrapper.updateWaveForm();
    }

    /**
     * Updates the GUI to edit the segmentationparts
     */
    public void updateSegmentationPartsEditor() {
        spe.update();
    }

    private JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem loadSongMenuItem = new JMenuItem("Load song...");
        loadSongMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setAcceptAllFileFilterUsed(false);
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.addChoosableFileFilter(new SongFileFilter());
                File dir = Configuration.getFile(ConfKey.dir_history);
                if (dir.exists() && dir.isDirectory()) {
                    fc.setCurrentDirectory(dir);
                }
                int returnVal = fc.showOpenDialog(TarsosSegmenterGui.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    //@TODO
                    //runButton.setEnabled(false);
                    File file = fc.getSelectedFile();
                    Configuration.set(ConfKey.dir_history, file.getParent());
                    model.setNewAudioFile(file);
                }
            }
        });

        JMenuItem loadSegmentationMenuItem = new JMenuItem("Load segmentation file...");
        loadSegmentationMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (AASModel.getInstance().getAudioFile() != null) {
                    JFileChooser fc = new JFileChooser();
                    fc.setAcceptAllFileFilterUsed(false);
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fc.addChoosableFileFilter(new SegmentationFileFilter());
                    File dir = Configuration.getFile(ConfKey.dir_history);
                    if (dir.exists() && dir.isDirectory()) {
                        fc.setCurrentDirectory(dir);
                    }
                    int returnVal = fc.showOpenDialog(TarsosSegmenterGui.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        model.getSegmentation().clearAll();
                        Configuration.set(ConfKey.dir_history, file.getParent());
                        if (file.getName().toLowerCase().endsWith(".textgrid")) {
                            SegmentationFileParser.parseFile(file.getAbsolutePath(), model.getSegmentation());
                        } else if (file.getName().toLowerCase().endsWith(".csv")) {
                            SegmentationFileParser.parseCSVFile(file.getAbsolutePath(), model.getSegmentation());
                        }
                        TarsosSegmenterGui.getInstance().update();
                    }
                } else {
                    JOptionPane.showMessageDialog(TarsosSegmenterGui.this, "Please open a soundfile first!", "Error: No Soundfile found", JOptionPane.ERROR_MESSAGE);
                }
            } 
        });

        JMenuItem saveMenuItem = new JMenuItem("Save segmentation as...");
        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setAcceptAllFileFilterUsed(false);
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.addChoosableFileFilter(SegmentationFileFilter.getTextGridFileFilter());
                fc.addChoosableFileFilter(SegmentationFileFilter.getCSVFileFilter());

                File dir = Configuration.getFile(ConfKey.dir_history);
                if (dir.exists() && dir.isDirectory()) {
                    fc.setCurrentDirectory(dir);
                }

                int returnVal = fc.showSaveDialog(TarsosSegmenterGui.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    String extension = fc.getFileFilter().getDescription();
                    Configuration.set(ConfKey.dir_history, file.getParent());
                    if (extension.equals("*.TextGrid")) {
                        SegmentationFileParser.writeToFile(file.getParent() + "/" + file.getName().split("\\.")[0] + ".TextGrid", model.getSegmentation());
                    }
                    if (extension.equals("*.csv")) {
                        SegmentationFileParser.writeToCSVFile(file.getParent() + "/" + file.getName().split("\\.")[0] + ".csv", model.getSegmentation());
                    }
                }
            }
        });


        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TarsosSegmenterGui.this.dispose();
            }
        });

        fileMenu.add(loadSongMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(loadSegmentationMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        JMenu optionMenu = new JMenu("Settings");

        JMenuItem optionMenuItem = new JMenuItem("Options...");
        optionMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new OptionsGUI();
            }
        });

        toggleMatrix = new JCheckBoxMenuItem("Show self-similarity matrix");
        toggleMatrix.addItemListener(TarsosSegmenterGui.this);
        toggleMatrix.setSelected(false);
        toggleMatrix.setEnabled(false);

        final JCheckBoxMenuItem initialMatrixMenuItem = new JCheckBoxMenuItem("Display initial matrix");
        initialMatrixMenuItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                mg.setDisplayInitialMatrix(initialMatrixMenuItem.isSelected());
            }
        });

        optionMenu.add(optionMenuItem);
        optionMenu.addSeparator();
        optionMenu.add(toggleMatrix);
        optionMenu.add(initialMatrixMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(optionMenu);

        return menuBar;
    }
}
