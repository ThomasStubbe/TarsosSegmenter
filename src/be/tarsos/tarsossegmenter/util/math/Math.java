/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.tarsos.tarsossegmenter.util.math;

import java.math.BigDecimal;

/**
 *
 * @author Stubbe
 */
public class Math {

    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    /**
     * Cepstral coefficients are calculated from the output of the Non-linear
     * Transformation method<br>
     * calls: none<br>
     * called by: featureExtraction
     *
     * @param f Output of the Non-linear Transformation method
     * @return Cepstral Coefficients
     */
    
    public static float[] dct(float f[], int amountOfCoef) {
        float coef[] = new float[amountOfCoef];

        for (int i = 0; i < coef.length; i++) {
            for (int j = 0; j < f.length; j++) {
                coef[i] += f[j] * java.lang.Math.cos(java.lang.Math.PI * i / f.length * (j + 0.5));
            }
        }

        return coef;
    }
}
