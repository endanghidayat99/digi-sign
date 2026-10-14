<template>
  <v-dialog :model-value="modelValue" fullscreen transition="dialog-bottom-transition" @update:model-value="emit('update:modelValue', $event)">
    <v-card color="background">
      <v-toolbar color="primary">
        <v-btn icon="mdi-close" @click="emit('update:modelValue', false)" />
        <v-toolbar-title>Tempatkan QR Tanda Tangan</v-toolbar-title>
        <v-spacer />
        <v-btn variant="flat" color="secondary" class="mr-2" :loading="signing" :disabled="!rendered" prepend-icon="mdi-draw-pen" @click="confirm = true">
          Tandatangani
        </v-btn>
      </v-toolbar>

      <div class="d-flex flex-column flex-md-row" style="height: calc(100vh - 64px)">
        <!-- panel pengaturan -->
        <div class="settings pa-4">
          <div class="label mb-1">Halaman</div>
          <div class="d-flex align-center ga-2 mb-4">
            <v-btn icon="mdi-chevron-left" size="small" variant="tonal" :disabled="page <= 1" @click="goPage(page - 1)" />
            <v-select v-model="page" :items="pageItems" hide-details density="compact" style="max-width: 120px" @update:model-value="goPage" />
            <span class="text-body-2">dari {{ pageCount }}</span>
            <v-btn icon="mdi-chevron-right" size="small" variant="tonal" :disabled="page >= pageCount" @click="goPage(page + 1)" />
          </div>

          <div class="label mb-1">Zoom pratinjau</div>
          <div class="d-flex align-center ga-2 mb-4">
            <v-btn icon="mdi-magnify-minus-outline" size="small" variant="tonal" @click="setZoom(zoom - 0.25)" />
            <v-btn size="small" variant="text" @click="setZoom(1)">{{ Math.round(zoom * 100) }}% (muat)</v-btn>
            <v-btn icon="mdi-magnify-plus-outline" size="small" variant="tonal" @click="setZoom(zoom + 0.25)" />
          </div>

          <div class="label mb-1">Ukuran QR ({{ Math.round(size * 100) }}% lebar halaman)</div>
          <v-slider v-model="size" :min="0.06" :max="0.3" :step="0.005" color="primary" hide-details class="mb-4" @update:model-value="clampPos" />

          <v-switch v-model="caption" label="Tampilkan keterangan di bawah QR" color="primary" hide-details density="compact" @update:model-value="clampPos" />
          <div class="text-caption text-medium-emphasis mb-4">Nama, jabatan & waktu tanda tangan.</div>

          <div class="label mb-2">Posisi cepat</div>
          <div class="d-flex flex-wrap ga-2 mb-4">
            <v-btn size="small" variant="tonal" @click="preset('bl')">Kiri bawah</v-btn>
            <v-btn size="small" variant="tonal" @click="preset('bc')">Tengah bawah</v-btn>
            <v-btn size="small" variant="tonal" @click="preset('br')">Kanan bawah</v-btn>
          </div>

          <v-textarea v-model="note" label="Catatan (opsional)" rows="2" auto-grow />

          <v-alert type="info" variant="tonal" density="compact" class="text-caption">
            Seret kotak QR ke area kosong dokumen (mis. kolom tanda tangan). QR berisi tautan verifikasi yang dapat dipindai siapa saja.
          </v-alert>
        </div>

        <!-- pratinjau halaman -->
        <div ref="viewport" class="viewport flex-grow-1 pa-4">
          <div v-if="loadingPdf" class="d-flex justify-center align-center fill-height">
            <v-progress-circular indeterminate color="primary" size="48" />
          </div>
          <div v-show="!loadingPdf" ref="stage" class="stage" :style="{ width: canvasW + 'px', height: canvasH + 'px' }">
            <canvas ref="canvas" />
            <div
              class="qr-box"
              :style="boxStyle"
              @pointerdown="startDrag"
            >
              <div class="qr-face" :style="{ height: qrPx + 'px' }">
                <v-icon :size="Math.max(16, qrPx * 0.45)" color="primary">mdi-qrcode</v-icon>
              </div>
              <div v-if="caption" class="qr-caption" :style="{ height: captionPx + 'px', fontSize: Math.max(7, qrPx * 0.075) + 'px' }">
                <div>Ditandatangani secara digital oleh</div>
                <div class="font-weight-bold">{{ signerName }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </v-card>

    <v-dialog v-model="confirm" max-width="460">
      <v-card>
        <v-card-title class="pt-4">Konfirmasi Tanda Tangan</v-card-title>
        <v-card-text>
          Dengan menekan <b>Ya, Tandatangani</b>, Anda menyatakan telah membaca dan menyetujui isi dokumen ini.
          QR Code akan ditempel di halaman {{ page }} dan tidak dapat dibatalkan.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="confirm = false">Batal</v-btn>
          <v-btn color="primary" variant="flat" :loading="signing" @click="doSign">Ya, Tandatangani</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import * as pdfjsLib from 'pdfjs-dist'
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import api, { errorMessage } from '@/api'

pdfjsLib.GlobalWorkerOptions.workerSrc = workerUrl

const props = defineProps({
  modelValue: Boolean,
  documentId: String,
  pageCount: { type: Number, default: 1 },
  signerName: String
})
const emit = defineEmits(['update:modelValue', 'signed', 'error'])

const canvas = ref()
const viewport = ref()
const stage = ref()
const page = ref(props.pageCount)
const size = ref(0.14)
const caption = ref(true)
const note = ref('')
const pos = ref({ x: 0, y: 0 }) // relatif 0..1 kiri-atas
const canvasW = ref(0)
const canvasH = ref(0)
const loadingPdf = ref(false)
const rendered = ref(false)
const signing = ref(false)
const confirm = ref(false)
const zoom = ref(1)

function setZoom(z) {
  zoom.value = Math.min(3, Math.max(0.5, z))
  renderPage(false)
}
let pdfDoc = null
let renderTask = null

const pageItems = computed(() => Array.from({ length: props.pageCount }, (_, i) => i + 1))
const qrPx = computed(() => size.value * canvasW.value)
// Sama dengan perhitungan backend: 4 baris x fontSize(7.5% QR) x 1.25
const captionPx = computed(() => (caption.value ? qrPx.value * 0.075 * 4 * 1.25 + 2 : 0))
const boxStyle = computed(() => ({
  left: pos.value.x * canvasW.value + 'px',
  top: pos.value.y * canvasH.value + 'px',
  width: qrPx.value + 'px',
  height: qrPx.value + captionPx.value + 'px'
}))

watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      page.value = props.pageCount
      await nextTick()
      await loadPdf()
    }
  }
)

