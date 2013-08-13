/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.util.io;

import java.io.File;
import java.io.FilenameFilter;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author stubb_000
 */
public class SongFileFilter extends FileFilter implements FilenameFilter {

    public final static String MP3 = "mp3";
    public final static String WAV = "wav";
    public final static String FLAC = "flac";

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String extension = getExtension(f);
        if (extension != null && (extension.equals(MP3) || extension.equals(WAV) || extension.equals(FLAC))) {
            return true;
        }
        return false;
    }

    public String getExtension(File f) {
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
        return "Sound files";//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean accept(File dir, String name) {
        if (name.endsWith("." + MP3) || name.endsWith("." + WAV) || name.endsWith("." + FLAC)){
            return true;
        } else {
            return false;
        }
    }
}
