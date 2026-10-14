#!/usr/bin/env bash
# Pasang / upgrade DigiSign di server production (bisa tanpa internet).
#   ./install.sh            pasang atau upgrade
set -euo pipefail
cd "$(dirname "$0")"

command -v docker >/dev/null || { echo "docker belum terpasang" >&2; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "plugin 'docker compose' belum terpasang" >&2; exit 1; }
command -v openssl >/dev/null || { echo "openssl dibutuhkan untuk membuat rahasia" >&2; exit 1; }

if [[ -f SHA256SUMS ]]; then
  echo "==> Memeriksa checksum paket"
  sha256sum -c --quiet SHA256SUMS
fi

# ---------- .env ----------
if [[ ! -f .env ]]; then
  cp .env.example .env
  chmod 600 .env
  echo "==> .env dibuat dari .env.example"
fi

# Isi rahasia acak untuk setiap nilai GANTI_OTOMATIS
while IFS= read -r key; do
  val=$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | cut -c1-40)
  sed -i "s|^${key}=GANTI_OTOMATIS$|${key}=${val}|" .env
  echo "    rahasia dibuat: $key"
done < <(grep -E '^[A-Z_]+=GANTI_OTOMATIS$' .env | cut -d= -f1)

# .env berformat compose (bukan shell), jadi jangan di-source.
env_get() { { grep -E "^$1=" .env || true; } | tail -n1 | cut -d= -f2- | sed -E 's/^"(.*)"$/\1/; s/^'"'"'(.*)'"'"'$/\1/'; }
APP_PUBLIC_BASE_URL=$(env_get APP_PUBLIC_BASE_URL)
APP_BIND=$(env_get APP_BIND)
APP_PORT=$(env_get APP_PORT)
APP_ADMIN_USERNAME=$(env_get APP_ADMIN_USERNAME)
APP_ADMIN_PASSWORD=$(env_get APP_ADMIN_PASSWORD)
if [[ -z "${APP_PUBLIC_BASE_URL:-}" || "$APP_PUBLIC_BASE_URL" == */ ]]; then
  echo "Isi APP_PUBLIC_BASE_URL di .env (mis. https://esign.perusahaan.co.id, tanpa '/' di akhir), lalu jalankan ulang." >&2
  exit 1
fi

# APP_TAG mengikuti paket ini
TAG=$(cat VERSION)
sed -i "s|^APP_TAG=.*|APP_TAG=${TAG}|" .env
export APP_TAG=$TAG

# ---------- Image ----------
echo "==> Memuat image"
for f in images/*.tar.gz; do
  echo "    $f"
  gunzip -c "$f" | docker load -q
done

echo "==> Menjalankan stack (postgres, app)"
docker compose up -d --remove-orphans

echo "==> Menunggu aplikasi sehat…"
for i in $(seq 1 60); do
  cid=$(docker compose ps -q app)
  st=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || echo starting)
  if [[ $st == healthy ]]; then
    echo
    echo "OK — DigiSign ${TAG} berjalan di ${APP_BIND:-127.0.0.1}:${APP_PORT:-8080}"
    echo "    Aplikasi   : ${APP_PUBLIC_BASE_URL}/"
    echo "    Verifikasi : ${APP_PUBLIC_BASE_URL}/verify/<token>   (alamat yang tertanam di QR)"
    # Tampilkan akun admin awal hanya pada instalasi pertama yang berhasil
    if [[ ! -f .installed ]]; then
      echo
      echo "    Admin      : ${APP_ADMIN_USERNAME}"
      echo "    Sandi awal : ${APP_ADMIN_PASSWORD}"
      echo "    (Simpan & ganti dari menu profil. Mengubah APP_ADMIN_PASSWORD di .env setelah ini tidak berpengaruh.)"
      date -Iseconds > .installed
    fi
    exit 0
  fi
  printf '\r    belum sehat: app(%s)   ' "$st"
  sleep 5
done
echo
echo "Aplikasi belum sehat. Cek log: docker compose logs --tail=200 app" >&2
exit 1
