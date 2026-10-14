# DigiSign — Tanda Tangan Digital Dokumen Internal

Aplikasi internal untuk menandatangani dokumen (Functional Test, Berita Acara Serah Terima, Log Modification, dan jenis lain yang bisa ditambah sendiri) dengan **QR Code berlogo perusahaan**. Saat QR dipindai, terbuka halaman web publik yang menampilkan detail verifikasi: nomor dokumen, jenis dokumen, tanggal & waktu tanda tangan, dan siapa penandatangannya.

| Lapisan  | Teknologi |
|----------|-----------|
| Backend  | Spring Boot 3.5 (Java 21), Spring Security + JWT, Spring Data JPA, Flyway |
| PDF & QR | Apache PDFBox 3, ZXing (Error Correction **H** agar tetap terbaca dengan logo) |
| Frontend | Vue 3 + Vuetify 3 + Pinia + pdf.js (pratinjau & penempatan QR) |
| Database | PostgreSQL |
| Deploy   | Satu image Docker (frontend disajikan oleh Spring Boot) |

---

## Alur kerja

```
Pengaju                          Penandatangan (1..n)                 Siapa pun (scan QR)
  │ unggah PDF, pilih jenis,        │                                      │
  │ pilih penandatangan  ─────────► │ lihat "Menunggu Tanda Tangan Saya"   │
  │                                 │ seret kotak QR ke posisi TTD         │
  │                                 │ ► QR + keterangan ditempel ke PDF    │
  │                                 │ ► versi PDF baru + SHA-256 dicatat   │
  │                                 │ ► segel HMAC dibuat                  │
  │                                 │                                      │
  │      semua sudah TTD ⇒ status SELESAI                                  │
  │                                                     scan ─────────────►│ /verify/{token}
  │                                                                        │ ✔ Tanda tangan SAH
  │                                                                        │ No, jenis, tanggal, penandatangan
  │                                                                        │ (opsional) unggah PDF ⇒ cek keaslian
```

## Fitur

- **Jenis dokumen dinamis** — admin menambah/ubah jenis dokumen, warna label, dan format penomoran
  (`{SEQ:4}/{CODE}/{ROMAN_MONTH}/{YEAR}` → `0007/BAST/X/2026`, nomor urut reset tiap tahun). Nomor juga bisa diisi manual.
- **Multi penandatangan** dengan peran (Pembuat, Pemeriksa, Pihak Pertama, Mengetahui, …). Setiap penandatangan menempelkan QR miliknya sendiri.
- **Penempatan QR visual** — halaman PDF dirender di browser, penandatangan menyeret kotak QR ke kolom tanda tangan, memilih halaman & ukuran. Mendukung halaman yang dirotasi.
- **QR dengan logo perusahaan di tengah** — logo diunggah dari menu *Pengaturan*, ukuran logo bisa diatur, ada pratinjau QR untuk dites pindai.
- **Halaman verifikasi publik** (tanpa login): status sah/tidak, data penandatangan (di-*snapshot* saat TTD sehingga tidak berubah walau jabatan user diganti), daftar seluruh penandatangan, status dokumen.
- **Cek keaslian file** — siapa pun dapat mengunggah PDF di halaman verifikasi; sistem mencocokkan SHA-256 dengan versi terakhir / versi setelah tiap TTD / versi asli. File tidak disimpan.
- **Segel HMAC-SHA256** per tanda tangan untuk mendeteksi perubahan data langsung di database.
- Tolak dokumen (dengan alasan), batalkan dokumen (QR lama akan tampil "DIBATALKAN"), riwayat audit, dashboard, manajemen user & role (ADMIN/USER), ganti password.

---

## Menjalankan lokal dengan Docker

```bash
cp .env.example .env          # lalu isi password & secret
docker compose up -d --build  # build image + jalankan Postgres & aplikasi
```

