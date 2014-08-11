/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.tarsos.tarsossegmenter.util.io;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author stubb_000
 */
public class SegmentationFileFilter extends FileFilter {

    public final static String TEXTGRID = "textgrid";
    public final static String CSV = "csv";
    
    private static TextGridFileFilter textGridFileFilter;
    private static CSVFileFilter csvFileFilter;

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String extension = getExtension(f);
        if (extension != null && (extension.equals(TEXTGRID) || extension.equals(CSV))) {
            return true;
        }
        return false;
    }

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    @Override
    public String getDescription() {
        return "TextGrid/csv files";
    }

    private static class TextGridFileFilter extends SegmentationFileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = getExtension(f);
            if (extension != null && extension.equals(TEXTGRID)) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "*.TextGrid";
        }
    }
    
    private static class CSVFileFilter extends SegmentationFileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = getExtension(f);
            if (extension != null && extension.equals(CSV)) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "*.csv";
        }
    }

    public static FileFilter getTextGridFileFilter() {
        if (textGridFileFilter == null){
            textGridFileFilter = new TextGridFileFilter();
        }
        return textGridFileFilter;
    }

    public static FileFilter getCSVFileFilter() {
        if (csvFileFilter == null){
            csvFileFilter = new CSVFileFilter();
        }
        return csvFileFilter;
    }
}