async function loadPdf() {
  loadingPdf.value = true
  rendered.value = false
  try {
    const res = await api.get(`/documents/${props.documentId}/file`, { responseType: 'arraybuffer' })
    pdfDoc = await pdfjsLib.getDocument({ data: new Uint8Array(res.data) }).promise
    await renderPage(true)
  } catch (e) {
    emit('error', await errorMessage(e, 'Gagal memuat PDF'))
  } finally {
    loadingPdf.value = false
  }
}

async function renderPage(resetPos = false) {
  if (!pdfDoc) return
  const p = await pdfDoc.getPage(page.value)
  const base = p.getViewport({ scale: 1 })
  const availW = Math.max(300, (viewport.value?.clientWidth || 800) - 32)
  const availH = Math.max(400, (viewport.value?.clientHeight || 900) - 32)
  // zoom 1 = seluruh halaman muat di layar
  const fit = Math.min(availW / base.width, availH / base.height)
  const scale = Math.min(fit * zoom.value, 3)
  const vp = p.getViewport({ scale })
  const ratio = window.devicePixelRatio || 1
  const c = canvas.value
  c.width = Math.floor(vp.width * ratio)
  c.height = Math.floor(vp.height * ratio)
  c.style.width = vp.width + 'px'
  c.style.height = vp.height + 'px'
  canvasW.value = vp.width
  canvasH.value = vp.height
  if (renderTask) renderTask.cancel()
  renderTask = p.render({ canvasContext: c.getContext('2d'), viewport: vp, transform: ratio !== 1 ? [ratio, 0, 0, ratio, 0, 0] : undefined })
  try {
    await renderTask.promise
  } catch {
    return
  }
  rendered.value = true
  if (resetPos) preset('br')
  else clampPos()
}

