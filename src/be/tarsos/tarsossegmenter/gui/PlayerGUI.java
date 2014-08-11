package be.tarsos.tarsossegmenter.gui;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.tarsos.tarsossegmenter.controller.listeners.AudioFileListener;
import be.tarsos.tarsossegmenter.model.AASModel;
import be.tarsos.tarsossegmenter.model.player.Player;
import be.tarsos.tarsossegmenter.model.player.PlayerState;
import be.tarsos.tarsossegmenter.util.TimeUnit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <p> A graphical user interface to control the audio player. </p>
 *
 * @author Joren Six, Thomas Stubbe
 */
public class PlayerGUI extends JPanel implements AudioFileListener {

    private JButton playButton;
    private JSlider positionSlider;
    //position value in the slider
    private int newPositionValue;
    private JLabel progressLabel;
    private JLabel totalLabel;
    private Player player;
    private JCheckBox loopCheckBox;
    private PropertyChangeListener stateChanged = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("state".equals(evt.getPropertyName())) {
                PlayerState newState = (PlayerState) evt.getNewValue();
                playButton.setEnabled(newState != PlayerState.NO_FILE_LOADED);
                positionSlider.setEnabled(newState != PlayerState.NO_FILE_LOADED);
                if (newState == PlayerState.PLAYING) {
                    playButton.setText("Pauze");
                } else if (newState == PlayerState.STOPPED) {
                    playButton.setText("Play");
                }
            }
        }
    };
    final AudioProcessor reportProgressProcessor = new AudioProcessor() {

        @Override
        public boolean process(AudioEvent audioEvent) {
            double timeStamp = audioEvent.getTimeStamp();
            if (!positionSlider.getValueIsAdjusting()) {
                newPositionValue = (int) (audioEvent.getProgress() * 1000);
                positionSlider.setValue(newPositionValue);
                setProgressLabelText(timeStamp, player.getDurationInSeconds());
            }
            return true;
        }

        @Override
        public void processingFinished() {
            newPositionValue = (int) (player.getStartAt() / player.getDurationInSeconds() * 1000);
            positionSlider.setValue(newPositionValue);
        }
    };

    public PlayerGUI() {
        this.player = Player.getInstance();
        AASModel.getInstance().addAudioFileChangedListener(this);
        playButton = new JButton("Play");
        playButton.setEnabled(false);
        playButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                if (player.getState() == PlayerState.PLAYING) {
                    player.pauze();
                } else if (player.getState() == PlayerState.STOPPED) {
                    player.play();
                }
            }
        });
        createProgressSlider();
        doGroupLayout();
        player.addProcessorBeforeTimeStrechting(reportProgressProcessor);
        player.addPropertyChangeListener(stateChanged);
    }

    private void doGroupLayout() {
        JPanel topPanel = new JPanel();
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel centerPanel = new JPanel();
        JPanel rightPanel = new JPanel(new BorderLayout());
        this.setLayout(new BorderLayout());
        
        playButton.setPreferredSize(new Dimension(65, 20));

        loopCheckBox = new JCheckBox();
        loopCheckBox.setText("Loop selection");

        loopCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (loopCheckBox.isSelected()) {
                    player.setRepeatLoop(true);
                } else {
                    player.setRepeatLoop(false);
                }
            }
        });
        
        JButton deselectButton = new JButton("Deselect");
        deselectButton.setPreferredSize(new Dimension(85, 20));
        deselectButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                Player.getInstance().resetSelection();
                TarsosSegmenterGui.getInstance().updateWaveFormGUI();
            }
            
        });
        
        leftPanel.add(progressLabel, BorderLayout.CENTER);
        rightPanel.add(totalLabel, BorderLayout.CENTER);
        centerPanel.add(playButton);
        centerPanel.add(loopCheckBox);
        centerPanel.add(deselectButton);
        topPanel.setLayout(new BorderLayout());
        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);
        topPanel.add(centerPanel, BorderLayout.CENTER);
        this.add(topPanel, BorderLayout.NORTH);
        this.add(positionSlider, BorderLayout.CENTER);
    }

    private void createProgressSlider() {
        positionSlider = new JSlider(0, 1000);
        positionSlider.setValue(0);
        positionSlider.setPaintLabels(false);
        positionSlider.setPaintTicks(false);
        positionSlider.setEnabled(false);

        positionSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent arg0) {
                if (newPositionValue != positionSlider.getValue()) {
                    double promille = positionSlider.getValue() / 1000.0;
                    double currentPosition = player.getDurationInSeconds() * promille;
                    if (positionSlider.getValueIsAdjusting()) {
                        setProgressLabelText(currentPosition, player.getDurationInSeconds());
                    } else {
                        double secondsToSkip = currentPosition;
                        PlayerState currentState = player.getState();
                        player.pauze(secondsToSkip);
                        if (currentState == PlayerState.PLAYING) {
                            player.play();
                        }
                    }

                }
                if (player.getState() == PlayerState.PLAYING && player.getEndSelection() > player.getStartSelection() && (float) positionSlider.getValue() / 1000f * AASModel.getInstance().getAudioFile().getLengthIn(TimeUnit.SECONDS) >= Player.getInstance().getEndSelection()) {
                    player.selectionPast();
                }
            }
        });

        progressLabel = new JLabel();
        totalLabel = new JLabel();
        if (player.hasFileLoaded()) {
            setProgressLabelText(0, player.getDurationInSeconds());
        } else {
            setProgressLabelText(0, 0);
        }
    }

    private void setProgressLabelText(double current, double max) {
        progressLabel.setText(formattedToString(current));
        totalLabel.setText(formattedToString(max));
    }

    public String formattedToString(double seconds) {
        int minutes = (int) (seconds / 60);
        int completeSeconds = (int) seconds - (minutes * 60);
        int hundred = (int) ((seconds - (int) seconds) * 100);
        return String.format(Locale.US, "%02d:%02d:%02d", minutes, completeSeconds, hundred);
    }

    @Override
    public void audioFileChanged() {
        setProgressLabelText(0, player.getDurationInSeconds());
        loopCheckBox.setSelected(false);
        this.revalidate();
    }
}
