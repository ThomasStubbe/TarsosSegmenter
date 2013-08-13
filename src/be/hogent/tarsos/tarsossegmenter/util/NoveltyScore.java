/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.util;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 *
 * @author Thomas
 */
public class NoveltyScore {

    /*
     * | 1 2 -2 -1 | Kernel = | 2 3 -3 -2 | |-2 -3 3 2 | |-1 -2 2 1 |
     *
     */
    /*
     * Laat een kernel over de diagonaal van de similarity matrix lopen. @TODO:
     * * switch matrix | 1 1 -1-1 | | 1 1 0 0 | | 0 0 1 1 | Kernel = | 1 1 -1-1
     * | = | 1 1 0 0 | - | 0 0 1 1 | |-1-1 1 1 | | 0 0 1 1 | | 1 1 0 0 | |-1-1 1
     * 1 | | 0 0 1 1 | | 1 1 0 0 |
     *
     * Elk element wordt vermenigvuldigd met het overeenkomstig element in de
     * self similarity matrix en opgeteld De totale som zal dus groot zijn als
     * de self similarity van beide blokken die overeenkomen met de 1'tjes goed
     * is en als de gelijkenis tussen het eerste en het 2de stuk op de diagonaal
     * laag is (dat is de vergelijking met de blokmatrix rechts of onder het
     * eerste blok
     *
     */
    //private static final int STARTENDWINDOW = 20;
    private static int maxWindowSize;// = 200;
    //private static int windowSizeStep = 50;
    private static int window;
    private static float treshholdMacro = 0.23f;
    private static float treshholdMeso = 0.2f; //adjusts dynamicly
    private static float treshholdMicro = 0.1f;
    private static float max;
    //private static float songDuration;
    private static int amountOfFrames;

    //Zonder de segmentatiepunten te berekenen (enkel de noveltyScore zelf)
    private static float[][] createGaussianKernel(int size) {

        float[][] gaussianKernel = new float[size][size];
//        float sigma = (size / 4f) - 2.5f;
        //float sigma = 1;
        float sigma = (float)Math.sqrt((2*size*size-2)/12);
        float mean = size / 2;
        double sum = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < gaussianKernel[i].length; j++) {

                gaussianKernel[i][j] = (float) (Math.exp(-0.5 * (Math.pow((i - mean) / sigma, 2) + Math.pow((j - mean) / sigma, 2))) / (2 * Math.PI * sigma * sigma));
                if (i >= mean && j < mean || i < mean && j >= mean) {
                    gaussianKernel[i][j] *= -1;
                }
                sum += gaussianKernel[i][j];
            }
        }
