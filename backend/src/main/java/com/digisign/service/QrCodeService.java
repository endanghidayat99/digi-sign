package com.digisign.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Membuat QR Code dengan logo perusahaan di tengah.
 * Menggunakan Error Correction level H (~30%) agar QR tetap terbaca walau tengahnya tertutup logo.
 */
@Service
public class QrCodeService {

    private static final Color DARK = new Color(0x1A, 0x1A, 0x1A);

    private final SettingService settings;

    public QrCodeService(SettingService settings) {
        this.settings = settings;
    }

    public BufferedImage generate(String content, int sizePx) {
        return generate(content, sizePx, settings.logo(), settings.getDouble(SettingService.QR_LOGO_RATIO, 0.22));
    }

    public BufferedImage generate(String content, int sizePx, byte[] logoBytes, double logoRatio) {
        try {
            var hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            BufferedImage img = new BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, sizePx, sizePx);
            g.setColor(DARK);
            for (int y = 0; y < sizePx; y++) {
                for (int x = 0; x < sizePx; x++) {
                    if (matrix.get(x, y)) img.setRGB(x, y, DARK.getRGB());
                }
            }

            // batasi rasio logo maksimal 0.28 supaya QR tetap bisa dipindai
            double ratio = Math.max(0.10, Math.min(0.28, logoRatio));
            int logoBox = (int) Math.round(sizePx * ratio);
            int pad = Math.max(4, logoBox / 10);
            int boxX = (sizePx - logoBox) / 2;
            int boxY = (sizePx - logoBox) / 2;

            // latar putih membulat di tengah
            g.setColor(Color.WHITE);
            int arc = logoBox / 4;
            g.fill(new RoundRectangle2D.Float(boxX - pad, boxY - pad, logoBox + 2f * pad, logoBox + 2f * pad, arc, arc));

            BufferedImage logo = readLogo(logoBytes);
            if (logo != null) {
                // jaga rasio aspek logo
                double scale = Math.min((double) logoBox / logo.getWidth(), (double) logoBox / logo.getHeight());
                int w = (int) Math.round(logo.getWidth() * scale);
                int h = (int) Math.round(logo.getHeight() * scale);
                g.drawImage(logo, boxX + (logoBox - w) / 2, boxY + (logoBox - h) / 2, w, h, null);
            } else {
                drawDefaultMark(g, boxX, boxY, logoBox);
            }
            g.dispose();
            return img;
        } catch (Exception e) {
            throw new IllegalStateException("Gagal membuat QR Code", e);
        }
    }

    public byte[] toPng(BufferedImage img) {
        try (var out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BufferedImage readLogo(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }

    /** Tanda default (tanda centang dalam lingkaran) bila logo perusahaan belum diunggah. */
    private void drawDefaultMark(Graphics2D g, int x, int y, int size) {
        g.setColor(new Color(0x15, 0x65, 0xC0));
        g.fillOval(x, y, size, size);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(2f, size / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[] xs = {x + size * 27 / 100, x + size * 44 / 100, x + size * 74 / 100};
        int[] ys = {y + size * 52 / 100, y + size * 68 / 100, y + size * 36 / 100};
        g.drawPolyline(xs, ys, 3);
    }
}
