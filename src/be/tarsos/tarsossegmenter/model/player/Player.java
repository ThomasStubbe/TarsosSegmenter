/*
 *
 *  Tarsos is developed by Joren Six at 
 *  The Royal Academy of Fine Arts & Royal Conservatory,
 *  University College Ghent,
 *  Hoogpoort 64, 9000 Ghent - Belgium
 *  
 *  http://tarsos.0110.be/tag/TarsosDSP
 *
 */
package be.tarsos.tarsossegmenter.model.player;

import be.hogent.tarsos.dsp.*;
import be.hogent.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.*;

public class Player implements AudioProcessor {

    private PropertyChangeSupport support = new PropertyChangeSupport(this);
    private PlayerState state;
    private File loadedFile;
    //private GainProcessor gainProcessor;
    private AudioPlayer audioPlayer;
    private WaveformSimilarityBasedOverlapAdd wsola;
    private AudioDispatcher dispatcher;
    //private EventListenerList listenerList = new EventListenerList();
    private final List<AudioProcessor> processorsBeforeTimeStretching;
    private double durationInSeconds;
    private double currentTime;
    private double startAt;
    //private double gain;
    //private double tempo;
    private StopAudioProcessor stopAudioProcessor;
    private float startSelection;
    private float endSelection;
    private boolean repeatSelection;

    private Player() {
        state = PlayerState.NO_FILE_LOADED;
        repeatSelection = false;
        startSelection = 0;
        endSelection = 0;
        processorsBeforeTimeStretching = new ArrayList<AudioProcessor>();
    }

    public void load(File file) {
        if (state != PlayerState.NO_FILE_LOADED) {
            eject();
        }
        loadedFile = file;
        AudioFileFormat fileFormat;
        try {
            fileFormat = AudioSystem.getAudioFileFormat(loadedFile);
        } catch (Exception e) {
            throw new Error(e);
        }
        AudioFormat format = fileFormat.getFormat();
        durationInSeconds = fileFormat.getFrameLength() / format.getFrameRate();
        startAt = 0;
        currentTime = 0;
        this.startSelection = 0;
        this.endSelection = 0;
        setState(PlayerState.STOPPED);
    }

    public void eject() {
        loadedFile = null;
        stop();
        setState(PlayerState.NO_FILE_LOADED);
    }

    public void play() {
        checkIfFileIsLoaded();
        play(startAt);
    }

    public boolean hasFileLoaded() {
        if (state == PlayerState.NO_FILE_LOADED) {
            return false;
        }
        return true;
    }

    private void checkIfFileIsLoaded() {
        if (state == PlayerState.NO_FILE_LOADED) {
            throw new IllegalStateException("Can not play when no file is loaded");
        }
    }

    public void play(double startTime) {
        checkIfFileIsLoaded();
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(loadedFile);
            AudioFormat format = fileFormat.getFormat();

            audioPlayer = new AudioPlayer(format);
            wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.slowdownDefaults(1.0f, format.getSampleRate()));

            dispatcher = AudioDispatcher.fromFile(loadedFile, wsola.getInputBufferSize(), wsola.getOverlap());

            wsola.setDispatcher(dispatcher);
            dispatcher.skip(startTime);
            dispatcher.addAudioProcessor(this);
            for (AudioProcessor processor : processorsBeforeTimeStretching) {
                dispatcher.addAudioProcessor(processor);
            }
            dispatcher.addAudioProcessor(wsola);
            dispatcher.addAudioProcessor(audioPlayer);

            Thread t = new Thread(dispatcher, "Audio Player Thread");
            t.start();
            setState(PlayerState.PLAYING);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public void pauze() {
        pauze(currentTime);
    }

    public void pauze(double pauzeAt) {
        checkIfFileIsLoaded();
        startAt = pauzeAt;
        if (state == PlayerState.PLAYING) {
            //set the state first to stop
            setState(PlayerState.STOPPED);
            //then stop the dispatcher:
            //  to prevent recursive call in processingFinished
            dispatcher.stop();
        } else if (state != PlayerState.STOPPED) {
            throw new IllegalStateException("Can not stop when nothing is playing");
        }
        //onPlayerStateChanged();
    }

    public void stop() {
        pauze(0);
    }

    private void setState(PlayerState newState) {
        PlayerState oldState = state;
        state = newState;
        support.firePropertyChange("state", oldState, newState);
        //onPlayerStateChanged();
    }