//        System.out.println("   KernelSize: " + size + " - Sum: " + sum);
        return gaussianKernel;
    }

    private static float[] calculateScore(float[][] matrix, int segmentationLevel) {
        amountOfFrames = matrix.length;
//        System.out.println("SegmentationLevel: " + segmentationLevel);
        int framesize = Configuration.getInt(ConfKey.framesize);

        switch (framesize) {
            case 1024:
                maxWindowSize = 200;
                break;
            case 2048:
                maxWindowSize = 120;
                break;
            case 4096:
                maxWindowSize = 66;
                break;
            case 8192:
                maxWindowSize = 34;
                break;
            case 16384:
                maxWindowSize = 18;
                break;
            default:
                maxWindowSize = 100;
                break;
        }

        switch (segmentationLevel) {
            case AASModel.MESO_LEVEL:
                maxWindowSize /= 2;
                break;
            case AASModel.MICRO_LEVEL:
                maxWindowSize /= 3;
                break;
        }
        if (maxWindowSize % 2 == 1) {
            maxWindowSize++;
        }

        float[][] kernel = null;

        float[] scores = new float[amountOfFrames];

        float avgScore;
        int beginEndFrame = Math.min(10, maxWindowSize/2 + 1);
        
        for (int frameNr = beginEndFrame; frameNr < amountOfFrames - beginEndFrame; frameNr++) {
            window = (frameNr * 2);
            if (window > maxWindowSize) {
                window = maxWindowSize;
            }
            if (frameNr > amountOfFrames - maxWindowSize / 2) {
                window = (amountOfFrames - frameNr) * 2;
            }

            if (kernel == null || window != kernel.length) {
                kernel = createGaussianKernel(window);
            }

            avgScore = calculateScore(matrix, frameNr, kernel);

            scores[frameNr] = Math.max(avgScore, 0);
        }

        //applying gausian: gaussian met sigma ~ 1.62
        //float[] gaussianKernel = {(float) (252. / 1024.), (float) (210. / 1024.), (float) (120. / 1024.), (float) (45. / 1024.), (float) (10. / 1024.), (float) (1. / 1024.)}; //semetrisch
        //gaussian met sigma = 2.2 (1/[s*WORTEL(2*PI)]*e^[-x²/(2*s²)]

        //6,10352E-05	0,000854492	0,005554199	0,022216797	0,061096191	0,122192383	0,183288574	0,209472656
        //float[] gaussianKernel = {0.000244141f, 0.002929688f, 0.016113281f, 0.053710938f, 0.120849609f, 0.193359375f, 0.225585938f}; //semetrisch
        //Moet oneven zijn!
        float[] gaussianKernel = {.0000610352f, 0.000854492f, 0.005554199f, 0.022216797f, 0.061096191f, 0.122192383f, 0.183288574f, 0.209472656f}; //semetrisch met uitzondering van de laatste waarde
        float[] smoothedscores = new float[amountOfFrames];


        max = 0;
        for (int i = 0; i < gaussianKernel.length; i++) {
            smoothedscores[i] = scores[i];
            smoothedscores[amountOfFrames - i - 1] = scores[amountOfFrames - i - 1];
        }
        for (int i = gaussianKernel.length - 1; i <= amountOfFrames - gaussianKernel.length; i++) {
            smoothedscores[i] = 0; //+ gaussianKernel[0] * scores[i];
            for (int j = 0; j < gaussianKernel.length - 1; j++) { //lengte 8 voor 
                smoothedscores[i] += gaussianKernel[j] * scores[i - gaussianKernel.length + 1 + j] + gaussianKernel[j] * scores[i + gaussianKernel.length - 1 - j];
            }
            smoothedscores[i] += gaussianKernel[gaussianKernel.length - 1] * scores[i];
            if (max < smoothedscores[i]) {
                max = smoothedscores[i];
            }
        }
        return smoothedscores;
    }

    private static float calculateScore(float[][] matrix, int frameNr, float[][] kernel) {
        float score = 0f;
        int mean = kernel.length / 2;

        for (int i = -mean; i < mean; i++) { //lengte 8 voor 
            for (int j = -mean; j < mean; j++) {
                if (frameNr + j <= frameNr + i) {
                    score += matrix[frameNr + i][frameNr + j] * kernel[mean + i][mean + j];
                } else {
                    score += matrix[frameNr + j][frameNr + i] * kernel[mean + i][mean + j];
                }

//                
//                if (frameNr - kernel.length / 2 + j > frameNr - kernel.length / 2 + i || frameNr - kernel.length / 2 + i < 0 || frameNr - kernel.length / 2 + j < 0 || frameNr - kernel.length / 2 + i >= matrix.length || frameNr - (kernel.length / 2) + j > matrix[frameNr - kernel.length / 2 + i].length) {
//                    System.out.println("   i: " + (frameNr - kernel.length / 2 + i) + "j: " + (frameNr - kernel.length / 2 + j));
//                } else {
//                    score += kernel[i][j] * matrix[frameNr - (kernel.length / 2) + i][frameNr - (kernel.length / 2) + j];
//                }
            }
        }

        //return (float)(Math.log(score)/Math.log(1.01));
        return score;
    }

    private static float calculateScore(float[][] matrix, int frameNr, int windowSize) {
        float score = 0f;

        for (int m = -windowSize / 2; m < windowSize / 2; m++) {
            for (int n = (-windowSize + 1) / 2; n < m; n++) {
                float temp = (matrix[frameNr + m][frameNr + n]);
                if (m >= 0 && n < 0) {
                    temp *= -1;
                }
                score += temp;
            }
        }
        return score;
    }

    public static float[][] calculateScore(float[][] matrix, float songDuration) {
        float[][] noveltyScore = new float[3][];

        if (Configuration.getBoolean(ConfKey.enable_macro)) {
            AASModel.getInstance().getSegmentation().addSegmentationPoint(0f, AASModel.MACRO_LEVEL);
            AASModel.getInstance().getSegmentation().addSegmentationPoint((float) AASModel.getInstance().getAudioFile().getLengthIn(TimeUnit.SECONDS), AASModel.MACRO_LEVEL);
            noveltyScore[AASModel.MACRO_LEVEL] = calculateScore(matrix, AASModel.MACRO_LEVEL);
            calculatePossibleSegmentationPoints(noveltyScore[AASModel.MACRO_LEVEL], AASModel.MACRO_LEVEL, songDuration, treshholdMacro);
            if (Configuration.getBoolean(ConfKey.enable_meso)) {
                //Stap 1: macropunten toevoegen op meso-niveau
                noveltyScore[AASModel.MESO_LEVEL] = calculateScore(matrix, AASModel.MESO_LEVEL);
                calculatePossibleSegmentationPoints(noveltyScore[AASModel.MESO_LEVEL], AASModel.MESO_LEVEL, songDuration, treshholdMeso);
                if (Configuration.getBoolean(ConfKey.enable_micro)) {
                    noveltyScore[AASModel.MICRO_LEVEL] = calculateScore(matrix, AASModel.MICRO_LEVEL);
                    calculatePossibleSegmentationPoints(noveltyScore[AASModel.MICRO_LEVEL], AASModel.MICRO_LEVEL, songDuration, treshholdMicro);
                }
            }
        }
        return noveltyScore;
    }

    /*
     * public static float[] calculateScore(float[][] matrix, float
     * songDuration, boolean macroEnabled, boolean mesoEnabled, boolean
     * microEnabled) { //@TODO: verschillende niveau's float[] scores = new
     * float[amountOfFrames];
     *
     * int amountOfSteps = (MAX_WINDOW - MIN_WINDOW + 1) / WINDOW_STEP;
     *
     * max = 0;
     *
     * for (int i = 0; i < amountOfFrames; i++) { scores[i] = 0; }
     *
     * for (int l = MIN_WINDOW; l <= MAX_WINDOW; l += WINDOW_STEP) { //int l =
     * 150; //xy is de diagonaal //l is de zijde van de kernel matrix
     *
     * //for (int frameNr=(l+1)/2; frameNr<amountOfFrames-l; frameNr++){ for
     * (int frameNr = (l + 1) / 2; frameNr < amountOfFrames - l; frameNr +=
     * WINDOW_STEP) { //System.out.println(matrix[frameNr][frameNr]); //for (int
     * l=2; l<Math.min(100,frameNr); l+=2){ float score = 0; //loopt niet heel
     * de matrix af!!!! (15-7) is eerste pixel for (int m = -l / 2; m < l / 2;
     * m++) { for (int n = -l / 2; n < l / 2; n++) { float temp = matrix[frameNr
     * + m][frameNr + n]; if (m <= 0 && n <= 0 || m > 0 && n > 0) { temp *= -1;
     * } score += temp; } } score = score / (float) amountOfSteps; score =
     * Math.max(0, score); if (max < score) { max = score; } scores[frameNr] +=
     * score; //} } }
     *
     * //applying gausian: float[] gaussianKernel = {(float) (70. / 1024.),
     * (float) (56. / 1024.), (float) (28. / 1024.), (float) (8. / 1024.),
     * (float) (1. / 1024.)}; //semetrisch
     *
     * float[] smoothedscores = new float[amountOfFrames];
     *
     * for (int i = 0; i < 4; i++) { smoothedscores[i] = scores[i];
     * smoothedscores[amountOfFrames - i - 1] = scores[amountOfFrames - i - 1];
     * } for (int i = 4; i < amountOfFrames - 4; i++) { smoothedscores[i] =
     * gaussianKernel[0] * scores[i - 4] + gaussianKernel[1] * scores[i - 3] +
     * gaussianKernel[2] * scores[i - 2] + gaussianKernel[3] * scores[i - 1] +
     * gaussianKernel[4] * scores[i] + gaussianKernel[3] * scores[i + 1] +
     * gaussianKernel[2] * scores[i + 2] + gaussianKernel[1] * scores[i + 3] +
     * gaussianKernel[0] * scores[i + 4]; }
     *
     * /*
     * for (int i=0; i<3; i++){ smoothedscores[i] = scores[i];
     * smoothedscores[amountOfFrames-i-1] = scores[amountOfFrames-i-1]; } for
     * (int i=3; i<amountOfFrames-3; i++){ smoothedscores[i] =
     * (float)0.006*scores[i-3]+(float)0.061*scores[i-2]+(float)0.242*scores[i-1]+(float)0.383*scores[i]+(float)0.242*scores[i+1]+(float)0.061*scores[i+2]+(float)0.006*scores[i+3];
     * }
     *
     *
     *
     * ArrayList<Float> potentialSegmentationPoints = new ArrayList(); for (int
     * frameNr = 0; frameNr < amountOfFrames; frameNr++) { if
     * (smoothedscores[frameNr] >= 0.7 * max) { float score =
     * smoothedscores[frameNr]; frameNr++; while (frameNr < amountOfFrames &&
     * score <= smoothedscores[frameNr]) { score = smoothedscores[frameNr];
     * frameNr++; } float time = getTimeofFrameInMillisec(frameNr,
     * amountOfFrames, songDuration); potentialSegmentationPoints.add((float)
     * ((double) (time) / (double) (1000))); System.out.println((double) (time)
     * / (double) (1000));
     *
     * while (frameNr < amountOfFrames && score >= smoothedscores[frameNr]) {
     * score = smoothedscores[frameNr]; frameNr++; } } } /* List<Double> values
     * = new ArrayList(); for (int i=0; i <amountOfFrames; i++){
     * values.add((double)scores[i]); } values =
     * PitchFunctions.getGaussianSmoothed(values, 0.3); //doubles, standard
     * diviation for (int i=0; i<amountOfFrames; i++){
     * scores[i]=values.get(i).floatValue(); }
     *
     *
     *
     * return smoothedscores; }
     */
