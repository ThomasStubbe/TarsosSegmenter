/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.tarsos.tarsossegmenter.gui;

import be.tarsos.tarsossegmenter.controller.listeners.AASModelListener;
import be.tarsos.tarsossegmenter.controller.listeners.AudioFileListener;
import be.tarsos.tarsossegmenter.model.AASModel;
import be.tarsos.tarsossegmenter.model.player.Player;
import be.tarsos.tarsossegmenter.model.segmentation.Segmentation;
import be.tarsos.tarsossegmenter.model.segmentation.SegmentationPart;
import be.tarsos.tarsossegmenter.util.TimeUnit;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

/**
 * <p> A graphical user interface displaying the structure of the song. </p>
 *
 * @author Thomas Stubbe
 *
 */
public class AudioStructureGUI extends JPanel implements MouseListener, AASModelListener, AudioFileListener {

    private static float songduration;
    private AASModel model;
    //private JPopupMenu popupMenu;
    Segmentation segmentation;

    /**
     * Constructs the AudioSegmentationGUI
     *
     * @param parent A reference to its parent (SegmentedWaveFormWrapper)
     */
    public AudioStructureGUI() {
        super();
        this.model = AASModel.getInstance();
        model.addModelListener(this);
        model.addAudioFileChangedListener(this);

        this.addMouseListener(this);
        segmentation = model.getSegmentation();
        this.setVisible(false);
    }

    @Override
    public void paint(final Graphics g) {
        super.paint(g);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        FontMetrics fm = g.getFontMetrics();

        for (int i = 0; i < segmentation.getSegmentation().size(); i++) {
            SegmentationPart macroSP = segmentation.getSegmentation().get(i);
            int begin = (int) (macroSP.getBegin() * 1000 / songduration * this.getWidth());
            int end = (int) (macroSP.getEnd() * 1000 / songduration * this.getWidth());
            g.setColor(macroSP.getColor());
            g.fillRect(begin, 0, end, 15);
            g.setColor(Color.black);
            g.drawRect(begin, 0, end, 15);
            if (macroSP.getLabel() != null && macroSP.getLabel().length() > 0) {
                if ((end - begin) - 2 <= fm.stringWidth(macroSP.getLabel())) {
                    String toPrint = macroSP.getLabel();
                    while (toPrint.length() > 1 && (end - begin) - 2 <= fm.stringWidth(toPrint)) {
                        toPrint = toPrint.substring(0, toPrint.length() - 2);
                    }
                    g.drawString(toPrint, (end + begin - fm.stringWidth(toPrint)) / 2, 27);
                } else {
                    g.drawString(macroSP.getLabel(), (end + begin - fm.stringWidth(macroSP.getLabel())) / 2, 12);
                }

            }
            if (macroSP.hasSubSegmentation()) {
                for (int j = 0; j < macroSP.getSubSegmentation().size(); j++) {
                    SegmentationPart mesoSP = macroSP.getSubSegmentation().get(j);
                    begin = (int) (mesoSP.getBegin() * 1000 / songduration * this.getWidth());
                    end = (int) (mesoSP.getEnd() * 1000 / songduration * this.getWidth());
                    g.setColor(mesoSP.getColor());
                    g.fillRect(begin, 15, end, 15);
                    g.setColor(Color.black);
                    g.drawRect(begin, 15, end, 15);
                    if (mesoSP.getLabel() != null && mesoSP.getLabel().length() > 0) {
                        if ((end - begin) - 2 <= fm.stringWidth(mesoSP.getLabel())) {
                            String toPrint = mesoSP.getLabel();
                            while (toPrint.length() > 1 && (end - begin) - 2 <= fm.stringWidth(toPrint)) {
                                toPrint = toPrint.substring(0, toPrint.length() - 2);
                            }
                            g.drawString(toPrint, (end + begin - fm.stringWidth(toPrint)) / 2, 27);
                        } else {
                            g.drawString(mesoSP.getLabel(), (end + begin - fm.stringWidth(mesoSP.getLabel())) / 2, 27);
                        }
                    }
                    if (mesoSP.hasSubSegmentation()) {
                        for (int k = 0; k < mesoSP.getSubSegmentation().size(); k++) {
                            SegmentationPart microSP = mesoSP.getSubSegmentation().get(k);
                            begin = (int) (microSP.getBegin() * 1000 / songduration * this.getWidth());
                            end = (int) (microSP.getEnd() * 1000 / songduration * this.getWidth());
                            g.setColor(microSP.getColor());
                            g.fillRect(begin, 30, end, 15);
                            g.setColor(Color.black);
                            g.drawRect(begin, 30, end, 15);
                            if (microSP.getLabel() != null && microSP.getLabel().length() > 0) {
                                if ((end - begin) - 2 <= fm.stringWidth(microSP.getLabel())) {
                                    String toPrint = microSP.getLabel();
                                    while (toPrint.length() > 1 && (end - begin) - 2 <= fm.stringWidth(toPrint)) {
                                        toPrint = toPrint.substring(0, toPrint.length() - 2);
                                    }
                                    //if (toPrint.length() > 0) {
                                        g.drawString(toPrint, (end + begin - fm.stringWidth(toPrint)) / 2, 42);
                                    //}
                                } else {
                                    g.drawString(microSP.getLabel(), (end + begin - fm.stringWidth(microSP.getLabel())) / 2, 42);
                                }
                            }
                        }
                    }
                }
            }
        }

        //Zwarte omkadering:
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, this.getWidth() - 1, 15);
        g.drawRect(0, 15, this.getWidth() - 1, 30);
        g.drawRect(0, 30, this.getWidth() - 1, 45);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        double time = (double) e.getX() / (double) this.getWidth() * (double) model.getAudioFile().getLengthIn(TimeUnit.SECONDS);
        int segmentationLevel = AASModel.MACRO_LEVEL;
        if (e.getY() < 15) {
            segmentationLevel = AASModel.MACRO_LEVEL;
        } else if (e.getY() < 30) {
            segmentationLevel = AASModel.MESO_LEVEL;
        } else if (e.getY() < 45) {
            segmentationLevel = AASModel.MICRO_LEVEL;
        }
        SegmentationPart sp = segmentation.searchSegmentationPart(time, segmentationLevel);
        if (sp != null) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Player.getInstance().setStartSelection(sp.getBegin());
                Player.getInstance().setEndSelection(sp.getEnd());
                TarsosSegmenterGui.getInstance().updateWaveFormGUI();
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                sp.split((float) time);
                TarsosSegmenterGui.getInstance().updateWaveFormGUI();
                TarsosSegmenterGui.getInstance().updateSegmentationPartsEditor();
                this.update();
            }
        } else {
            System.out.println("Could not find segmentationPart!");
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void calculationStarted() {
        this.setVisible(false);
    }

    @Override
    public void calculationDone() {
        checkSegmentationAndEditSizeAndVisibility();
        this.repaint();
    }

    public void update() {
        checkSegmentationAndEditSizeAndVisibility();
        this.repaint();
    }

    @Override
    public void audioFileChanged() {
        this.setVisible(false);
        songduration = model.getAudioFile().getLengthInMilliSeconds();
        this.repaint();
    }

    public void checkSegmentationAndEditSizeAndVisibility() {
        if (!segmentation.isEmpty()) {
            int height = 15;
            if (segmentation.getSegmentation().hasSubSegmentation()) {
                height += 15;
                if (segmentation.getSegmentation().hasSubSubSegmentation()) {
                    height += 15;
                }
            }
            this.setPreferredSize(new Dimension(0, height));
            this.setVisible(true);
        } else {
            setVisible(false);
        }
    }
}
