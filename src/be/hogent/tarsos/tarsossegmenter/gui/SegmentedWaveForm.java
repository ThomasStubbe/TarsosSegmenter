package be.hogent.tarsos.tarsossegmenter.gui;

import be.hogent.tarsos.tarsossegmenter.controller.listeners.AASModelListener;
import be.hogent.tarsos.tarsossegmenter.controller.listeners.AudioFileListener;
import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.model.AudioFile;
import be.hogent.tarsos.tarsossegmenter.model.player.Player;
import be.hogent.tarsos.tarsossegmenter.model.segmentation.SegmentationPart;
import be.hogent.tarsos.tarsossegmenter.util.TimeUnit;
import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * <p>
 * A graphical user interface displaying the waveform and the macro segmentation
 * of the audiotrack.
 * </p>
 *
 * @author Thomas Stubbe, Joren Six
 */
public final class SegmentedWaveForm extends JPanel implements AudioFileListener, AASModelListener {

    AASModel model;
    private boolean needsScaledUpdate;
    private boolean scaling;
    //private static final long serialVersionUID = 3730361987954996673L;
    /**
     * Logs messages.
     */
    private AudioFile audioFile;
    //private double minMarkerPosition; // position in seconds
    //private double maxMarkerPosition; // position in seconds
    /**
     * A cached waveform image used to scale to the correct height and width.
     */
    private BufferedImage waveFormImage;
    /**
     * The same image scaled to the current height and width.
     */
    private BufferedImage scaledWaveFormImage;
    /**
     * The font used to draw axis labels.
     */
    private static final Font AXIS_FONT = new Font("SansSerif", Font.TRUETYPE_FONT, 10);

    public SegmentedWaveForm() {

        this.model = AASModel.getInstance();
        needsScaledUpdate = false;
        scaling = false;
        model.addModelListener(this);
        model.addAudioFileChangedListener(this);
        this.audioFile = model.getAudioFile();
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    setMarkerInPixels(event.getX(), false);
                } else {
                    setMarkerInPixels(event.getX(), true);
                }
//                AnnotationPublisher.getInstance().clear();
//                AnnotationPublisher.getInstance().alterSelection(minMarkerPosition, maxMarkerPosition);
//                AnnotationPublisher.getInstance().delegateAddAnnotations(minMarkerPosition, maxMarkerPosition);
            }
        });
        //minMarkerPosition = 0;

        //AASModel.getInstance().get
//        setMarker(0, true);
//        setMarker(0, false);
        this.audioFileChanged();
    }

    /**
     * Sets the marker position in pixels.
     *
     * @param newPosition The new position in pixels.
     */
    private void setMarkerInPixels(final int newPosition, final boolean minMarker) {
        float newPositionInTime = (float) (getLengthInSeconds() / (double) getWidth()) * newPosition;
        if (minMarker && newPositionInTime < Player.getInstance().getEndSelection()) {
            Player.getInstance().setStartSelection(newPositionInTime);
            requestRepaint();
        } else if (!minMarker && newPositionInTime > Player.getInstance().getStartSelection()) {
            Player.getInstance().setEndSelection(newPositionInTime);
            requestRepaint();
        }

    }
    private boolean waveFormCreationFinished = false;

    private void setWaveFormCreationFinished(boolean isFinished) {
        waveFormCreationFinished = isFinished;
    }

    /**
     * Sets the marker position in seconds.
     *
     * @param newPosition The new position of the marker in seconds.
     * @param minMarker True if the marker to place is the marker at the left,
     * the minimum. False otherwise.
     */
