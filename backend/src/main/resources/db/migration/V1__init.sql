CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    position        VARCHAR(150),
    department      VARCHAR(150),
    employee_no     VARCHAR(50),
    email           VARCHAR(150),
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE document_type (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30)  NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    -- Token: {SEQ} {SEQ:n} {CODE} {YEAR} {YY} {MONTH} {ROMAN_MONTH} {DAY}
    number_format   VARCHAR(200) NOT NULL DEFAULT '{SEQ:4}/{CODE}/{ROMAN_MONTH}/{YEAR}',
    color           VARCHAR(20)  DEFAULT '#1565C0',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE document_type_sequence (
    document_type_id BIGINT  NOT NULL REFERENCES document_type(id) ON DELETE CASCADE,
    period_year      INTEGER NOT NULL,
    last_value       INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (document_type_id, period_year)
);

CREATE TABLE document (
    id                  UUID PRIMARY KEY,
    document_number     VARCHAR(150) NOT NULL UNIQUE,
    document_type_id    BIGINT       NOT NULL REFERENCES document_type(id),
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    document_date       DATE,
    status              VARCHAR(20)  NOT NULL,
    original_filename   VARCHAR(255) NOT NULL,
    original_path       VARCHAR(500) NOT NULL,
    original_hash       VARCHAR(64)  NOT NULL,
    current_path        VARCHAR(500) NOT NULL,
    current_hash        VARCHAR(64)  NOT NULL,
    version             INTEGER      NOT NULL DEFAULT 0,
    page_count          INTEGER      NOT NULL DEFAULT 1,
    created_by          BIGINT       NOT NULL REFERENCES app_user(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ
);
CREATE INDEX idx_document_type ON document(document_type_id);
CREATE INDEX idx_document_status ON document(status);
CREATE INDEX idx_document_created_by ON document(created_by);

CREATE TABLE signature (
    id                  UUID PRIMARY KEY,
    document_id         UUID         NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    signer_id           BIGINT       NOT NULL REFERENCES app_user(id),
    sign_order          INTEGER      NOT NULL DEFAULT 1,
    role_label          VARCHAR(100),
    status              VARCHAR(20)  NOT NULL,
    verification_token  VARCHAR(64)  UNIQUE,
    -- snapshot data penandatangan saat menandatangani
    signer_name         VARCHAR(150),
    signer_position     VARCHAR(150),
    signer_department   VARCHAR(150),
    signed_at           TIMESTAMPTZ,
    signed_ip           VARCHAR(64),
    note                TEXT,
    page_number         INTEGER,
    pos_x               DOUBLE PRECISION,
    pos_y               DOUBLE PRECISION,
    qr_size             DOUBLE PRECISION,
    hash_before         VARCHAR(64),
    hash_after          VARCHAR(64),
    seal                VARCHAR(128),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (document_id, signer_id)
);
CREATE INDEX idx_signature_signer ON signature(signer_id, status);

CREATE TABLE app_setting (
    setting_key     VARCHAR(100) PRIMARY KEY,
    setting_value   TEXT
);

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    document_id UUID,
    user_id     BIGINT,
    action      VARCHAR(50)  NOT NULL,
    detail      TEXT,
    ip_address  VARCHAR(64),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_document ON audit_log(document_id);

-- Data awal jenis dokumen (bisa ditambah/diubah dari menu Jenis Dokumen)
INSERT INTO document_type (code, name, description, number_format, color) VALUES
 ('FT',   'Functional Test',             'Dokumen hasil pengujian fungsional aplikasi', '{SEQ:4}/FT/{ROMAN_MONTH}/{YEAR}',   '#1565C0'),
 ('BAST', 'Berita Acara Serah Terima',   'Berita acara serah terima pekerjaan/barang',  '{SEQ:4}/BAST/{ROMAN_MONTH}/{YEAR}', '#2E7D32'),
 ('LOGM', 'Log Modification',            'Catatan modifikasi/perubahan sistem',          '{SEQ:4}/LOGM/{ROMAN_MONTH}/{YEAR}', '#EF6C00');

INSERT INTO app_setting (setting_key, setting_value) VALUES
 ('company.name', 'PT Nama Perusahaan'),
 ('company.address', ''),
 ('qr.logo_ratio', '0.22');