function goPage(n) {
  page.value = n
  renderPage(false)
}

function preset(where) {
  const m = 0.06
  const boxH = (qrPx.value + captionPx.value) / canvasH.value
  const y = 1 - boxH - m
  if (where === 'bl') pos.value = { x: m, y }
  if (where === 'bc') pos.value = { x: (1 - size.value) / 2, y }
  if (where === 'br') pos.value = { x: 1 - size.value - m, y }
  clampPos()
}

function clampPos() {
  if (!canvasH.value) return
  const boxH = (qrPx.value + captionPx.value) / canvasH.value
  pos.value = {
    x: Math.min(Math.max(0, pos.value.x), 1 - size.value),
    y: Math.min(Math.max(0, pos.value.y), Math.max(0, 1 - boxH))
  }
}

let drag = null
function startDrag(e) {
  e.preventDefault()
  drag = { sx: e.clientX, sy: e.clientY, ox: pos.value.x, oy: pos.value.y }
  window.addEventListener('pointermove', onDrag)
  window.addEventListener('pointerup', endDrag)
}
function onDrag(e) {
  if (!drag) return
  pos.value = { x: drag.ox + (e.clientX - drag.sx) / canvasW.value, y: drag.oy + (e.clientY - drag.sy) / canvasH.value }
  clampPos()
}
function endDrag() {
  drag = null
  window.removeEventListener('pointermove', onDrag)
  window.removeEventListener('pointerup', endDrag)
}
onBeforeUnmount(endDrag)

async function doSign() {
  signing.value = true
  try {
    const { data } = await api.post(`/documents/${props.documentId}/sign`, {
      page: page.value,
      x: +pos.value.x.toFixed(5),
      y: +pos.value.y.toFixed(5),
      size: +size.value.toFixed(4),
      caption: caption.value,
      note: note.value || null
    })
    confirm.value = false
    emit('signed', data)
    emit('update:modelValue', false)
  } catch (e) {
    confirm.value = false
    emit('error', await errorMessage(e, 'Gagal menandatangani'))
  } finally {
    signing.value = false
  }
}
</script>

<style scoped>
.settings { width: 100%; max-width: 340px; background: #fff; border-right: 1px solid #E3E8EF; overflow-y: auto; }
.viewport { overflow: auto; background: #DDE3EA; }
.stage { position: relative; margin: 0 auto; box-shadow: 0 6px 24px rgba(15, 42, 74, 0.25); background: #fff; }
.stage canvas { display: block; }
.qr-box {
  position: absolute; cursor: move; touch-action: none; user-select: none;
  outline: 2px dashed #2EC4B6; outline-offset: 2px; background: rgba(46, 196, 182, 0.12);
}
.qr-face { display: flex; align-items: center; justify-content: center; background: rgba(255, 255, 255, 0.85); }
.qr-caption { text-align: center; line-height: 1.25; color: #1a1a1a; overflow: hidden; background: rgba(255, 255, 255, 0.85); }
@media (max-width: 960px) { .settings { max-width: none; border-right: 0; border-bottom: 1px solid #E3E8EF; max-height: 40vh; } }
</style>