//    private static void calculateSegmentationPoints(float[] scores, int NIVEAU, ArrayList<SegmentationPart> segmentationParts) {
//
//        segmentationParts.clear();
//
//        for (int frameNr = 0; frameNr < amountOfFrames; frameNr++) {
//            if (scores[frameNr] >= 0.7 * max) {
//                float score = scores[frameNr];
//                frameNr++;
//                while (frameNr < amountOfFrames && score <= scores[frameNr]) {
//                    score = scores[frameNr];
//                    frameNr++;
//                }
//                float time = getTimeofFrameInMillisec(frameNr, amountOfFrames, songDuration);
//                //sg.getBegin() = oud punt
//                //sg.einde = nieuw punt
//
//                //(float) ((double) (time) / (double) (1000))
//                segmentationParts.add(sg);
//                System.out.println((double) (time) / (double) (1000));
//
//                while (frameNr < amountOfFrames && score >= scores[frameNr]) {
//                    score = scores[frameNr];
//                    frameNr++;
//                }
//            }
//        }
//    }
    private static float getTimeofFrameInMillisec(int frameNr, int amountOfFrames, float songDuration) {
        double temp = (double) frameNr / (double) amountOfFrames;
        return (float) (temp * songDuration);
    }

    //@TODO: maxheap -> segmentatiepunten met grootste score eerst!
    private static void calculatePossibleSegmentationPoints(float[] scores, int segmentationLevel, float songDuration, float treshhold) {
        //Zoeken naar lokale maxima in novelty score die > treshhold -> deze punten in potentialSegmentationIndices steken
        ArrayList<Integer> potentialSegmentationIndices = new ArrayList();
        for (int frameNr = 0; frameNr < amountOfFrames; frameNr++) {
            if (scores[frameNr] >= treshhold * max) {
                float score = scores[frameNr];
                frameNr++;
                while (frameNr < amountOfFrames && score <= scores[frameNr]) {
                    score = scores[frameNr];
                    frameNr++;
                }

                float time = getTimeofFrameInMillisec(frameNr, amountOfFrames, songDuration) / 1000f;
                if (potentialSegmentationIndices.size() > 0 && time - getTimeofFrameInMillisec(potentialSegmentationIndices.get(potentialSegmentationIndices.size() - 1), amountOfFrames, songDuration) / 1000f < 2f) {
                    if (scores[potentialSegmentationIndices.get(potentialSegmentationIndices.size() - 1)] < scores[frameNr]) {
                        potentialSegmentationIndices.set(potentialSegmentationIndices.size() - 1, frameNr);
                    }
                } else {
                    potentialSegmentationIndices.add(frameNr);
                }

                while (frameNr < amountOfFrames && score >= scores[frameNr]) {
                    score = scores[frameNr];
                    frameNr++;
                }
            }
        }

        //@TODO: performanter zonder tussengegevensstrutuur
        //@TODO: naverwerking (vb, min X seconden tussen + afhankelijk van structuuranalyse en verhoudingen)
        //@TODO: op Meso niveau -> segmentatiepunten dicht bij het macro niveau wegsmijten?
        //potentialSegmentationIndices omzetten naar segmentationParts
        int minPoints = 0;
        int maxPoints = 100;
        switch (segmentationLevel) {
            case (AASModel.MACRO_LEVEL):
                minPoints = 2;
                maxPoints = 120;
                break;
            case (AASModel.MESO_LEVEL):
                minPoints = 60;
                maxPoints = 120;
                break;
            case (AASModel.MICRO_LEVEL):
                minPoints = 70;
                maxPoints = 200;
                break;
        }

        if (potentialSegmentationIndices.size() < minPoints && treshhold > 0.05f) {
            potentialSegmentationIndices.clear();
            treshhold *= 0.7;
            calculatePossibleSegmentationPoints(scores, segmentationLevel, songDuration, treshhold);
        } else if (potentialSegmentationIndices.size() > maxPoints && treshhold < 0.7) {
            potentialSegmentationIndices.clear();
            treshhold *= 1.3;
            calculatePossibleSegmentationPoints(scores, segmentationLevel, songDuration, treshhold);
        } else {
            for (int i = 0; i < potentialSegmentationIndices.size(); i++) {
                AASModel.getInstance().getSegmentation().addSegmentationPoint(getTimeofFrameInMillisec(potentialSegmentationIndices.get(i), amountOfFrames, songDuration) / 1000f, segmentationLevel);
            }

        }
        AASModel.getInstance().getSegmentation().sortSegmentationPoints();
        //return scores;
    }
}
