#!/usr/bin/env bash
# =============================================================================
#  Build image DigiSign + paket deploy siap salin ke server production
#  (termasuk server tanpa internet).
#
#  Pemakaian:
#    ./build.sh                 # image digisign + postgres → paket deploy (versi dari file VERSION)
#    ./build.sh 1.1.0           # versi ditentukan saat build (menimpa file VERSION untuk build ini)
#    ./build.sh --app-only      # upgrade: hanya image aplikasi (postgres sudah ada di server)
#    PLATFORM=linux/arm64 ./build.sh   # arsitektur server lain (default linux/amd64)
#
#  Hasil:  dist/digisign_<versi>_deploy.tar.gz
#          → salin ke server, extract, jalankan ./install.sh
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")"

PLATFORM=${PLATFORM:-linux/amd64}
POSTGRES_IMAGE=${POSTGRES_IMAGE:-postgres:16-alpine}
WITH_DEPS=1
VERSION=$(cat VERSION)

for arg in "$@"; do
  case "$arg" in
    --app-only) WITH_DEPS=0 ;;
    -h|--help)  sed -n '2,14p' "$0"; exit 0 ;;
    -*) echo "Argumen tidak dikenal: $arg" >&2; exit 1 ;;
    *)  VERSION=$arg ;;
  esac
done
[[ $VERSION =~ ^[A-Za-z0-9._-]+$ ]] || { echo "Versi tidak valid: '$VERSION'" >&2; exit 1; }

command -v docker >/dev/null || { echo "docker tidak ditemukan" >&2; exit 1; }

GIT_SHA=$(git rev-parse --short HEAD 2>/dev/null || true)
TAG="${VERSION}${GIT_SHA:+-$GIT_SHA}"

echo "==> Versi    : $TAG"
echo "==> Platform : $PLATFORM"

echo "==> Build image digisign:$TAG"
DOCKER_BUILDKIT=1 docker build --platform "$PLATFORM" --build-arg APP_VERSION="$TAG" \
  -t "digisign:$TAG" -t "digisign:latest" .

PKG="digisign_${TAG}"
DIST="dist/$PKG"
rm -rf "$DIST"
mkdir -p "$DIST/images"

echo "==> Export digisign"
docker save "digisign:$TAG" | gzip -9 > "$DIST/images/digisign_${TAG}.tar.gz"

if [[ $WITH_DEPS == 1 ]]; then
  echo "==> Export $POSTGRES_IMAGE"
  docker pull --platform "$PLATFORM" "$POSTGRES_IMAGE"
  docker save --platform "$PLATFORM" "$POSTGRES_IMAGE" | gzip -9 > "$DIST/images/$(echo "$POSTGRES_IMAGE" | tr ':/' '__').tar.gz"
fi

echo "==> Menyusun paket deploy"
cp deploy/docker-compose.yml deploy/install.sh deploy/.env.example "$DIST/"
sed -i.bak -e "s|^APP_TAG=.*|APP_TAG=${TAG}|" \
           -e "s|^POSTGRES_IMAGE=.*|POSTGRES_IMAGE=${POSTGRES_IMAGE}|" "$DIST/.env.example" && rm -f "$DIST/.env.example.bak"
chmod +x "$DIST/install.sh"
echo "$TAG" > "$DIST/VERSION"
( cd "$DIST" && find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS )

tar -C dist -czf "dist/${PKG}_deploy.tar.gz" "$PKG"

echo
echo "Selesai:"
ls -lh "dist/${PKG}_deploy.tar.gz"
cat <<MSG

Langkah di server production:
  salin dist/${PKG}_deploy.tar.gz ke server, mis. ke /opt/
  cd /opt && tar xzf ${PKG}_deploy.tar.gz && cd ${PKG}
  # upgrade? salin .env & .installed dari folder versi lama:  cp -a ../<versi-lama>/{.env,.installed} .
  sudo ./install.sh          # pertama kali: membuat .env + rahasia acak, lalu minta isi APP_PUBLIC_BASE_URL
  vi .env                    # isi APP_PUBLIC_BASE_URL=https://domain-anda
  sudo ./install.sh          # jalankan lagi
MSG
