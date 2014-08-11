/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.tarsos.tarsossegmenter.model.preprocessing;

import be.tarsos.tarsossegmenter.model.AASModel;
import be.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.tarsos.tarsossegmenter.util.configuration.Configuration;

/**
 *
 * @author Thomas
 */
public class PreProcesses {

    private final static float PREEMPHASISALPHA = 0.95f;
    private static float[] hammingCoef;

    public static float[][] framing(float[] in, int samplesPerFrame, int overlap) {

        float[][] frames;

        float temp = (float) (in.length - overlap) / (samplesPerFrame - overlap);
        int numFrames = (int) temp;

        // unconditionally round up
        if ((temp / numFrames) != 1) {
            numFrames = numFrames + 1;
        }

        // mfccs = new float[numFrames][amountOfCoef];

        // use zero padding to fill up frames with not enough samples
        float paddedSignal[] = new float[numFrames * (samplesPerFrame - overlap) + overlap];
        System.arraycopy(in, 0, paddedSignal, 0, in.length);
        frames = new float[numFrames][samplesPerFrame];

        // break down speech signal into frames with specified shift interval to create overlap

        for (int n = 0; n < samplesPerFrame; n++) {
            frames[0][n] = paddedSignal[samplesPerFrame + n];
        }

        for (int i = 1; i < numFrames; i++) {
            for (int n = 0; n < samplesPerFrame; n++) {
                frames[i][n] = paddedSignal[i * (samplesPerFrame - overlap) + n];
            }
        }
        return frames;
    }

