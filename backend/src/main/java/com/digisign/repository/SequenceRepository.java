package com.digisign.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Nomor urut dokumen per jenis per tahun — atomik via UPSERT PostgreSQL. */
@Repository
public class SequenceRepository {

    private final JdbcTemplate jdbc;

    public SequenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int next(long documentTypeId, int year) {
        Integer v = jdbc.queryForObject("""
                INSERT INTO document_type_sequence (document_type_id, period_year, last_value)
                VALUES (?, ?, 1)
                ON CONFLICT (document_type_id, period_year)
                DO UPDATE SET last_value = document_type_sequence.last_value + 1
                RETURNING last_value
                """, Integer.class, documentTypeId, year);
        return v == null ? 1 : v;
    }
}
