/**
 *
 * Tarsos is developed by Joren Six at The Royal Academy of Fine Arts & Royal
 * Conservatory, University College Ghent, Hoogpoort 64, 9000 Ghent - Belgium
 * 
*
 */
package be.hogent.tarsos.tarsossegmenter.util.io;

import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

//import be.hogent.tarsos.sampled.pitch.Annotation;
//import be.hogent.tarsos.sampled.pitch.Pitch;
//import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
/**
 * Exports a DatabaseResult to a CSV-file.
 *
 * @author Joren Six
 */
public final class FileUtils {

    /**
     * Parses files that can be exported from <a
     * href="http://miracle.otago.ac.nz/postgrads/tartini/">The Tartini
     * program</a>. The files look like this (with header):
     *
     * <pre>
     *        Time(secs) Pitch(semi-tones)       Volume(rms)
     *                  0                 0              -150
     *           0.301859           72.3137          -108.931
     *           0.325079           72.0692          -79.6062
     *           0.348299           71.9804           -64.634
     *           0.371519           71.9619          -59.5387
     *           0.394739           71.9699          -59.0133
     *           0.417959           71.9717           -59.567
     *           0.441179           71.9854          -59.7326
     *           0.464399            72.011          -58.6959
     * </pre>
     *
     * @param fileName The Tartini pitch file.
     * @return A list of annotations, recovered from the pitch file.
     */
//	public static List<Annotation> parseTartiniPitchFile(final String fileName) {
//		final List<Annotation> samples = new ArrayList<Annotation>();
//		final String contents = FileUtils.readFile(fileName);
//		final String[] lines = contents.split("\n");
//		for (int i = 1; i < lines.length; i++) {
//			final String[] data = lines[i].split(" +");
//			final double time = Double.parseDouble(data[1]);
//			final double midiCents = Double.parseDouble(data[2]);
//			final Pitch pitch = Pitch.getInstance(PitchUnit.MIDI_CENT, midiCents);
//			final Annotation s = new Annotation(time, pitch.getPitch(PitchUnit.HERTZ),
//					PitchDetectionMode.TARSOS_MPM);
//			samples.add(s);
//		}
//		return samples;
//	}
//	public static void writePitchAnnotations(final String fileName, final List<Annotation> samples) {
//		StringBuilder sb = new StringBuilder();
//		sb.append("Start(s),Pitch(Hz),Probability[0-1.0],Source\n");
//		for (Annotation s : samples) {
//			sb.append(s.toString()).append("\n");
//		}
//		writeFile(sb.toString(), fileName);
//	}
//
//	public static List<Annotation> readPitchAnnotations(final String fileName) {
//		final List<Annotation> annotations = new ArrayList<Annotation>();
//		final String contents = FileUtils.readFile(fileName);
//		final String[] lines = contents.split("\n");
//		// Skip the first line, the header.
//		for (int i = 1; i < lines.length; i++) {
//			annotations.add(Annotation.parse(lines[i]));
//		}
//		return annotations;
//	}
    public static String temporaryDirectory() {
        final String tempDir = System.getProperty("java.io.tmpdir");
        return tempDir;
    }

    // Disable the default constructor.
    private FileUtils() {
    }

    /**
     * Joins path elements using the systems path separator. e.g. "/tmp" and
     * "test.wav" combined together should yield /tmp/test.wav on UNIX.
     *
     * @param path The path parts part.
     * @return Each element from path joined by the systems path separator.
     */
    public static String combine(final String... path) {
        File file = new File(path[0]);
        for (int i = 1; i < path.length; i++) {
            file = new File(file, path[i]);
        }
        return file.getPath();
    }

