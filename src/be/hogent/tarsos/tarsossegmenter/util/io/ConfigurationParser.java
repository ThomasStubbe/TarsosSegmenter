/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.util.io;

import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;
import java.io.*;

/**
 *
 * @author stubb_000
 */
public class ConfigurationParser {

    public static void loadConfigurationFile(File file) throws FileNotFoundException, IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String strLine = in.readLine();
            while (strLine != null && strLine.trim().contains("=")) {
                strLine = in.readLine();

                while (strLine != null && !strLine.trim().contains("=")) {
                    strLine = in.readLine();
                }
                if (strLine != null) {
                    strLine.replaceAll("\r", "");
                    strLine.replaceAll("\n", "");
                    String[] keyValue = strLine.split("=");
                    Configuration.set(ConfKey.valueOf(keyValue[0]), keyValue[1]);
                }
            }
        }
    }

    public static void saveConfigurationFile(File file) throws IOException {
        FileWriter fstream;
        BufferedWriter out;
        file.delete();
        file.createNewFile();
        fstream = new FileWriter(file);
        out = new BufferedWriter(fstream);
        for (ConfKey key : ConfKey.values()) {
            out.write(key.name() + "=" + Configuration.get(key) + "\r\n");
        }
        out.close();
    }
}
