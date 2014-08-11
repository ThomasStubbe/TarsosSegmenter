/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.tarsos.tarsossegmenter.util.configuration;

import java.util.ArrayList;

/**
 *
 * @author Thomas
 */
public enum ConfKey {

    /**
     *
     */
    data_directory(true),
    audio_file_name_pattern,
    dir_history,
    /**
     * Transcode the audio or just copy it. <br> The default is
     * <code>true</code>.
     */
    transcode_audio,
    /**
     * The format to transcode the audio to. It is one of the enum values
     * defined in DefaultAttributes.
     */
    transcoded_audio_to,
    /**
     * Checks if the transcoded audio is in the configured format, this can be
     * skipped for performance reasons.
     */
    transcode_check_format,
    enable_mfcc(ConfKey.MFCC, ConfKey.BOOL),
    //enable_mfcc,
    enable_cqt(ConfKey.CQT, ConfKey.BOOL),
    enable_autocorrelation(ConfKey.AC, ConfKey.BOOL),
    enable_macro(ConfKey.GENERAL, ConfKey.BOOL),
    enable_meso(ConfKey.GENERAL, ConfKey.BOOL),
    enable_micro(ConfKey.GENERAL, ConfKey.BOOL),
    framesize(ConfKey.GENERAL, ConfKey.INT),
    overlapping(ConfKey.GENERAL, ConfKey.INT),
    lowfilterfreq(ConfKey.GENERAL, ConfKey.FLOAT),
    upperfilterfreq(ConfKey.GENERAL, ConfKey.FLOAT),
    mfcc_coef(ConfKey.MFCC, ConfKey.INT),
    mfcc_melfilters(ConfKey.MFCC, ConfKey.INT),
    cqt_bins(ConfKey.CQT, ConfKey.INT),
    enable_line_detection(ConfKey.GENERAL, ConfKey.BOOL),
    enable_binary(ConfKey.GENERAL, ConfKey.BOOL),
    binary_treshold(ConfKey.GENERAL, ConfKey.FLOAT),
    ignore_first_segment(ConfKey.GENERAL, ConfKey.BOOL),
    ignore_last_segment(ConfKey.GENERAL, ConfKey.BOOL),
    enable_white_area_reducement(ConfKey.GENERAL, ConfKey.BOOL);
    public boolean isRequiredDir;
    public static final String GENERAL = "General";
    public static final String MFCC = "MFCC";
    public static final String CQT = "CQT";
    public static final String AC = "Autocorrelation";
    public static final String BOOL = "bool";
    public static final String INT = "int";
    public static final String FLOAT = "float";
    public static final int[] FRAMESIZES = {1024, 2048, 4096, 8192, 16384};
    /**
     * True if the configured key is a required directory. False otherwise.
     */
    //private final boolean isRequiredDir;
    /**
     * Create a configuration key.
     *
     * @param isReqDir True if the configured key is a required directory. False
     * otherwise.
     */
    private String category;
    private String type;

    ConfKey(String category, String type) {
        this.category = category;
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public static ConfKey[] getValues(String category) {
        ArrayList<ConfKey> keys = new ArrayList();
        for (ConfKey key : ConfKey.values()) {
            if (key.getCategory() != null) {
                if (key.getCategory().equals(category)) {
                    keys.add(key);
                }
            }
        }
        //String[] a = 
        return keys.toArray(new ConfKey[keys.size()]);
    }

    public static String[] getCategories() {
        return new String[]{ConfKey.GENERAL, ConfKey.MFCC, ConfKey.CQT, ConfKey.AC};
    }

    ConfKey(final boolean isReqDir) {
        this.isRequiredDir = isReqDir;
    }

    /**
     * By default configured values are not required directories.
     */
    ConfKey() {
        this(false);
    }

    /**
     * Checks if this key is a required directory.
     *
     * @return True if the configured value is a required directory, false
     * otherwise.
     */
    public boolean isRequiredDirectory() {
        return isRequiredDir;
    }
    //vb 2000 : 2048 1024

    public static int getClosestFrameSize(int frameSize) {
        int i = 0;
        while (i < FRAMESIZES.length && frameSize > FRAMESIZES[i]) {
            i++;
        }
        if (i < FRAMESIZES.length && frameSize == FRAMESIZES[i]) {
            return frameSize;
        } else {
            if (i == 0) {
                return FRAMESIZES[i];
            } else if (i == FRAMESIZES.length) {
                return FRAMESIZES[i - 1];
            } else {
                int half = (FRAMESIZES[i - 1] + FRAMESIZES[i]) / 2;
                if (frameSize > half) {
                    return FRAMESIZES[i];
                } else {
                    return FRAMESIZES[i - 1];
                }
            }
        }

    }

    public static int getFrameSizeIndex(int value) {
        for (int i = 0; i < FRAMESIZES.length; i++) {
            if (FRAMESIZES[i] == value) {
                return i;
            }
        }
        return 0;
    }
}