Buka `http://localhost:8080`, login dengan `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`, lalu:
1. **Pengaturan & Logo QR** → isi nama perusahaan, unggah logo (PNG transparan, persegi), cek pratinjau QR dengan kamera HP.
2. **Pengguna** → tambahkan pegawai beserta **nama lengkap & jabatan** (inilah yang tercetak di QR).
3. **Jenis Dokumen** → sesuaikan/tambah jenis dokumen.
4. Ganti password admin dari menu profil.

## Build & deploy production

```bash
./build.sh                 # image digisign + postgres → dist/digisign_<versi>_deploy.tar.gz (versi dari file VERSION)
./build.sh 1.1.0           # versi ditentukan saat build
./build.sh --app-only      # upgrade: hanya image aplikasi (tanpa postgres)
PLATFORM=linux/arm64 ./build.sh   # server ARM (default linux/amd64)
```

Di server (tanpa internet pun bisa, cukup Docker + compose plugin + openssl):

```bash
# salin dist/digisign_1.0.0_deploy.tar.gz ke server, mis. /opt/
cd /opt && tar xzf digisign_1.0.0_deploy.tar.gz && cd digisign_1.0.0
sudo ./install.sh     # pertama kali: membuat .env + semua rahasia acak, lalu minta APP_PUBLIC_BASE_URL
vi .env               # APP_PUBLIC_BASE_URL=https://esign.perusahaan.co.id
sudo ./install.sh     # cek checksum → docker load → compose up → tunggu sehat → tampilkan sandi admin
```

`install.sh` otomatis:
- mengisi semua nilai `GANTI_OTOMATIS` di `.env` dengan rahasia acak (sandi DB, admin, JWT, segel tanda tangan);
- menampilkan username & sandi admin awal pada instalasi pertama.

Isi paket: `images/` (digisign + postgres), `docker-compose.yml`, `.env.example`, `install.sh`, `VERSION`, `SHA256SUMS`.
Sumbernya ada di folder `deploy/`.

**Upgrade**: extract paket baru, salin `.env` (dan `.installed`) dari folder versi lama, jalankan `./install.sh`.
Data tetap aman karena volume compose project `digisign` sama.

**Backup**: volume `digisign_pgdata` (database) dan `digisign_appdata` (file PDF & logo), serta file `.env` —
terutama `APP_SIGNATURE_SECRET` (hilang/berubah = segel tanda tangan lama tidak sah).

> **Penting:** `APP_PUBLIC_BASE_URL` harus alamat yang bisa dibuka oleh HP yang memindai QR (domain publik, atau alamat intranet jika verifikasi hanya internal). Nilai ini **tertanam permanen di QR**, jadi tentukan sebelum mulai menandatangani dokumen.

---

## Konfigurasi (environment variable)

| Variabel | Default | Keterangan |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/digisign` | JDBC URL PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | `digisign` | Kredensial DB |
| `APP_PUBLIC_BASE_URL` | `http://localhost:8080` | Basis URL di dalam QR → `{url}/verify/{token}` |
| `APP_STORAGE_DIR` | `/data/storage` (Docker) | Lokasi file PDF & logo — **wajib volume** |
| `APP_JWT_SECRET` | (contoh) | Kunci token login, ≥ 32 karakter |
| `APP_SIGNATURE_SECRET` | (contoh) | Kunci segel HMAC. **Jangan diubah** setelah ada TTD (segel lama jadi tidak valid) |
| `APP_JWT_EXPIRATION_MINUTES` | `480` | Masa berlaku login |
| `APP_ADMIN_USERNAME` / `_PASSWORD` / `_FULLNAME` | `admin` / `admin123` | Admin awal, dibuat hanya jika tabel user kosong |
| `APP_PUBLIC_DOWNLOAD` | `false` | Izinkan PDF dibuka dari halaman verifikasi publik |
| `APP_TIMEZONE` | `Asia/Makassar` | Zona waktu penomoran & keterangan di QR |
| `MAX_UPLOAD_SIZE` | `25MB` | Batas ukuran PDF |

