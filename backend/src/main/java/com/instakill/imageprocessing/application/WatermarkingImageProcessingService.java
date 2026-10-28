package com.instakill.imageprocessing.application;

import com.instakill.common.error.BadRequestException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Service
public class WatermarkingImageProcessingService implements ImageProcessingService {

    private static final int MAX_WIDTH = 300;
    private static final String WATERMARK_TEXT = "Instakill Demo";
    private static final int MARGIN = 8;

    @Override
    public byte[] process(byte[] image, String format) {
        BufferedImage source = read(image);
        int width = Math.min(source.getWidth(), MAX_WIDTH);
        int height = source.getHeight() * width / source.getWidth();

        int type = "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage canvas = new BufferedImage(width, height, type);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, width, height, null);
        drawWatermark(graphics, width, height);
        graphics.dispose();

        return write(canvas, format);
    }

    private BufferedImage read(byte[] image) {
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
            if (decoded == null) {
                throw new BadRequestException("Invalid image");
            }
            return decoded;
        } catch (IOException e) {
            throw new BadRequestException("Invalid image");
        }
    }

    private void drawWatermark(Graphics2D graphics, int width, int height) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, width / 15));
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        int x = width - metrics.stringWidth(WATERMARK_TEXT) - MARGIN;
        int y = height - MARGIN - metrics.getDescent();

        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        graphics.setColor(Color.BLACK);
        graphics.drawString(WATERMARK_TEXT, x + 1, y + 1);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        graphics.setColor(Color.WHITE);
        graphics.drawString(WATERMARK_TEXT, x, y);
    }

    private byte[] write(BufferedImage canvas, String format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(canvas, format, out)) {
                throw new IllegalStateException("No image writer for format " + format);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