    public static float[][] normalization(float[][] in) {

        float totalAvg = 0;
        float avg[] = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            avg[i] = 0;
            for (int j = 0; j < in[i].length; j++) {
                avg[i] += Math.abs(in[i][j] / in[i].length);
            }
            totalAvg += Math.abs((avg[i] / in.length));
        }
        float[][] normalizedSamples = new float[in.length][];
        for (int i = 0; i < normalizedSamples.length; i++) {
            normalizedSamples[i] = new float[in[i].length];
            if (avg[i] > totalAvg) {
                for (int j = 0; j < normalizedSamples[i].length; j++) {
                    normalizedSamples[i][j] = in[i][j] / avg[i] * totalAvg;
                }
            } else {
                System.arraycopy(in[i], 0, normalizedSamples[i], 0, normalizedSamples[i].length);
            }
        }
        return normalizedSamples;
    }

    public static float[][] normalizeImage(float[][] in, float range) {
        //In = (I-Min)*(newMax-newMin)/Max-Min+newMin


        float totalAvg = 0;
        float avg[] = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            avg[i] = 0;
            for (int j = 0; j < in[i].length; j++) {
                avg[i] += Math.abs(in[i][j] / in[i].length);
            }
            totalAvg += Math.abs((avg[i] / in.length));
        }
        float[][] normalizedImage = new float[in.length][];
        for (int i = 0; i < normalizedImage.length; i++) {
            normalizedImage[i] = new float[in[i].length];
            if (avg[i] > totalAvg) {
                for (int j = 0; j < normalizedImage[i].length; j++) {
                    normalizedImage[i][j] = in[i][j] / avg[i] * totalAvg;
                }
            } else {
                System.arraycopy(in[i], 0, normalizedImage[i], 0, normalizedImage[i].length);
            }
        }
        return normalizedImage;
    }

    public static float[] preEmphase(float[] in) {
        float[] EmphasedSamples = new float[in.length];
        for (int i = 1; i < in.length; i++) {
            EmphasedSamples[i] = (float) in[i] - PREEMPHASISALPHA * in[i - 1];
        }
        return EmphasedSamples;
    }

    public static void hammingWindow(float[][] frames, int samplesPerFrame) {
        if (hammingCoef == null || hammingCoef.length != samplesPerFrame) {
            hammingCoef = new float[samplesPerFrame];
            for (int n = 0; n < samplesPerFrame; n++) {
                hammingCoef[n] = (float) (0.54 - 0.46 * Math.cos((2 * Math.PI * n) / (samplesPerFrame)));
            }
        }

        for (int m = 0; m < frames.length; m++) {
            for (int n = 0; n < samplesPerFrame; n++) {
                frames[m][n] *= hammingCoef[n];
            }
        }
    }

    public static float diagonalEdgeDetection(float[][] matrix, float maxValue) {
        //diagonalen versterken:
        //kernel over elke pixel laten lopen, waarbij de pixel wordt vervangen door zichzelf + de waarde van de overeenkomste pixels in de (matrix-MAX_VALUE/2) * de factor in de matrix
        int amountOfFrames = matrix.length;
        int[] kernel = {4, 5, 7, 5, 4};

        float[][] temp = new float[amountOfFrames][];
        for (int i = 0; i < amountOfFrames; i++) {
            temp[i] = new float[i + 1];
        }
        float newMax = 0;
        for (int i = 2; i < amountOfFrames - 2; i++) {
            for (int j = 2; j <= i; j++) {
                if (i != j) {
                    float temp2 = matrix[i][j];
                    for (int k = -2; k < kernel.length - 2; k++) {
                        temp2 += (matrix[i + k][j + k] - maxValue / 2) * kernel[k + 2];
                    }
                    temp[i][j] = Math.max(temp2, 0);
                    //Range updaten 
                    if (temp[i][j] > newMax) {
                        newMax = temp[i][j];
                    }
                }
            }
        }
        maxValue = newMax;

        for (int i = 2; i < amountOfFrames - 2; i++) {
            System.arraycopy(temp[i], 2, matrix[i], 2, temp[i].length - 2);
        }

        for (int i = 0; i < amountOfFrames; i++) {
            matrix[i][i] = maxValue;
        }

        return maxValue;
    }
   
    public static void whiteAreasToDiagonals(float[][] matrix, float range) {
        int amountOfFrames = matrix.length;

        boolean first = Configuration.getBoolean(ConfKey.ignore_first_segment);
        float lengthFrameInSec = AASModel.getInstance().getSamplesPerFrame() / AASModel.getInstance().getSampleRate(); //vb 0.18 voor 4048
        final int MIN_SIZE = (int) Math.round(2 / lengthFrameInSec);
        float initialThreshold = range / 1.5f;
        //één zijde van de matrix overlopen
        for (int i = 0; i < amountOfFrames - MIN_SIZE; i++) {
            int[] xLineLength = detectWhiteLine(matrix, i, i, initialThreshold, false, MIN_SIZE);
            int index = xLineLength.length - 1;
            while (index >= 0 && xLineLength[index] == 0) {
                index--;
            }
            float avg = 0;
            while (index >= 0 && avg < initialThreshold) {
                if (xLineLength[index] != 0) {
                    avg = checkAvgColorSquare(matrix, i, i, xLineLength[index]);
                }
                index--;
            }
            index++;
            if (xLineLength[index] > MIN_SIZE) {
                //avg = checkAvgColorSquare(matrix, i, i, xLineLength[index]);
                if (avg > initialThreshold && !first) {
                    float threshold = (float) (0.8 * avg);
                    //initiëel vierkant zwart maken
                    for (int m = i; m < i + xLineLength[index]; m++) {
                        for (int n = i; n < m; n++) {
                            matrix[m][n] /= 2;
                        }
                    }
                    int x = i + xLineLength[index];
                    while (x < amountOfFrames - xLineLength[index]) {
                        int tempXLineLength[] = detectWhiteLine(matrix, x, i, threshold, false, MIN_SIZE);
                        boolean done = false;
                        int index2 = -1;
                        for (int k = 0; k < tempXLineLength.length; k++) {
                            if (tempXLineLength[k] != 0 && !done) {
                                int tempYLineLength[] = detectWhiteLine(matrix, x+tempXLineLength[k]/2, i, threshold, true, MIN_SIZE);
                                for (int l = 0; l < tempYLineLength.length; l++) {
                                    if (tempYLineLength[l] != 0 && Math.abs(1 - ((float) tempXLineLength[k] / (float) tempYLineLength[l])) < 0.2) {
                                        index2 = k;
                                        done = true;
                                    }
                                }
                            } else if (done) {
                                break;
                            }
                        }
                        if (index2 > -1) {
                            float tempAvg = checkAvgColorSquare(matrix, x, i, xLineLength[index2]);
                            if (Math.abs(1 - ((float) tempXLineLength[index2] / (float) tempXLineLength[index2])) < 0.2 && tempAvg > threshold) {
                                //Gevonden! -> wis kot + behoud diagonaal + reset max en x=x+j + reset shiftarray
                                for (int k = x; k < x + tempXLineLength[index2]; k++) {
                                    for (int l = i; l < i + tempXLineLength[index2]; l++) {
                                        if (k > l && Math.abs((l - i) - (k - x)) > 2) { //alles zwart maken behalve de diagonaal + een speling van 2
                                            matrix[k][l] /= 2;
                                        }
                                    }
                                }
                            }
                            x += tempXLineLength[index2];
                        } else {
                            x += Math.max(tempXLineLength[0], 1);
                        }
                    }
                } else if (avg > initialThreshold && first) {
                    first = false;
                }
                i += xLineLength[index];
            }
        }
    }