//    public void setMarker(final double newPosition, final boolean minMarker) {
//        if (minMarker && newPosition < maxMarkerPosition) {
//            minMarkerPosition = newPosition;
//        } else if (!minMarker && newPosition > minMarkerPosition) {
//            maxMarkerPosition = newPosition;
//        }
//        requestRepaint();
//    }
//    public double getMarker(final boolean minMarker) {
//        final double markerValue;
//        if (minMarker) {
//            markerValue = minMarkerPosition;
//        } else {
//            markerValue = maxMarkerPosition;
//        }
//        return markerValue;
//    }
    private void requestRepaint() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
    }

    @Override
    public void paint(final Graphics g) {
        Graphics2D graphics = (Graphics2D) g;
        initializeGraphics(graphics);
        if (waveFormImage != null && waveFormCreationFinished) {
            drawWaveForm(graphics);
        } else {
            graphics.transform(getSaneTransform());
            drawReference(graphics);
            graphics.transform(getInverseSaneTransform());
        }
        g.setColor(Color.black);
        g.drawLine(0, 0, 0, this.getHeight());
        g.drawLine(this.getWidth() - 1, 0, this.getWidth() - 1, this.getHeight());
        
        g.drawLine(0, 0, this.getWidth() - 1, 0);
        g.drawLine(0, this.getHeight()-1, this.getWidth() - 1, this.getHeight()-1);
        
        drawMarker(graphics);
    }

    /**
     * <pre>
     *  (0,h/2)              (+w,h/2)
     *      -------------------|
     *      |                  |
     *      |                  |
     *      |                  |
     * (0,0)|------------------| (0,h/2)
     *      |                  |
     *      |                  |
     *      |                  |
     *      -------------------
     *  (0,-h/2)            (w,-h/2)
     * </pre>
     *
     * @return A transform where (0,0) is in the middle left of the screen.
     * Positive y is up, negative y down.
     */
    private AffineTransform getSaneTransform() {
        return getSaneTransform(getHeight());
    }

    private AffineTransform getSaneTransform(final float heigth) {
        return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, heigth / 2);
    }

    private AffineTransform getInverseSaneTransform() {
        return getInverseSaneTransform(getHeight());
    }

    private AffineTransform getInverseSaneTransform(final float heigth) {
        return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, heigth / 2);
    }

    private void drawMarker(final Graphics2D graphics) {
        int minX = (int) secondsToPixels(Player.getInstance().getStartSelection());
        graphics.transform(getSaneTransform());
        graphics.setColor(Color.black);
        graphics.drawLine(minX, getHeight() / 2, minX, -getHeight() / 2);
        int maxX = (int) secondsToPixels(Player.getInstance().getEndSelection());
        graphics.setColor(Color.black);
        graphics.drawLine(maxX, getHeight() / 2, maxX, -getHeight() / 2);
        Color color = new Color(0.0f, 0.0f, 0.0f, 0.15f); // black
        // graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
        // 0.5f));
        graphics.setPaint(color);
        Rectangle rectangle = new Rectangle(minX + 1, -getHeight() / 2, maxX - minX, getHeight() * 4);
        graphics.fill(rectangle);
        graphics.transform(getInverseSaneTransform());
    }

    private void initializeGraphics(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.BLACK);
    }

    private void drawWaveForm(final Graphics2D g) {
        // Render the cached image.
        scaleWaveFormImage();
        g.drawImage(scaledWaveFormImage, 0, 0, null);
    }

    public BufferedImage scaleWaveFormImage() {
        if (waveFormCreationFinished && !scaling && (scaledWaveFormImage == null || scaledWaveFormImage.getHeight() != getHeight()
                || scaledWaveFormImage.getWidth() != getWidth() || needsScaledUpdate)) {
            scaling = true;
            //StopWatch watch = new StopWatch();

            int sourceWidth = waveFormImage.getWidth();
            int sourceHeight = waveFormImage.getHeight();
            int destWidth = getWidth();
            int destHeight = getHeight();

            double xScale = (double) getWidth() / (double) sourceWidth;
            double yScale = (double) getHeight() / (double) sourceHeight;

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();

            scaledWaveFormImage = gc.createCompatibleImage(destWidth, destHeight, waveFormImage.getColorModel().getTransparency());
            Graphics2D g2d = null;
            try {
                g2d = scaledWaveFormImage.createGraphics();
                g2d.getTransform();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                AffineTransform at = AffineTransform.getScaleInstance(xScale, yScale);
                g2d.drawRenderedImage(waveFormImage, at);
                g2d.transform(getInverseSaneTransform());
                drawReference(g2d);
            } finally {
                if (g2d != null) {
                    g2d.dispose();
                }
            }
            needsScaledUpdate = false;
            scaling = false;
            //LOG.fine("Rescaled wave form image in " + watch.formattedToString());
        }
        return scaledWaveFormImage;
    }

    /**
     * Draws a string on canvas using the current transform but makes sure the
     * text is not flipped.
     *
     * @param graphics The canvas.
     * @param text The string to draw.
     * @param x The ( by the current affine transform transformed) x location.
     * @param y The ( by the current affine transform transformed) y location.
     */
    private void drawString(final Graphics2D graphics, final String text, final double x, final double y) {
        Point2D source = new Point2D.Double(x, y);
        Point2D destination = new Point2D.Double();
        AffineTransform transform = graphics.getTransform();
        graphics.transform(getInverseSaneTransform());
        transform.transform(source, destination);
        graphics.drawString(text, (int) destination.getX(), (int) destination.getY());
        graphics.transform(getSaneTransform());
    }

    private double secondsToPixels(double seconds) {
        return seconds * getWidth() / getLengthInSeconds();
    }

    private double getLengthInSeconds() {
        final double lengthInSeconds;
        if (audioFile == null) {
            // default length = 200 sec;
            lengthInSeconds = 200;
        } else {
            lengthInSeconds = audioFile.getLengthIn(TimeUnit.SECONDS);
        }
        return lengthInSeconds;
    }

    /**
     * Draw reference lines on the canvas: minute markers and 1 and -1 amplitude
     * markers.
     *
     * @param g The canvas.
     */
    private void drawReference(final Graphics2D g) {
        final int width = getWidth();
        final int height = getHeight();
        final int one = (int) (height / 2 * 0.85);
        g.setColor(Color.GRAY);
        // line in center
        g.drawLine(0, 0, width, 0);
        // mark one and minus one left (y axis)
        g.drawLine(0, one, 3, one);
        g.drawLine(0, -one, 3, -one);

        g.setFont(AXIS_FONT);
        drawString(g, " 1.0", 6, one - 3);
        drawString(g, "-1.0", 6, -one - 3);

        // mark one and minus one right (y axis)
        g.drawLine(width, one, width - 3, one);
        g.drawLine(width, -one, width - 3, -one);

        // start at 10 sec;
        for (int i = 10; i < getLengthInSeconds(); i += 10) {
            int x = (int) secondsToPixels(i);
            int y = height / 2;

            final int markerSize;
            if (i % 60 == 0) {
                markerSize = (int) (height / 2 * 0.15);
                // minute markers
                drawString(g, i / 60 + ":00", x - 8, y - markerSize - 9);
            } else {
                // marker every 10 sec
                markerSize = (int) (height / 2 * 0.05);
            }
            g.drawLine(x, y, x, y - markerSize);
            g.drawLine(x, -y, x, -y + markerSize);
        }
        g.setColor(Color.BLACK);
    }

    @Override
    public void audioFileChanged() {
        this.audioFile = model.getAudioFile();
        this.waveFormImage = null;
        this.scaledWaveFormImage = null;
        setWaveFormCreationFinished(false);
        createWaveFormImage();
    }

    private void createWaveFormImage() {
        //final StopWatch watch = new StopWatch();

        try {
            final int waveFormHeight = 300;
            final int waveFormWidth = 1800;
            waveFormImage = new BufferedImage(waveFormWidth, waveFormHeight, BufferedImage.TYPE_INT_RGB);
            final Graphics2D waveFormGraphics = waveFormImage.createGraphics();
            initializeGraphics(waveFormGraphics);
            waveFormGraphics.setColor(Color.white);
            waveFormGraphics.clearRect(0, 0, waveFormWidth, waveFormHeight);



            //Paint markers
            if (audioFile != null) {
                ArrayList<SegmentationPart> macroSegmentation = model.getSegmentation().getSegmentation();

                //Draw the segments on the waveForm
                if (macroSegmentation != null) {
                    float songDuration = (float) audioFile.getLengthIn(TimeUnit.SECONDS);
                    for (int i = 0; i < macroSegmentation.size(); i++) {
                        waveFormGraphics.setColor(macroSegmentation.get(i).getColor());
                        int x1 = (int) (macroSegmentation.get(i).getBegin() / songDuration * waveFormWidth);
                        int x2 = (int) (macroSegmentation.get(i).getEnd() / songDuration * waveFormWidth);
                        waveFormGraphics.fillRect(x1 + 1, 0, x2, waveFormHeight);
                        waveFormGraphics.setColor(Color.BLACK);
                        waveFormGraphics.drawLine(x2, 0, x2, waveFormHeight);

                    }
                }



                waveFormGraphics.transform(getSaneTransform(waveFormHeight));
                final float frameRate = audioFile.fileFormat().getFormat().getFrameRate();
                int framesPerPixel = audioFile.fileFormat().getFrameLength() / waveFormWidth / 8;

                waveFormGraphics.setColor(Color.black);

                final int one = (int) (waveFormHeight / 2 * 0.85);

                final double secondsToX;
                secondsToX = 1000 * waveFormWidth / (float) audioFile.getLengthInMilliSeconds();
                AudioDispatcher adp = AudioDispatcher.fromFile(new File(audioFile.transcodedPath()), framesPerPixel, 0);
                adp.addAudioProcessor(new AudioProcessor() {
                    private int frame = 0;

                    @Override
                    public void processingFinished() {
                        setWaveFormCreationFinished(true);
                        needsScaledUpdate = true;
                        invalidate();
                        requestRepaint();
                    }

                    @Override
                    public boolean process(AudioEvent audioEvent) {
                        float[] audioFloatBuffer = audioEvent.getFloatBuffer();
                        double seconds = frame / frameRate;
                        frame += audioFloatBuffer.length;
                        int x = (int) (secondsToX * seconds);
                        int y = (int) (audioFloatBuffer[0] * one);
                        waveFormGraphics.drawLine(x, 0, x, y);
                        return true;
                    }
                });
                new Thread(adp, "Waveform image builder").start();
            }

        } catch (UnsupportedAudioFileException | IOException e) {
            //@TODO: popup?
            e.printStackTrace();
        }

    }

    @Override
    public void calculationStarted() {
    }

    @Override
    public void calculationDone() {
        update();
    }

    public void update() {
        this.createWaveFormImage();
    }
}
