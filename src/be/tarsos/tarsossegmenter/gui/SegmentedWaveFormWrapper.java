package be.tarsos.tarsossegmenter.gui;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * <p>
 * A graphical user interface containing the SegmentedWaveForm, the AudioStructureGUI and the PlayerGUI
 * </p>
 * @author Thomas Stubbe
 */

public final class SegmentedWaveFormWrapper extends JPanel {
    
    //private JPanel topPanel;
    private JPanel centerPanel;
    private PlayerGUI playerGUI;
    private AudioStructureGUI asg;
    private SegmentedWaveForm waveForm;
    //private JPanel wavePanel;
    
    public SegmentedWaveFormWrapper(){        this.waveForm = new SegmentedWaveForm();
        this.asg = new AudioStructureGUI();
        this.setLayout(new BorderLayout());
        playerGUI = new PlayerGUI();
        centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(asg, BorderLayout.NORTH);
        centerPanel.add(waveForm, BorderLayout.CENTER);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 7)); 
        //wavePanel.add(this)
        this.add(playerGUI, BorderLayout.NORTH);
        this.add(centerPanel, BorderLayout.CENTER);
        
        //this.add(topPanel, BorderLayout.NORTH);
        //this.add(waveForm, BorderLayout.CENTER);
    }
    
//    public void audioFileChanged(AudioFile audioFile){
//        waveForm.audioFileChanged(audioFile);
//    }
    
    public void updateAudioStructureGUI(){
        asg.update();
    }
    
    public void updateWaveForm(){
        waveForm.update();
    }
    
}