    public double getDurationInSeconds() {
        checkIfFileIsLoaded();
        return durationInSeconds;
    }

    public PlayerState getState() {
        return state;
    }

    public double getStartAt() {
        return startAt;
    }

    public void addProcessorBeforeTimeStrechting(AudioProcessor processor) {
        processorsBeforeTimeStretching.add(processor);
    }

    public boolean removeProcessorBeforeTimeStretching(AudioProcessor processor) {
        return processorsBeforeTimeStretching.remove(processor);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        support.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        support.removePropertyChangeListener(l);
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        currentTime = audioEvent.getTimeStamp();
        return true;
    }

    @Override
    public void processingFinished() {
        if (state == PlayerState.PLAYING) {
            stop();
        }
    }
    private static Player instance;

    public static Player getInstance() {
        if (instance == null) {
            instance = new Player();
        }
        return instance;
    }

    /**
     * Increase the gain by a defined percentage.
     *
     * @param percent to increase the gain.
     */
//    public void increaseGain(double percent) {
//        double newGain = gain + percent;
//        if (newGain >= 0 && newGain <= 2.0) {
//            setGain(newGain);
//        }
//    }
//    public void increaseTempo(double percent) {
//        double newTempo = tempo + percent;
//        if (newTempo >= 0 && newTempo <= 3.0) {
//            setTempo(newTempo);
//        }
//    }
    /**
     * At this point the sound that is played stops automatically.
     *
     * @param stopAt The time at which the player stops (in seconds).
     */
    public void setStopAt(double stopAt) {
        if (stopAt > durationInSeconds) {
            stopAt = durationInSeconds;
        }
        if (stopAudioProcessor == null) {
            stopAudioProcessor = new StopAudioProcessor(stopAt);
        } else {
            stopAudioProcessor.setStopTime(stopAt);
        }
    }

    /**
     * Returns the time at which the audio play back stops automatically.
     *
     * @return The time at which the audio play back stops automatically.
     */
    public double getStopAt() {
        return stopAudioProcessor.getStopAt();
    }

    public float getEndSelection() {
        return endSelection;
    }

    public void setEndSelection(float endSelection) {
        this.endSelection = endSelection;
        if (endSelection < this.currentTime) {
            if (this.getState() == PlayerState.PLAYING) {
                this.pauze();
                this.play(startSelection);
            } else {
                this.play(startSelection);
                this.pauze(startSelection);
            }
        }
    }

    public float getStartSelection() {
        return startSelection;
    }

    public void setStartSelection(float startSelection) {
        this.startSelection = startSelection;
        if (startSelection > this.currentTime) {
            if (this.getState() == PlayerState.PLAYING) {
                this.pauze();
                this.play(startSelection);
            } else {
                this.play(startSelection);
                this.pauze(startSelection);
            }
        }
    }

    public void setRepeatLoop(boolean b) {
        this.repeatSelection = b;
    }

    public void selectionPast() {
        if (repeatSelection && this.getState() == PlayerState.PLAYING) {
            this.pauze();
            this.play(this.startSelection);
        } else {
            if (this.getState() == PlayerState.PLAYING) {
                this.pauze();
                this.play(this.startSelection);
                this.pauze(this.startSelection);
            } else {
                this.play(this.startSelection);
                this.pauze(this.startSelection);
            }
        }
    }

    public void resetSelection() {
        this.startSelection = 0;
        this.endSelection = 0;
    }
}
/**
 * Loads a new audio file. Throws an error if the audio format is not
 * recognized.
 *
 * @param file The audio file to load.
 */
//    public void addEventListener(PlayerListener listener) {
//        listenerList.add(PlayerListener.class, listener);
//    }
//
//    // This methods allows classes to unregister for MyEvents
//    public void removeEventListener(PlayerListener listener) {
//        listenerList.remove(PlayerListener.class, listener);
//    }
//
//    private void onPlayerStateChanged() {
//        Object[] listeners = listenerList.getListenerList();
//        // Each listener occupies two elements - the first is the listener class
//        // and the second is the listener instance
//        for (int i = 0; i < listeners.length; i += 2) {
//            if (listeners[i] == AASModelListener.class) {
//                ((PlayerListener) listeners[i + 1]).stateChanged();
//            }
//        }
//    }

