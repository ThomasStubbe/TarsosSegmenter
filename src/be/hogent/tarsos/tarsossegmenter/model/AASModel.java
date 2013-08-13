package be.hogent.tarsos.tarsossegmenter.model;

import be.hogent.tarsos.tarsossegmenter.controller.listeners.AASModelListener;
import be.hogent.tarsos.tarsossegmenter.controller.listeners.AudioFileListener;
import be.hogent.tarsos.tarsossegmenter.gui.TarsosSegmenterGui;
import be.hogent.tarsos.tarsossegmenter.gui.BackgroundTask;
import be.hogent.tarsos.tarsossegmenter.gui.ProgressDialog;
import be.hogent.tarsos.tarsossegmenter.model.player.Player;
import be.hogent.tarsos.tarsossegmenter.model.segmentation.Segmentation;
import be.hogent.tarsos.tarsossegmenter.model.structure.StructureDetection;
import be.hogent.tarsos.tarsossegmenter.util.NoveltyScore;
import be.hogent.tarsos.tarsossegmenter.util.TimeUnit;
import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;
import be.hogent.tarsos.tarsossegmenter.util.io.FileUtils;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AutoCorrelation;
import be.hogent.tarsos.dsp.ConstantQ;
import be.hogent.tarsos.dsp.mfcc.MFCC;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;
import javax.swing.event.EventListenerList;

/**
 * <p>
 * The main graphical user interface. This contains all other GUI's.
 * </p>
 *
 * @author Thomas Stubbe
 *
 */

public class AASModel {

	public static final float CQT_THRESHOLD = 0.0008f;
    public static float MAX_SCALE_VALUE = 1000;
    public static final int MACRO_LEVEL = 0;
    public static final int MESO_LEVEL = 1;
    public static final int MICRO_LEVEL = 2;
//    public static final String RESOURCESPATH = "resources/";
//    public static final String AUDIOFILESPATH = RESOURCESPATH + "structureset/";
//    public static final String AUDIOFILESPATH2 = RESOURCESPATH + "structureset2/";
//    public static final String CONFIGURATIONPATH = RESOURCESPATH + "configuration/";
//    public static final String SEGMENTATIONPATH = AUDIOFILESPATH + "SegmentationFiles/";
//    public static final String GROUNDTRUTHPATH = SEGMENTATIONPATH + "GroundTruth/";
    private String segmenationFileLocation;
    private String segmenationRefFileLocation;
    private boolean useMFCC;
    private boolean useAutoCorrelation;
    private boolean useCQT;
    private AudioFile audioFile;
    private int amountOfFrames;
    private int frameSize; //frameSize
    private int overlapping;
    private float sampleRate;
    private float lowerFilterFreq;
    private float upperFilterFreq;
    static int samplesPerPixel;
    private int mfccCoef;
    private int melfilters;
    private int cqtBins;
    private AudioDispatcher ad;
    //CoeficientenMatrices
    private float[][] mfccs;
    private float[][] cqtcs;
    private float[] autoCorrelationcs;
    private float[][] similarityMatrix;
    private float[][] resultMatrix;
    private float[][] noveltyScores;
    private EventListenerList modelListenerList = new EventListenerList();
    private EventListenerList audioFileListenerList = new EventListenerList();
//    private int macroSegmentationIndex;
//    private int mesoSegmentationIndex;
//    private int microSegmentationIndex;
//    private ArrayList<ArrayList<SegmentationPart>> bestMacroSegmentations;
//    private ArrayList<ArrayList<SegmentationPart>> bestMesoSegmentations;
//    private ArrayList<ArrayList<SegmentationPart>> bestMicroSegmentations;
    private boolean macroEnabled;
    private boolean mesoEnabled;
    private boolean microEnabled;
    private static AASModel instance;
    private boolean guiEnabled;
    private boolean onlyStructureDetection;
    //private int segmentationLevel;
    private Segmentation segmentation;
    public static final boolean test = true;

//    public int getSegmentationLevel() {
//        return segmentationLevel;
//    }
//    public void setSegmentationLevel(int segmentationLevel) {
//        this.segmentationLevel = segmentationLevel;
//    }
//    public SegmentationList getActiveSegmentation() {
//        return segmentation.getSegmentation();
//    }
    public static AASModel getInstance() {
        if (instance == null) {
            instance = new AASModel();
        }
        return instance;
    }

    private AASModel() {
        guiEnabled = true;
        onlyStructureDetection = false;
        segmentation = new Segmentation();
        loadConfiguration();
    }

    public void setGuiEnabled(boolean value) {
        this.guiEnabled = value;
    }

