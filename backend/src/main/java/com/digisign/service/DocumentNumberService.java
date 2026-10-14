package com.digisign.service;

import com.digisign.config.AppProperties;
import com.digisign.domain.DocumentType;
import com.digisign.repository.SequenceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Penomoran otomatis berdasarkan format jenis dokumen. Token yang didukung:
 * {SEQ} / {SEQ:n} (nomor urut per tahun, n digit), {CODE}, {YEAR}, {YY}, {MONTH}, {ROMAN_MONTH}, {DAY}
 * Contoh: {SEQ:4}/BAST/{ROMAN_MONTH}/{YEAR} -> 0007/BAST/X/2026
 */
@Service
public class DocumentNumberService {

    private static final Pattern SEQ = Pattern.compile("\\{SEQ(?::(\\d))?}");
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};

    private final SequenceRepository sequences;
    private final ZoneId zone;

    public DocumentNumberService(SequenceRepository sequences, AppProperties props) {
        this.sequences = sequences;
        this.zone = ZoneId.of(props.timezone());
    }

    public String next(DocumentType type) {
        LocalDate today = LocalDate.now(zone);
        int seq = sequences.next(type.getId(), today.getYear());
        return format(type.getNumberFormat(), type.getCode(), today, seq);
    }

    public static String format(String pattern, String code, LocalDate date, int seq) {
        String s = pattern == null || pattern.isBlank() ? "{SEQ:4}/{CODE}/{ROMAN_MONTH}/{YEAR}" : pattern;
        Matcher m = SEQ.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int digits = m.group(1) == null ? 1 : Integer.parseInt(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(String.format("%0" + digits + "d", seq)));
        }
        m.appendTail(sb);
        return sb.toString()
                .replace("{CODE}", code)
                .replace("{YEAR}", String.valueOf(date.getYear()))
                .replace("{YY}", String.format("%02d", date.getYear() % 100))
                .replace("{MONTH}", String.format("%02d", date.getMonthValue()))
                .replace("{ROMAN_MONTH}", ROMAN[date.getMonthValue() - 1])
                .replace("{DAY}", String.format("%02d", date.getDayOfMonth()));
    }
}