    private static byte[] createChecksum(String filename, final int numberOfBytes)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest complete;
        try (InputStream fis = new FileInputStream(filename)) {
            if (numberOfBytes % 1024 != 0) {
                throw new IllegalArgumentException("Number of bytes should be dividable by 1024.");
            }
            byte[] buffer = new byte[1024];
            complete = MessageDigest.getInstance("MD5");
            int numRead;
            int maxNumberOfBuffers = numberOfBytes / 1024;
            int currentNumberOfBuffers = 0;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
                currentNumberOfBuffers++;
            } while (numRead != -1 && currentNumberOfBuffers < maxNumberOfBuffers);
        }
        return complete.digest();
    }

    /**
     * Calculate an MD5 checksum of <b>the first 256kB</b> of a file. The idea
     * is to get a unique hash for a file, quickly. A hash of the first 256kB is
     * 'unique enough' and much faster than calculating an md5 for a complete
     * file. On Unix this method can be checked using:
     *
     * <pre>
     * head -c 262144 file_to_calculate_md5_for | md5sum
     *
     * <pre/>
     *
     * @param filename
     * 		The absolute file name of a file used to calculate an md5 checksum of <b>the first 256kB</b> off.
     * @return An MD5 checksum of <b>the first 256kB</b> of a file.
     */
    public static String getMD5Checksum(String filename) {
        String result = "";
        try {
            byte[] b;
            b = createChecksum(filename, 262144);
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }
        } catch (NoSuchAlgorithmException e) {
            String message = "MD5 Algorithm not known, runtime outdated?";
            throw new Error(message, e);
        } catch (IOException e) {
            String message = "Exception while computing file md5 " + filename;
        }
        return result;
    }

    /**
     * @return The path where the program is executed.
     */
    public static String runtimeDirectory() {
        String runtimePath = "";
        try {
            runtimePath = new File(".").getCanonicalPath();
        } catch (final IOException e) {
        }
        return runtimePath;
    }

    /**
     * Writes a file to disk. Uses the string contents as content. Failures are
     * logged.
     *
     * @param contents The contents of the file.
     * @param name The name of the file to create.
     */
    /**
     * @param contents
     * @param name
     */
    public static void writeFile(final String contents, final String name) {
        writeFile(contents, name, false);
    }

    private static void writeFile(final String contents, final String name, final boolean append) {
        BufferedWriter outputStream = null;
        PrintWriter output = null;
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(name, append);
            outputStream = new BufferedWriter(fileWriter);
            output = new PrintWriter(outputStream);
            output.print(contents);
            outputStream.flush();
            output.close();
        } catch (final IOException e) {
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (final IOException e) {
            }
        }
    }

    /**
     * Appends a string to a file on disk. Fails silently.
     *
     * @param contents The contents of the file.
     * @param name The name of the file to create.
     */
    public static void appendFile(final String contents, final String name) {
        writeFile(contents, name, true);
    }

    /**
     * Reads the contents of a file.
     *
     * @param name the name of the file to read
     * @return the contents of the file if successful, an empty string
     * otherwise.
     */
    public static String readFile(final String name) {
        FileReader fileReader;
        final StringBuilder contents = new StringBuilder();
        try {
            final File file = new File(name);
            if (!file.exists()) {
                throw new IllegalArgumentException("File " + name + " does not exist");
            }
            fileReader = new FileReader(file);
            try (BufferedReader reader = new BufferedReader(fileReader)) {
                String inputLine = reader.readLine();
                while (inputLine != null) {
                    contents.append(inputLine).append("\n");
                    inputLine = reader.readLine();
                }
            }
        } catch (final IOException i1) {
        }
        return contents.toString();
    }

    /**
     * Reads the contents of a file in a jar.
     *
     * @param path the path to read e.g. /package/name/here/help.html
     * @return the contents of the file when successful, an empty string
     * otherwise.
     */
    public static String readFileFromJar(final String path) {
        final StringBuilder contents = new StringBuilder();
        final URL url = FileUtils.class.getResource(path);
        URLConnection connection;
        try {
            connection = url.openConnection();
            final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String inputLine;
                inputLine = reader.readLine();
                while (inputLine != null) {
                    contents.append(new String(inputLine.getBytes(), "UTF-8")).append("\n");
                    inputLine = reader.readLine();
                }
            }
        } catch (final IOException | NullPointerException e) {
        }
        return contents.toString();
    }

    /**
     * Copy a file from a jar.
     *
     * @param source The path to read e.g. /package/name/here/help.html
     * @param target The target to save the file to.
     */
    public static void copyFileFromJar(final String source, final String target) {
        try {
            try (InputStream inputStream = new FileUtils().getClass().getResourceAsStream(source)) {
                OutputStream out;
                out = new FileOutputStream(target);
                final byte[] buffer = new byte[4096];
                int len = inputStream.read(buffer);
                while (len != -1) {
                    out.write(buffer, 0, len);
                    len = inputStream.read(buffer);
                }
                out.close();
            }
        } catch (final FileNotFoundException e) {
        } catch (final IOException e) {
        }
    }

    /**
     * Reads a CSV-file from disk. The separator can be chosen.
     *
     * @param fileName the filename, an exception if thrown if the file does not
     * exist
     * @param separator the separator, e.g. ";" or ","
     * @param expectedColumns The expected number of columns, user -1 if the
     * number is unknown. An exception is thrown if there is a row with an
     * unexpected row length.
     * @return a List of string arrays. The data of the CSV-file can be found in
     * the arrays. Each row corresponds with an array.
     */
    public static List<String[]> readCSVFile(final String fileName, final String separator,
            final int expectedColumns) {
        final List<String[]> data = new ArrayList<>();
        FileReader fileReader;

        try {
            final File file = new File(fileName);
            if (!file.exists()) {
                throw new IllegalArgumentException("File '" + fileName + "' does not exist");
            }
            fileReader = new FileReader(file);
            try (BufferedReader in = new BufferedReader(fileReader)) {
                String inputLine;
                int lineNumber = 0;
                inputLine = in.readLine();
                while (inputLine != null) {
                    lineNumber++;
                    final String[] row = inputLine.split(separator);
                    if (expectedColumns == -1 || expectedColumns == row.length) {
                        data.add(row);
                    } else {
                        throw new AssertionError("Unexpected row length (line " + lineNumber + " ). "
                                + "Expected:" + expectedColumns + " real " + row.length
                                + ". CVS-file incorrectly formatted?");
                    }
                    inputLine = in.readLine();
                }
            }
        } catch (final IOException i1) {
        }
        return data;
    }

    public interface RowFilter {

        boolean acceptRow(String[] row);
    }
    public static final RowFilter ACCEPT_ALL_ROWFILTER = new RowFilter() {

        @Override
        public boolean acceptRow(final String[] row) {
            return true;
        }
    };

    public static List<String> readColumnFromCSVData(final List<String[]> data, final int columnIndex,
            final RowFilter filter) {
        final RowFilter actualFilter = filter == null ? ACCEPT_ALL_ROWFILTER : filter;
        final List<String> columnData = new ArrayList<>();
        for (final String[] row : data) {
            if (actualFilter.acceptRow(row)) {
                columnData.add(row[columnIndex]);
            }
        }
        return columnData;
    }

    public static void export(final String filename, final String[] header, final List<Object[]> data) {

        final String dateFormat = "yyyy-MM-dd hh:mm:ss";
        final String numberFormat = "#0.000";
        final SimpleDateFormat exportDateFormatter = new SimpleDateFormat(dateFormat);
        final DecimalFormat exportDecimalFormat = new DecimalFormat(numberFormat);
        final String separator = "\t";

        FileWriter fileWriter;
        PrintWriter output = null;
        try {
            fileWriter = new FileWriter(filename + ".csv");
            final BufferedWriter outputStream = new BufferedWriter(fileWriter);
            output = new PrintWriter(outputStream);

            if (header != null) {
                // HEADERS
                for (final String valueObject : header) {
                    String value = valueObject == null ? "" : valueObject.toString();
                    value = value.replace(separator, "");
                    output.print(value + separator);
                }
                output.println("");
            }

            // DATA
            for (final Object[] row : data) {
                for (final Object valueObject : row) {
                    String value = valueObject == null ? "" : valueObject.toString();
                    if (valueObject != null) {
                        if (valueObject instanceof Double) {
                            value = exportDecimalFormat.format(valueObject);
                        } else if (valueObject instanceof Date) {
                            value = exportDateFormatter.format(valueObject);
                        }
                    }
                    value = value.replace(separator, "");
                    output.print(value + separator);
                }
                output.println("");
            }
            outputStream.flush();
        } catch (final IOException i1) {
        } finally {
            output.close();
        }
    }

    /**
     * <p> Return a list of files in directory that satisfy pattern. Pattern
     * should be a valid regular expression not a 'unix glob pattern' so in
     * stead of
     * <code>*.wav</code> you could use
     * <code>.*\.wav</code> </p> <p> E.g. in a directory
     * <code>home</code> with the files
     * <code>test.txt</code>,
     * <code>blaat.wav</code> and
     * <code>foobar.wav</code> the pattern
     * <code>.*\.wav</code> matches
     * <code>blaat.wav</code> and
     * <code>foobar.wav</code> </p>
     *
     * @param directory A readable directory.
     * @param pattern A valid regular expression.
     * @param recursive A boolean defining if directories should be traversed
     * recursively.
     * @return a list of filenames matching the pattern for directory.
     * @exception Error an error is thrown if the directory is not ... a
     * directory.
     * @exception java.util.regex.PatternSyntaxException Unchecked exception
     * thrown to indicate a syntax error in a regular-expression pattern.
     */
    public static List<String> glob(final String directory, final String pattern, final boolean recursive) {
        final List<String> matchingFiles = new ArrayList<>();
        final Pattern p = Pattern.compile(pattern);
        final File dir = new File(new File(directory).getAbsolutePath());
        glob(dir, p, recursive, matchingFiles);
        // sort alphabetically
        Collections.sort(matchingFiles);
        return matchingFiles;
    }

    private static void glob(final File directory, final Pattern pattern, final boolean recursive,
            List<String> matchingFiles) {

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        for (final String file : directory.list()) {
            File filePath = new File(FileUtils.combine(directory.getAbsolutePath(), file));
            if (recursive && filePath.isDirectory()) {
                glob(filePath, pattern, recursive, matchingFiles);
            } else {
                if (pattern.matcher(file).matches() && file != null) {
                    matchingFiles.add(filePath.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Return the extension of a file.
     *
     * @param fileName the file to get the extension for
     * @return the extension. E.g. TXT or JPEG.
     */
    public static String extension(final String fileName) {
        final int dot = fileName.lastIndexOf('.');
        return dot == -1 ? "" : fileName.substring(dot + 1);
    }

    /**
     * Returns the filename without path and without extension.
     *
     * @param fileName
     * @return the file name without extension and path
     */
    public static String basename(final String fileName) {
        int dot = fileName.lastIndexOf('.');
        int sep = fileName.lastIndexOf(File.separatorChar);
        if (sep == -1) {
            sep = fileName.lastIndexOf('\\');
        }
        if (dot == -1) {
            dot = fileName.length();
        }
        return fileName.substring(sep + 1, dot);
    }

    /**
     * Returns the path for a file.<br>
     * <code>path("/home/user/test.jpg") == "/home/user"</code><br> Uses the
     * correct pathSeparator depending on the operating system. On windows
     * c:/test/ is not c:\test\
     *
     * @param fileName the name of the file using correct path separators.
     * @return the path of the file.
     */
    public static String path(final String fileName) {
        final int sep = fileName.lastIndexOf(File.separatorChar);
        return fileName.substring(0, sep);
    }

    /**
     * Checks if a file exists.
     *
     * @param fileName the name of the file to check.
     * @return true if and only if the file or directory denoted by this
     * abstract pathname exists; false otherwise
     */
    public static boolean exists(final String fileName) {
        return new File(fileName).exists();
    }

    /**
     * Creates a directory and parent directories if needed.
     *
     * @param path the path of the directory to create
     * @return true if the directory was created (possibly with parent
     * directories) , false otherwise
     */
    public static boolean mkdirs(final String path) {
        return new File(path).mkdirs();
    }

    /**
     * Copy from source to target.
     *
     * @param source the source file.
     * @param target the target file.
     */
    public static void cp(final String source, final String target) {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(new File(source)).getChannel();
            outChannel = new FileOutputStream(new File(target)).getChannel();
            // JavaVM does its best to do this as native I/O operations.
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (final FileNotFoundException e) {
        } catch (final IOException e) {
        } finally {
            try {
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (final IOException e) {
                // ignore
            }
        }
    }

    /**
     * Removes a file from disk.
     *
     * @param fileName the file to remove
     * @return true if and only if the file or directory is successfully
     * deleted; false otherwise
     */
    public static boolean rm(final String fileName) {
        return new File(fileName).delete();
    }

    /**
     * Tests whether the file denoted by this abstract pathname is a directory.
     *
     * @param inputFile A pathname string.
     * @return true if and only if the file denoted by this abstract pathname
     * exists and is a directory; false otherwise.
     */
    public static boolean isDirectory(final String inputFile) {
        return new File(inputFile).isDirectory();
    }

    /**
     * Checks if the name of the file (extension) is a known audio extension.
     *
     * @param file The file to check.
     * @return True if the name of the file matches
     * ConfKey.audio_file_name_pattern, false otherwise.
     */
    public static boolean isAudioFile(final File file) {
        return file.getAbsolutePath().matches(Configuration.get(ConfKey.audio_file_name_pattern));
    }
}