    public final void loadConfiguration() {
        boolean oldUseMFCC = useMFCC;
        boolean oldUseAutoCorrelation = useAutoCorrelation;
        boolean oldUseCQT = useCQT;
        int oldFrameSize = frameSize;
        int oldOverlapping = overlapping;
        int oldCqtBins = cqtBins;
        int oldMfccCoef = mfccCoef;
        int oldMelfilters = melfilters;
        float oldLowerFilterFreq = lowerFilterFreq;
        float oldUpperFilterFreq = upperFilterFreq;

        useMFCC = Configuration.getBoolean(ConfKey.enable_mfcc);
        useAutoCorrelation = Configuration.getBoolean(ConfKey.enable_autocorrelation);
        useCQT = Configuration.getBoolean(ConfKey.enable_cqt);
        frameSize = Configuration.getInt(ConfKey.framesize);
        overlapping = Configuration.getInt(ConfKey.overlapping);
        cqtBins = Configuration.getInt(ConfKey.cqt_bins);
        mfccCoef = Configuration.getInt(ConfKey.mfcc_coef);
        macroEnabled = Configuration.getBoolean(ConfKey.enable_macro);
        mesoEnabled = Configuration.getBoolean(ConfKey.enable_meso);
        microEnabled = Configuration.getBoolean(ConfKey.enable_micro);
        melfilters = Configuration.getInt(ConfKey.mfcc_melfilters);
        lowerFilterFreq = Configuration.getInt(ConfKey.lowfilterfreq);
        upperFilterFreq = Configuration.getInt(ConfKey.upperfilterfreq);

        if (onlyStructureDetection == true
                && oldUseMFCC == useMFCC
                && oldUseAutoCorrelation == useAutoCorrelation
                && oldUseCQT == useCQT
                && oldFrameSize == frameSize
                && oldOverlapping == overlapping
                && oldCqtBins == cqtBins
                && oldMfccCoef == mfccCoef
                && oldMelfilters == melfilters
                && oldLowerFilterFreq == lowerFilterFreq
                && oldUpperFilterFreq == upperFilterFreq) {
            this.onlyStructureDetection = true;
        } else {
            this.onlyStructureDetection = false;
        }

        if (audioFile != null) {
            amountOfFrames = audioFile.fileFormat().getFrameLength() / (frameSize - overlapping);
            samplesPerPixel = (int) Math.pow(2, (int) Math.floor(Math.log(audioFile.fileFormat().getFrameLength() / 800) / Math.log(2)));
        }
    }

