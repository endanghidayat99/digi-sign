package com.digisign.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;

/** Menempelkan QR Code (dan keterangan singkat opsional) ke halaman PDF. */
@Service
public class PdfStampService {

    public record Placement(int page, double relX, double relY, double relSize, boolean caption) {}

    public record PdfInfo(int pageCount) {}

    public PdfInfo inspect(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.isEncrypted()) {
                throw new IllegalArgumentException("PDF terenkripsi/terproteksi password tidak didukung");
            }
            return new PdfInfo(doc.getNumberOfPages());
        } catch (IOException e) {
            throw new IllegalArgumentException("File bukan PDF yang valid");
        }
    }

    /**
     * @param p posisi relatif terhadap halaman (0..1), titik acuan kiri-atas seperti tampilan di layar.
     *          relSize = lebar QR relatif terhadap lebar halaman.
     */
    public byte[] stamp(byte[] pdf, BufferedImage qr, Placement p, String[] captionLines) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            int pageIndex = Math.max(1, Math.min(p.page(), doc.getNumberOfPages())) - 1;
            PDPage page = doc.getPage(pageIndex);

            // Ukuran halaman sesuai tampilan (memperhitungkan rotasi halaman)
            PDRectangle box = page.getCropBox();
            int rotation = ((page.getRotation() % 360) + 360) % 360;
            boolean swap = rotation == 90 || rotation == 270;
            float viewW = swap ? box.getHeight() : box.getWidth();
            float viewH = swap ? box.getWidth() : box.getHeight();

            float size = (float) (clamp(p.relSize(), 0.04, 0.5) * viewW);
            float captionFont = Math.max(4.5f, size * 0.075f);
            float captionH = p.caption() && captionLines != null ? captionLines.length * captionFont * 1.25f + 2 : 0;

            float x = (float) (clamp(p.relX(), 0, 1) * viewW);
            float yTop = (float) (clamp(p.relY(), 0, 1) * viewH);
            x = Math.min(x, viewW - size);
            yTop = Math.min(yTop, viewH - size - captionH);
            float y = viewH - yTop - size; // koordinat PDF (kiri-bawah) pada ruang tampilan

            PDImageXObject image = LosslessFactory.createFromImage(doc, qr);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                // transformasi dari ruang tampilan ke ruang halaman bila halaman diputar
                cs.transform(viewToPage(rotation, box));
                cs.drawImage(image, x, y, size, size);

                if (captionH > 0) {
                    var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    var bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    float ty = y - captionFont;
                    for (int i = 0; i < captionLines.length; i++) {
                        String line = toWinAnsi(captionLines[i]);
                        var f = i == 1 ? bold : font;
                        line = fit(line, f, captionFont, size + 30);
                        float tw = f.getStringWidth(line) / 1000 * captionFont;
                        cs.beginText();
                        cs.setFont(f, captionFont);
                        cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
                        cs.newLineAtOffset(x + (size - tw) / 2, ty);
                        cs.showText(line);
                        cs.endText();
                        ty -= captionFont * 1.25f;
                    }
                }
            }

            var out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Gagal menempelkan QR ke PDF", e);
        }
    }

    private static Matrix viewToPage(int rotation, PDRectangle box) {
        float llx = box.getLowerLeftX(), lly = box.getLowerLeftY();
        float w = box.getWidth(), h = box.getHeight();
        return switch (rotation) {
            case 90 -> new Matrix(0, 1, -1, 0, llx + w, lly);
            case 180 -> new Matrix(-1, 0, 0, -1, llx + w, lly + h);
            case 270 -> new Matrix(0, -1, 1, 0, llx, lly + h);
            default -> new Matrix(1, 0, 0, 1, llx, lly);
        };
    }

    private static String fit(String s, PDType1Font f, float fontSize, float maxW) throws IOException {
        String r = s;
        while (r.length() > 3 && f.getStringWidth(r) / 1000 * fontSize > maxW) {
            r = r.substring(0, r.length() - 2);
        }
        return r.equals(s) ? s : r + "...";
    }

    /** Font standar PDF hanya mendukung WinAnsi; buang aksen/karakter di luar itu. */
    private static String toWinAnsi(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        StringBuilder sb = new StringBuilder();
        for (char c : n.toCharArray()) sb.append(c >= 32 && c < 127 ? c : '?');
        return sb.toString();
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
