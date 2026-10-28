package com.instakill.imageprocessing.application;

import com.instakill.common.error.BadRequestException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WatermarkingImageProcessingServiceTest {

    private static final Color BACKGROUND = new Color(128, 128, 128);

    private final WatermarkingImageProcessingService service = new WatermarkingImageProcessingService();

    @Test
    void scalesWideImageToMaxWidthKeepingAspectRatio() throws IOException {
        byte[] result = service.process(imageBytes(800, 400, "png"), "png");

        BufferedImage processed = decode(result);
        assertThat(processed.getWidth()).isEqualTo(300);
        assertThat(processed.getHeight()).isEqualTo(150);
    }

    @Test
    void keepsWidthOfSmallImage() throws IOException {
        byte[] result = service.process(imageBytes(100, 50, "png"), "png");

        BufferedImage processed = decode(result);
        assertThat(processed.getWidth()).isEqualTo(100);
        assertThat(processed.getHeight()).isEqualTo(50);
    }

    @Test
    void drawsWatermarkInBottomRightCorner() throws IOException {
        byte[] result = service.process(imageBytes(300, 150, "png"), "png");

        BufferedImage processed = decode(result);
        boolean watermarkFound = false;
        for (int x = 200; x < 300 && !watermarkFound; x++) {
            for (int y = 120; y < 150 && !watermarkFound; y++) {
                watermarkFound = processed.getRGB(x, y) != BACKGROUND.getRGB();
            }
        }
        assertThat(watermarkFound).isTrue();
    }

    @Test
    void processesJpeg() throws IOException {
        byte[] result = service.process(imageBytes(800, 400, "jpg"), "jpg");

        assertThat(decode(result).getWidth()).isEqualTo(300);
    }

    @Test
    void rejectsBytesThatAreNotAnImage() {
        assertThatThrownBy(() -> service.process("not an image".getBytes(), "png"))
                .isInstanceOf(BadRequestException.class);
    }

    private static byte[] imageBytes(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height,
                "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
}
