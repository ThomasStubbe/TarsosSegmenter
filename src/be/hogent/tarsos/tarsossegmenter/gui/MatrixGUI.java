/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.gui;

import be.hogent.tarsos.tarsossegmenter.controller.listeners.AASModelListener;
import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.model.player.Player;
import be.hogent.tarsos.tarsossegmenter.model.player.PlayerState;
import be.hogent.tarsos.tarsossegmenter.util.TimeUnit;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * <p>
 * A graphical user interface displaying the matrix
 * </p>
 *
 * @author Thomas Stubbe
 *
 */
public class MatrixGUI extends JPanel implements MouseListener, AASModelListener {

    private final int MAX_PIXEL_VALUE = 255;
    private int size;
    private BufferedImage matrixImage;
    private BufferedImage initialMatrixImage;
    private BufferedImage scaledMatrixImage;
    private int[][] graphLine;
    private boolean constructingImage;
    private boolean displayInitialMatrix;
    private boolean needsUpdate;

    public MatrixGUI() {
        super();
        constructingImage = false;
        needsUpdate = false;
        displayInitialMatrix = false;
        this.addMouseListener(this);
        AASModel.getInstance().addModelListener(this);
    }

    public void constructInitialMatrix(float[][] matrix) {
        if (matrix != null) {
            size = matrix.length;
            if (initialMatrixImage != null) {
                initialMatrixImage.flush();
            }
            initialMatrixImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            constructMatrix(initialMatrixImage, matrix);
        }
    }

    public void constructMatrix(float[][] matrix) {
        if (matrix != null) {
            size = matrix.length;
            if (matrixImage != null) {
                matrixImage.flush();
            }
            matrixImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            constructMatrix(matrixImage, matrix);
        }
    }

    private void constructMatrix(BufferedImage image, float[][] matrix) {
        constructingImage = true;

        if (scaledMatrixImage != null) {
            scaledMatrixImage.flush();
        }

        float max = determineMax(matrix);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j <= i; j++) {
                int brightness = (int) ((matrix[i][j] / max) * MAX_PIXEL_VALUE);
                Color color = new Color(brightness, brightness, brightness);
                image.setRGB(i, j, color.getRGB());
                image.setRGB(j, i, color.getRGB());
            }
        }
        constructingImage = false;
        scaleImage();
    }

    public void addNoveltyScore(float[][] noveltyScore) {
        graphLine = new int[noveltyScore.length][size];
        for (int i = 0; i < noveltyScore.length; i++) {
            if (noveltyScore[i] != null) {
                float max = 0;
                for (int j = 0; j < size; j++) {
                    if (Math.abs(noveltyScore[i][j]) > max) {
                        max = Math.abs(noveltyScore[i][j]);
                    }
                }
                for (int j = 0; j < size - 2; j++) {
                    int value = (int) (noveltyScore[i][j] / max * (size - 2));
                    graphLine[i][j] = value;
                }
            }
        }
        scaleImage();
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        scaleImage();
        while (constructingImage) {
        }
        final Graphics2D graphics = (Graphics2D) g;
        //graphics.scale((double) this.getWidth() / (double) size, (double) this.getHeight() / (double) size);

        if (scaledMatrixImage != null) {
            graphics.drawImage(scaledMatrixImage, 0, 0, this);
            graphics.setColor(Color.RED);
            if (graphLine != null) {
                int level = AASModel.getInstance().getSegmentation().getActiveSegmentationLevel();
                for (int i = 1; i < size; i++) {
                    graphics.drawLine((int) (i * this.getWidth() / size), (int) (graphLine[level][i] * this.getHeight() / size), (int) ((i - 1) * this.getWidth() / size), (int) (graphLine[level][i - 1] * this.getHeight() / size));
                }
            }
        }
        graphics.dispose();
        g.dispose();
    }

    private float determineMax(float[][] matrix) {
        float max = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j <= i; j++) {
                if (matrix[i][j] > max) {
                    max = matrix[i][j];
                }
            }
        }
        return max;
    }

    private void scaleImage() {
        if (!constructingImage && (scaledMatrixImage == null || scaledMatrixImage.getHeight() != getHeight()
                || scaledMatrixImage.getWidth() != getWidth() || needsUpdate)) {
            constructingImage = true;
            if (scaledMatrixImage != null) {
                scaledMatrixImage.flush();
            }
            BufferedImage image;
            if (displayInitialMatrix) {
                image = initialMatrixImage;
            } else {
                image = matrixImage;
            }

            //StopWatch watch = new StopWatch();
            if (image != null) {
                int sourceWidth = image.getWidth();
                int sourceHeight = image.getHeight();
                int destWidth = getWidth();
                int destHeight = getHeight();
                if (destWidth > 0 && destHeight > 0) {
                    double xScale = (double) getWidth() / (double) sourceWidth;
                    double yScale = (double) getHeight() / (double) sourceHeight;

                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice gd = ge.getDefaultScreenDevice();
                    GraphicsConfiguration gc = gd.getDefaultConfiguration();

                    scaledMatrixImage = gc.createCompatibleImage(destWidth, destHeight, image.getColorModel().getTransparency());
                    Graphics2D g2d = null;
                    try {
                        g2d = scaledMatrixImage.createGraphics();
                        g2d.getTransform();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                        AffineTransform at = AffineTransform.getScaleInstance(xScale, yScale);
                        g2d.drawRenderedImage(image, at);
                        g2d.transform(getInverseSaneTransform());
                    } finally {
                        if (g2d != null) {
                            g2d.dispose();
                        }
                    }
                }
            }
            if (needsUpdate) {
                needsUpdate = false;
            }
            constructingImage = false;
        }
    }

    private AffineTransform getInverseSaneTransform() {
        return getInverseSaneTransform(getHeight());
    }

    private AffineTransform getInverseSaneTransform(final float heigth) {
        return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, heigth / 2);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        double time = (double) e.getX() / (double) this.getWidth() * (double) AASModel.getInstance().getAudioFile().getLengthIn(TimeUnit.SECONDS);
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (Player.getInstance().getState() == PlayerState.PLAYING) {
                Player.getInstance().pauze();
                Player.getInstance().play(time);
            } else if (Player.getInstance().getState() != PlayerState.NO_FILE_LOADED) {
                Player.getInstance().play(time);
                Player.getInstance().pauze(time);
            }
        }

    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public void setDisplayInitialMatrix(boolean value) {
        if (displayInitialMatrix != value) {
            this.displayInitialMatrix = value;
            needsUpdate = true;
            this.repaint();
        }
    }

    @Override
    public void calculationStarted() {
    }

    @Override
    public void calculationDone() {
        constructMatrix(AASModel.getInstance().getSimilarityMatrix());
        constructInitialMatrix(AASModel.getInstance().getInitialSimilarityMatrix());
        if (AASModel.getInstance().getNoveltyScore() != null) {
            addNoveltyScore(AASModel.getInstance().getNoveltyScore());
        } else {
            this.graphLine = null;
        }
        needsUpdate = true;
        this.repaint();
    }
}