    public void calculate() throws java.lang.OutOfMemoryError {
        if (frameSize <= 8192 && audioFile.getLengthIn(TimeUnit.MINUTES) > 16){
            int result = JOptionPane.showConfirmDialog(TarsosSegmenterGui.getInstance(), "Analysing this audiofile with a framesize <= 8192 would be very intensive for the CPU and memory.\nWould you like to set the framesize to 16384?", "Warning: Intensive task", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result != JOptionPane.CANCEL_OPTION){
                if (result == JOptionPane.YES_OPTION){
                    Configuration.set(ConfKey.framesize, 16384);
                    loadConfiguration();
                }
            } else {
                return;
            }
        } else if (frameSize <= 4096 && audioFile.getLengthIn(TimeUnit.MINUTES) > 12){
            int result = JOptionPane.showConfirmDialog(TarsosSegmenterGui.getInstance(), "Analysing this audiofile with a framesize <= 4096 would be very intensive for the CPU and memory.\nWould you like to set the framesize to 8192?", "Warning: Intensive task", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result != JOptionPane.CANCEL_OPTION){
                if (result == JOptionPane.YES_OPTION){
                    Configuration.set(ConfKey.framesize, 8192);
                    loadConfiguration();
                }
            } else {
                return;
            }
        } else if (frameSize <= 2048 && audioFile.getLengthIn(TimeUnit.SECONDS) > 360){
            int result = JOptionPane.showConfirmDialog(TarsosSegmenterGui.getInstance(), "Analysing this audiofile with a framesize <= 2048 would be very intensive for the CPU and memory.\nWould you like to set the framesize to 4096?", "Warning: Intensive task", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result != JOptionPane.CANCEL_OPTION){
                if (result == JOptionPane.YES_OPTION){
                    Configuration.set(ConfKey.framesize, 4096);
                    loadConfiguration();
                }
            } else {
                return;
            }
        }

        if (!onlyStructureDetection) {
            try {
                File file = new File(audioFile.transcodedPath());
                    ad = AudioDispatcher.fromFile(file, audioFile.fileFormat().getFrameLength(), 0);
            } catch (UnsupportedAudioFileException | IOException e) {
                JOptionPane.showMessageDialog(TarsosSegmenterGui.getInstance(), "Could not transcode audiofile: make sure it is an audiofile and that you have access/rights to the file", "Error", JOptionPane.ERROR_MESSAGE);
            }

            MFCC mfccAD = null;
            ConstantQ cqtAD = null;
            AutoCorrelation acAD = null;

            if (useMFCC) {
                mfccAD = new MFCC(this.frameSize, this.sampleRate, this.melfilters, this.mfccCoef, this.lowerFilterFreq, this.upperFilterFreq);
                ad.addAudioProcessor(mfccAD);
            }
            if (useAutoCorrelation) {
                acAD = new AutoCorrelation();
                ad.addAudioProcessor(acAD);
            }
            if (useCQT) {
                cqtAD = new ConstantQ(sampleRate, lowerFilterFreq, upperFilterFreq, cqtBins);
                ad.addAudioProcessor(cqtAD);
            }
                ad.setStepSizeAndOverlap(frameSize, overlapping);
            ad.run();

//            if (useMFCC) {
//                mfccs = mfccAD.getMFCC();
//                ad.removeAudioProcessor(mfccAD);
//            }
//            if (useAutoCorrelation) {
//                autoCorrelationcs = acAD.getValues();
//                ad.removeAudioProcessor(acAD);
//            }
//            if (useCQT) {
//                cqtcs = cqtAD.getValues();
//                ad.removeAudioProcessor(cqtAD);
//            }
            mfccAD = null;
            cqtAD = null;
            acAD = null;
            constructSelfSimilarityMatrix();

            mfccs = null;
            autoCorrelationcs = null;
            cqtcs = null;

            System.gc();

        }
        resultMatrix = new float[similarityMatrix.length][];
        for (int i = 0; i < similarityMatrix.length; i++) {
            resultMatrix[i] = new float[similarityMatrix[i].length];
            System.arraycopy(similarityMatrix[i], 0, resultMatrix[i], 0, similarityMatrix[i].length);
        }
        if (this.macroEnabled) {
            segmentation.clearAll();
        } else if (this.mesoEnabled) {
            segmentation.clearMesoAndMicro();
        } else if (this.microEnabled) {
            segmentation.clearMicro();
        }
        noveltyScores = NoveltyScore.calculateScore(similarityMatrix, audioFile.getLengthInMilliSeconds());
        StructureDetection sd = new StructureDetection(audioFile.getLengthInMilliSeconds() / 1000f, resultMatrix, MAX_SCALE_VALUE);
        sd.preProcessing();

        if (macroEnabled || mesoEnabled || microEnabled) {
            sd.run();
        }
        sd = null;
        System.gc();
    }

    public AudioFile getAudioFile() {
        return audioFile;
    }

    public float[][] getSimilarityMatrix() {
        return resultMatrix;
    }

    public float[][] getInitialSimilarityMatrix() {
        return similarityMatrix;
    }