//                //Hier wordt gekeken of het wel een viekant is en geen lijn
//                float avg = 0;//matrix[i+1][i];
//                for (int x = i + 1; x < i + lineLength; x++) {
//                    for (int y = i; y < x; y++) {
//                        avg += matrix[x][y] / (lineLength * lineLength / 2);
//                    }
//                }
//                    if (i + 2 * lineLength < matrix.length) {
//                        ShiftArray avgRows = new ShiftArray(lineLength);
//                        //Eerste vierkant rechts van initiële vierkant berekenen
//                        for (int m = i + lineLength; m < i + 2 * lineLength; m++) {
//                            float rowAvg = 0;
//                            for (int n = i; n < i + lineLength; n++) {
//                                rowAvg += matrix[m][n] / lineLength;
//                            }
//                            avgRows.setValue(m - i - lineLength, rowAvg);
//                        }
//
//                        float squareAvg = avgRows.getTotalSum() / lineLength;
//                        float maxAvg = squareAvg;
//
//                        //Naar rechts gaan en gelijkaardige vierkanten zoeken --> reduceren tot diagonalen
//                        for (int x = i + (2 * lineLength) + 1; x < amountOfFrames - lineLength; x++) {
//                            float rowAvg = 0;
//                            for (int m = i; m < i + lineLength; m++) {
//                                rowAvg += matrix[x][m] / lineLength;
//                            }
//                            squareAvg = avgRows.shiftLeftAdd(rowAvg) / lineLength;
//
//                            if (squareAvg >= threshold * 0.9) {
////                                int k = 0;
//                                if (squareAvg > maxAvg) {
////                                    while (squareAvg > maxAvg){
////                                        k++;
////                                        rowAvg = 0;
////                                        for (int m = i + j + k; m < i + j; m++) {
////                                            rowAvg += matrix[x][m] / j;
////                                        }
////                                    }
//                                    maxAvg = squareAvg;
//                                } else {
//                                    //Gevonden! -> wis kot + behoud diagonaal + reset max en x=x+j + reset shiftarray
//                                    for (int k = x - lineLength - 1; k < x - 1; k++) {
//                                        for (int l = i; l < i + lineLength; l++) {
//                                            if (Math.abs((l - i) - (k - x + lineLength)) > 2) {
//                                                //System.out.println(k + "," + l);
//                                                matrix[k][l] = 0;
//                                            }
//                                        }
//                                    }
//                                    maxAvg = 0;
//                                    avgRows.reset();
//                                }
//                            }
//                        }
//                    }
//                } else if (avg > threshold && first) {
//                    first = false;
//                }
//            }
//            i += lineLength;
//        }
//        return 0f;
//    }
    //Hier wordt gekeken of het wel een viekant is en geen lijn
    private static float checkAvgColorSquare(float[][] matrix, int startX, int startY, int length) {
        float avg = 0;//matrix[i+1][i];
        for (int x = startX; x < startX + length; x++) {
            for (int y = startY; y < startY + length; y++) {
                if (x >= y) {
                    avg += matrix[x][y] / (length * length);
                } else {
                    avg += matrix[y][x] / (length * length);
                }
            }
        }
        return avg;
    }

    //Zoekt een witte lijn
    private static int[] detectWhiteLine(float[][] matrix, int startFrameX, int startFrameY, float threshold, boolean horizontal, int minlength) {
        final int MAX_CONTINUOUS_MISTAKES = 2;
        final int MAX_MISTAKES = 3;
        int lineLength = 1;
        int amountOfFrames = matrix.length;
        boolean continueSearch = true;
        int amountOfContinuousMistakes = 0;
        int amountOfMistakes = 0;
        int[] result = new int[MAX_MISTAKES];
        for (int i = 0; i < MAX_MISTAKES; i++) {
            result[i] = 0;
        }
        if (!horizontal) {
            while (continueSearch) {
                while (startFrameX + lineLength < amountOfFrames && matrix[startFrameX + lineLength][startFrameY] > threshold) {
                    lineLength++;
                }
                if (amountOfMistakes < Math.max(lineLength-MAX_MISTAKES,0) && startFrameX + lineLength < amountOfFrames - MAX_MISTAKES) {
                    lineLength++;
                    if (matrix[startFrameX + lineLength][startFrameY] > threshold) {
                        continueSearch = true;
                        amountOfContinuousMistakes = 0;
                    } else if (amountOfContinuousMistakes >= MAX_CONTINUOUS_MISTAKES) {
                        lineLength -= MAX_CONTINUOUS_MISTAKES;
                        if (lineLength >= minlength) {
                            result[amountOfMistakes] = lineLength;
                        } else {
                            result[amountOfMistakes] = 0;
                        }
                        amountOfMistakes++;
                        continueSearch = false;
                    } else { //fout toelaten:
                        if (lineLength - 1 >= minlength) {
                            result[amountOfMistakes] = result[amountOfMistakes] = lineLength - 1;
                        } else {
                            result[amountOfMistakes] = 0;
                        }
                        amountOfMistakes++;
                        amountOfContinuousMistakes++;
                        if (amountOfMistakes >= MAX_MISTAKES) {
                            continueSearch = false;
                        }
                    }
                } else {
                    continueSearch = false;
                    lineLength--;
                    if (lineLength >= minlength) {
                        result[amountOfMistakes] = lineLength;
                    } else {
                        result[amountOfMistakes] = 0;
                    }
                }
            }
        } else {
            amountOfFrames = matrix[startFrameX].length;
            while (continueSearch) {
                while (startFrameY + lineLength < amountOfFrames && matrix[startFrameX][startFrameY + lineLength] > threshold) {
                    lineLength++;
                }
                if (amountOfMistakes < Math.max(lineLength-MAX_MISTAKES,0) && startFrameY + lineLength < amountOfFrames - MAX_MISTAKES) {
                    lineLength++;
                    if (matrix[startFrameX][startFrameY + lineLength] > threshold) {
                        continueSearch = true;
                        amountOfContinuousMistakes = 0;
                    } else if (amountOfContinuousMistakes >= MAX_CONTINUOUS_MISTAKES) {
                        lineLength -= MAX_CONTINUOUS_MISTAKES;
                        if (lineLength >= minlength) {
                            result[amountOfMistakes] = lineLength;
                        } else {
                            result[amountOfMistakes] = 0;
                        }
                        amountOfMistakes++;
                        continueSearch = false;
                    } else { //fout toelaten:
                        if (lineLength - 1 >= minlength) {
                            result[amountOfMistakes] = result[amountOfMistakes] = lineLength - 1;
                        } else {
                            result[amountOfMistakes] = 0;
                        }
                        amountOfMistakes++;
                        amountOfContinuousMistakes++;
                        if (amountOfMistakes >= MAX_MISTAKES) {
                            continueSearch = false;
                        }
                    }
                } else {
                    continueSearch = false;
                    lineLength--;
                    if (lineLength >= minlength) {
                        result[amountOfMistakes] = lineLength;
                    } else {
                        result[amountOfMistakes] = 0;
                    }
                }
            }
        }
        return result;
    }

    protected static class ShiftArray {

        private float[] array;
        private int beginIndex;
        private float totalSum;

        public ShiftArray(int size) {
            array = new float[size];
            for (int i = 0; i < array.length; i++) {
                array[i] = 0;
            }
            beginIndex = 0;
            totalSum = 0;
        }

        public void shiftLeft() {
            totalSum -= array[beginIndex];
            array[beginIndex] = 0;
            beginIndex = (beginIndex + 1) % array.length;
        }

        public void shiftRight() {
            totalSum -= array[beginIndex];
            array[beginIndex] = 0;
            beginIndex = (beginIndex - 1) % array.length;
        }

        public float getValue(int index) {
            return array[(beginIndex + index) % array.length];
        }

        public void setValue(int index, float value) {
            totalSum = totalSum - array[(beginIndex + index) % array.length] + value;
            array[(beginIndex + index) % array.length] = value;
        }

        public float shiftLeftAdd(float value) {
            totalSum = totalSum - array[beginIndex] + value;
            array[beginIndex] = value;
            beginIndex = (beginIndex + 1) % array.length;
            return totalSum;
        }

        public float getSize() {
            return array.length;
        }

        public float getTotalSum() {
            return totalSum;
        }

        private void reset() {
            for (int i = 0; i < array.length; i++) {
                array[i] = 0;
            }
            totalSum = 0;
        }
    }

    public static float sharpen(float[][] matrix) {

        int amountOfFrames = matrix.length;

        //Witte gebieden behouden:
        //int sharpenKernel[][] = {{6, 1, -2, -2, -2}, {1, 10, 1, -2, -2}, {-2, 1, 10, 1, -2}, {-2, -2, 1, 10, 1}, {-2, -2, -2, 1, 6}};
        //Witte gebieden wegsmijten:
        int sharpenKernel[][] = {{6, 1, -4, -4, -4}, {1, 10, 1, -4, -4}, {-4, 1, 10, 1, -4}, {-4, -4, 1, 10, 1}, {-4, -4, -4, 1, 6}};

        int half = sharpenKernel.length / 2;

        float[][] temp = new float[amountOfFrames][];
        for (int i = 0; i < amountOfFrames; i++) {
            temp[i] = new float[i + 1];
        }


        float newMax = 0;
        for (int i = half; i < amountOfFrames - half; i++) {
            for (int j = half; j < i; j++) {
                float temp2 = 0;
                for (int k = -half; k <= half; k++) {
                    for (int l = -half; l <= half; l++) {
                        if (i + k < j + l) {
                            temp2 += matrix[j + l][i + k] * sharpenKernel[half + k][half + l];
                        } else {
                            temp2 += matrix[i + k][j + l] * sharpenKernel[half + k][half + l];
                        }
                    }
                }
                temp[i][j] = Math.max(temp2, 0);
                if (temp[i][j] > newMax) {
                    newMax = temp[i][j];
                }
            }
        }

        for (int i = half; i < amountOfFrames - half; i++) {
            System.arraycopy(temp[i], half, matrix[i], half, temp[i].length - half);
        }

        //Diagonaal herstellen
        for (int i = 0; i < amountOfFrames; i++) {
            matrix[i][i] = newMax;
        }

        return newMax;
    }

    public static float makeBinary(float[][] matrix, float treshhold, float maxValue) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j <= i; j++) {
                if (matrix[i][j] > treshhold) {
                    matrix[i][j] = 1;
                } else {
                    matrix[i][j] = 0;
                }
            }
        }
        return 1;
    }

    public static void dilate(float[][] image, float maxValue) {
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[i].length; j++) {
                if (image[i][j] == maxValue) {
                    if (i > 0 && j <= i - 1 && image[i - 1][j] == 0) {
                        image[i - 1][j] = maxValue * 2;
                    }
                    if (j > 0 && image[i][j - 1] == 0) {
                        image[i][j - 1] = maxValue * 2;
                    }
                    if (i + 1 < image.length && image[i + 1][j] == 0) {
                        image[i + 1][j] = maxValue * 2;
                    }
                    if (j + 1 < image[i].length && image[i][j + 1] == 0) {
                        image[i][j + 1] = maxValue * 2;
                    }
                }
            }
        }
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[i].length; j++) {
                if (image[i][j] == maxValue * 2) {
                    image[i][j] = maxValue;
                }
            }
        }
    }

    public static float[][] applySobelKernel(float[][] matrix) {
        int amountOfFrames = matrix.length;
        int[][] sobelKernelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int[][] sobelKernelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        float Gx;
        float Gy;

        float[][] temp = new float[amountOfFrames][];

        for (int i = 0; i < amountOfFrames; i++) {
            temp[i] = new float[i + 1];
        }

        for (int i = 1; i < amountOfFrames - 1; i++) {
            for (int j = 1; j < i - 1; j++) {
                Gx = matrix[i - 1][j - 1] * sobelKernelX[0][0]
                        + matrix[i][j - 1] * sobelKernelX[1][0]
                        + matrix[i + 1][j - 1] * sobelKernelX[2][0]
                        + matrix[i - 1][j] * sobelKernelX[0][1]
                        + matrix[i][j] * sobelKernelX[1][1]
                        + matrix[i + 1][j] * sobelKernelX[2][1]
                        + matrix[i - 1][j + 1] * sobelKernelX[0][2]
                        + matrix[i][j + 1] * sobelKernelX[1][2]
                        + matrix[i + 1][j + 1] * sobelKernelX[2][2];
                Gy = matrix[i - 1][j - 1] * sobelKernelY[0][0]
                        + matrix[i][j - 1] * sobelKernelY[1][0]
                        + matrix[i + 1][j - 1] * sobelKernelY[2][0]
                        + matrix[i - 1][j] * sobelKernelY[0][1]
                        + matrix[i][j] * sobelKernelY[1][1]
                        + matrix[i + 1][j] * sobelKernelY[2][1]
                        + matrix[i - 1][j + 1] * sobelKernelY[0][2]
                        + matrix[i][j + 1] * sobelKernelY[1][2]
                        + matrix[i + 1][j + 1] * sobelKernelY[2][2];
                temp[i][j] = (float) Math.sqrt((Gx * Gx + Gy * Gy));
            }
        }
        return temp;
    }
}
