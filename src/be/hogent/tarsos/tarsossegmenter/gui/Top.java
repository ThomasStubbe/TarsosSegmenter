package be.hogent.tarsos.tarsossegmenter.gui;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.controller.listeners.AudioFileListener;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * <p> A graphical user interface containing the songs name and a button to analyse the song's segmentation
 *
 * @author Thomas Stubbe
 *
 */
public class Top extends JPanel implements AudioFileListener {

    private JLabel songLabel;
    private JButton runButton;

    public Top() {
        super();
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.setBorder(new EmptyBorder(5, 0, 0, 0));
        AASModel.getInstance().addAudioFileChangedListener(this);

        runButton = new JButton("Analyse");
        runButton.setPreferredSize(new Dimension(80, 20));
        runButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                AASModel.getInstance().run();
            }
        });
        runButton.setEnabled(false);


        songLabel = new JLabel("Song: ");
        songLabel.setPreferredSize(new Dimension(250, 20));

        this.add(runButton);
        this.add(songLabel);
    }

    @Override
    public void audioFileChanged() {
        runButton.setEnabled(true);
        songLabel.setText("Song: " + AASModel.getInstance().getAudioFile().originalBasename());
    }
}