    public int getOverlapping() {
        return overlapping / 1000;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getSamplesPerFrame() {
        return frameSize;
    }

    public void addModelListener(AASModelListener listener) {
        modelListenerList.add(AASModelListener.class, listener);
    }

    // This methods allows classes to unregister for MyEvents
    public void removeModelListener(AASModelListener listener) {
        modelListenerList.remove(AASModelListener.class, listener);
    }

    private void onCalculationStarted() {
        segmentation.clearAll();
        Object[] listeners = modelListenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == AASModelListener.class) {
                ((AASModelListener) listeners[i + 1]).calculationStarted();
            }
        }
    }

    private void onCalculationEnd() {
        onlyStructureDetection = true;
        //System.out.print("END");
        Object[] listeners = modelListenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == AASModelListener.class) {
                ((AASModelListener) listeners[i + 1]).calculationDone();
            }
        }
    }

    private void constructSelfSimilarityMatrix() {
        float maxMFCC = Float.MIN_VALUE;
        float minMFCC = Float.MAX_VALUE;
        float maxAC = Float.MIN_VALUE;
        float minAC = Float.MAX_VALUE;
        float maxCQT = Float.MIN_VALUE;
        float minCQT = Float.MAX_VALUE;

        int size = amountOfFrames;

        float[][] mfcSimilarityMatrix = null;
        float[][] acSimilarityMatrix = null;
        float[][] cqtSimilarityMatrix = null;
        similarityMatrix = null;

        if (useMFCC) {
            mfcSimilarityMatrix = new float[size][];
            for (int i = 0; i < size; i++) {
                mfcSimilarityMatrix[i] = new float[i + 1];
            }
        }
        if (useAutoCorrelation) {
            acSimilarityMatrix = new float[size][];
            for (int i = 0; i < size; i++) {
                acSimilarityMatrix[i] = new float[i + 1];
            }
        }
        if (useCQT) {
            cqtSimilarityMatrix = new float[size][];
            for (int i = 0; i < size; i++) {
                cqtSimilarityMatrix[i] = new float[i + 1];
            }
        }

        similarityMatrix = new float[size][];
        for (int i = 0; i < size; i++) {
            similarityMatrix[i] = new float[i + 1];
        }

        //Average bij meerdere COEF bepalen 
        //Min en Max voor de range bepalen
        //@TODO
        //3 dubbele for kan korter hier of volgende keer?
        for (int i = 0; i < size; i++) {
            for (int j = 0; j <= i; j++) {
                if (useMFCC) {
                    float average = 0;
                    for (int k = 1; k < mfccCoef; k++) { //@TODO: beginnen van 0, 1 of 2 ?
                        //euclidean distance
                        average += (mfccs[i][k] - mfccs[j][k]) * (mfccs[i][k] - mfccs[j][k]);
                        //average += Math.abs(mfccs[i][k] - mfccs[j][k]);// * (mfccs[i][k] - mfccs[j][k]);
                    }
                    average = (float) Math.sqrt(average);
                    mfcSimilarityMatrix[i][j] = average;
                    if (average > maxMFCC) {
                        maxMFCC = average;
                    }
                    if (average < minMFCC) {
                        minMFCC = average;
                    }
                }
                if (useCQT) {
                    float average = 0;
                    for (int b = 1; b < cqtcs[i].length; b++) {
                        //euclidean distance
                        average += (cqtcs[i][b] - cqtcs[j][b]) * (cqtcs[i][b] - cqtcs[j][b]);
                    }
                    average = (float) Math.sqrt(average);
                    cqtSimilarityMatrix[i][j] = average;
                    if (average > maxCQT) {
                        maxCQT = average;
                    }
                    if (average < minCQT) {
                        minCQT = average;
                    }

                }
                if (useAutoCorrelation) {
                    float temp = (float)Math.sqrt(Math.abs(autoCorrelationcs[i] - autoCorrelationcs[j]));
                    //float temp = Math.abs(autoCorrelationcs[i] - autoCorrelationcs[j]);
                    acSimilarityMatrix[i][j] = temp;
                    if (temp > maxAC) {
                        maxAC = temp;
                    }
                    if (minAC > temp) {
                        minAC = temp;
                    }
                }
            }
        }

        //De verhouding van de coeficienten bepalen
        float factor = 0;
        if (useMFCC) {
            factor++;
        }
        if (useAutoCorrelation) {
            factor++;
        }
        if (useCQT) {
            factor++;
        }

        float coeficient = (float) MAX_SCALE_VALUE / factor;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j <= i; j++) {
                similarityMatrix[i][j] = MAX_SCALE_VALUE;
                if (useMFCC) {
                    similarityMatrix[i][j] -= (float) (((mfcSimilarityMatrix[i][j] - minMFCC) / (maxMFCC - minMFCC)) * coeficient);
                }
                if (useAutoCorrelation) {
                    similarityMatrix[i][j] -= (float) (((acSimilarityMatrix[i][j] - minAC) / (maxAC - minAC)) * coeficient);
                }
                if (useCQT) {
                    similarityMatrix[i][j] -= (float) (((cqtSimilarityMatrix[i][j] - minCQT) / (maxCQT - minCQT)) * coeficient);
                }

            }
        }
    }

    public float[][] getNoveltyScore() {
        return noveltyScores;
    }

    public void setNewAudioFile(final File newFile) {
        if (AASModel.getInstance().isGuiEnabled()) {
            //AnnotationPublisher.getInstance().clearTree();
            TranscodingTask transcodingTask = new TranscodingTask(newFile);
            final List<BackgroundTask> detectorTasks = new ArrayList();
            detectorTasks.add(transcodingTask);
            transcodingTask.addHandler(new BackgroundTask.TaskHandler() {

                @Override
                public void taskInterrupted(BackgroundTask backgroundTask, Exception e) {
                }

                @Override
                public void taskDone(BackgroundTask backgroundTask) {
                    if (backgroundTask instanceof TranscodingTask) {
                        setAudioFile(((TranscodingTask) backgroundTask).getAudioFile());
                    }
                }
            });
            String title = "Progress: " + FileUtils.basename(newFile.getAbsolutePath());

            //AnnotationPublisher.getInstance().clear();
            //AnnotationPublisher.getInstance().extractionStarted();
            final ProgressDialog dialog = new ProgressDialog(title, transcodingTask, detectorTasks);
            dialog.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("allTasksFinished")) {
                        onAudioFileChange();
                        //AnnotationPublisher.getInstance().extractionFinished();
                    }
                }
            });
            dialog.pack();
            dialog.setVisible(true);
        } else {
            try {
                this.audioFile = new AudioFile(newFile.getAbsolutePath());
                onAudioFileChange();
            } catch (EncoderException e) {
                e.printStackTrace();
            }
        }
    }

    public Segmentation getSegmentation() {
        return segmentation;
    }

    private class SegmentationTask extends BackgroundTask {

        protected SegmentationTask() {
            super("Analysing segmentation", false);
        }

        @Override
        public Void doInBackground() {
            Runnable runSegmentationAnalyser = new Runnable() {

                @Override
                public void run() {
                    try {
                        AASModel.getInstance().calculate();
                    } catch (Exception e) {
                        interrupt(SegmentationTask.this, e);
                        e.printStackTrace();
                    }
                }
            };
            Thread t = new Thread(runSegmentationAnalyser, getName());
            t.start();
            setProgress(50);
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            setProgress(100);
            return null;

        }
    }

    private class TranscodingTask extends BackgroundTask {

        private final File newFile;
        AudioFile transcodedAudioFile;

        protected TranscodingTask(final File file) {
            super("Transcoding " + FileUtils.basename(file.getAbsolutePath()), false);
            newFile = file;
        }

        @Override
        public Void doInBackground() {
            Runnable runTranscoder = new Runnable() {

                @Override
                public void run() {
                    try {
                        transcodedAudioFile = new AudioFile(newFile.getAbsolutePath());
                    } catch (EncoderException e) {
                        interrupt(TranscodingTask.this, e);
                    }
                }
            };
            //Do the actual detection in the background
            Thread t = new Thread(runTranscoder, getName());
            t.start();
            setProgress(50);
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            setProgress(100);
            return null;
        }

        public AudioFile getAudioFile() {
            return transcodedAudioFile;
        }
    }

    private void setAudioFile(final AudioFile newAudioFile) {
        this.audioFile = newAudioFile;
    }

    public boolean isMacroEnabled() {
        return macroEnabled;
    }

    public boolean isMesoEnabled() {
        return mesoEnabled;
    }

    public boolean isMicroEnabled() {
        return microEnabled;
    }


    public void cleanMemory() {
        this.autoCorrelationcs = null;
        this.cqtcs = null;
        this.mfccs = null;
        this.noveltyScores = null;
        this.similarityMatrix = null;

        System.gc();
    }

    public void run() {
        onCalculationStarted();
        if (guiEnabled) {
            SegmentationTask segmentationTask = new SegmentationTask();
            final List<BackgroundTask> detectorTasks = new ArrayList();
            detectorTasks.add(segmentationTask);
            String title = "Progress: " + this.getAudioFile().originalBasename();
            final ProgressDialog dialog = new ProgressDialog(title, segmentationTask, detectorTasks);
            dialog.pack();
            dialog.setVisible(true);
        } else {
            this.calculate();
        }
        onCalculationEnd();
    }

    private void onAudioFileChange() {
        //audioFileChanged();
        this.onlyStructureDetection = false;
        this.similarityMatrix = null;
        amountOfFrames = audioFile.fileFormat().getFrameLength() / (frameSize - overlapping);
        samplesPerPixel = (int) Math.pow(2, (int) Math.floor(Math.log(audioFile.fileFormat().getFrameLength() / 800) / Math.log(2)));
        sampleRate = audioFile.fileFormat().getFormat().getSampleRate();
        segmentation.clearAll();
        //resetSegmentationPoints();
//        this.macroSegmentationPoints.clear();
//        this.mesoSegmentationPoints.clear();
//        this.microSegmentationPoints.clear();

        File file = new File(audioFile.transcodedPath());
        if (guiEnabled) {
            Player.getInstance().load(file);
        }

        Object[] listeners = audioFileListenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == AudioFileListener.class) {
                ((AudioFileListener) listeners[i + 1]).audioFileChanged();
            }
        }
    }

    public synchronized void addAudioFileChangedListener(AudioFileListener listener) {
        audioFileListenerList.add(AudioFileListener.class, listener);
    }

    public synchronized void removeAudioFileChangedListener(AudioFileListener listener) {
        audioFileListenerList.remove(AudioFileListener.class, listener);
    }

    public boolean isGuiEnabled() {
        return guiEnabled;
    }
}