Di belakang reverse proxy (Nginx/Traefik) pastikan header `X-Forwarded-For` / `X-Forwarded-Proto` diteruskan; aplikasi sudah memakai `forward-headers-strategy: framework`.

---

## Pengembangan lokal

```bash
# Postgres
docker run -d --name pg -e POSTGRES_DB=digisign -e POSTGRES_USER=digisign -e POSTGRES_PASSWORD=digisign -p 5432:5432 postgres:16-alpine

# Backend  (http://localhost:8080)
cd backend && mvn spring-boot:run

# Frontend (http://localhost:5173, /api diproxy ke 8080)
cd frontend && npm install && npm run dev
```

## Struktur proyek

```
digisign/
├── Dockerfile                 multi-stage: node → maven → JRE
├── VERSION                    versi paket deploy
├── build.sh                   build image + paket deploy offline
├── deploy/                    docker-compose production, .env.example, install.sh
├── docker-compose.yml         app + postgres (lokal)
├── backend/
│   └── src/main/java/com/digisign/
│       ├── config/            security, properties, SPA forward, admin awal
│       ├── domain/            AppUser, DocumentType, Document, Signature, AppSetting, AuditLog
│       ├── repository/        JPA + nomor urut atomik (UPSERT)
│       ├── security/          JWT
│       ├── service/           DocumentService, QrCodeService, PdfStampService, VerificationService, ...
│       └── web/               REST controller + DTO
│   └── src/main/resources/db/migration/V1__init.sql
└── frontend/src/
    ├── views/                 Login, Dashboard, Dokumen, Ajukan, Detail, Jenis Dokumen, Pengguna, Pengaturan, Verify
    └── components/QrPlacementDialog.vue   render PDF (pdf.js) + drag posisi QR
```

## API ringkas

| Method | Endpoint | Akses |
|---|---|---|
| POST | `/api/auth/login` | publik |
| GET/PUT | `/api/auth/me`, POST `/api/auth/change-password` | login |
| GET | `/api/documents?q=&typeId=&status=&all=&page=&size=` | login |
| POST | `/api/documents` (multipart: `data` JSON + `file` PDF) | login |
| GET | `/api/documents/{id}`, `/api/documents/{id}/file?original=` | pengaju / penandatangan / admin |
| POST | `/api/documents/{id}/sign` `{page,x,y,size,caption,note}` | penandatangan |
| POST | `/api/documents/{id}/reject`, `/cancel` | penandatangan / pengaju |
| GET | `/api/documents/pending`, `/api/documents/dashboard` | login |
| CRUD | `/api/document-types`, `/api/users`, `/api/settings`, `/api/settings/logo` | admin |
| GET | `/api/public/verify/{token}` | publik |
| POST | `/api/public/verify/{token}/check` (multipart `file`) | publik |
| GET | `/api/public/branding`, `/api/public/branding/logo` | publik |

## Keamanan & catatan hukum

- Token verifikasi acak 144-bit (tidak bisa ditebak/di-enumerasi), sehingga QR tidak membocorkan dokumen lain.
- Siapa pun bisa membuat QR palsu yang mengarah ke situs lain. Edukasi pemeriksa dokumen untuk **memastikan domain** di browser adalah `APP_PUBLIC_BASE_URL` resmi perusahaan.
- Tanda tangan ini adalah **tanda tangan elektronik tidak tersertifikasi** (bukti persetujuan internal + jejak audit). Untuk dokumen yang butuh kekuatan hukum penuh sebagai tanda tangan elektronik tersertifikasi menurut UU ITE/PP 71/2019, perlu integrasi dengan PSrE berinduk (mis. BSrE, Privy, Peruri) — arsitektur ini bisa diperluas dengan menambah tanda tangan PAdES setelah penempelan QR.
- Backup rutin dua hal: database Postgres **dan** volume `/data` (file PDF). Keduanya saling bergantung.
