<template>
  <v-progress-linear v-if="!doc && !loadError" indeterminate color="primary" />
  <v-alert v-else-if="loadError" type="error" variant="tonal">{{ loadError }}</v-alert>

  <template v-else>
    <div class="d-flex align-start flex-wrap ga-3 mb-6">
      <v-btn icon="mdi-arrow-left" variant="text" to="/documents" />
      <div class="flex-grow-1">
        <div class="d-flex align-center ga-2 flex-wrap">
          <TypeChip :type="doc.type" />
          <StatusChip :status="doc.status" />
        </div>
        <div class="page-title mt-1">{{ doc.title }}</div>
        <div class="page-subtitle">{{ doc.type.name }} · No. <b>{{ doc.documentNumber }}</b></div>
      </div>
      <div class="d-flex ga-2 flex-wrap">
        <v-btn v-if="doc.canSign" color="primary" prepend-icon="mdi-draw-pen" @click="placement = true">Tandatangani</v-btn>
        <v-btn v-if="doc.canSign" color="error" variant="tonal" prepend-icon="mdi-close" @click="rejectDialog = true">Tolak</v-btn>
        <v-menu>
          <template #activator="{ props }">
            <v-btn v-bind="props" variant="tonal" prepend-icon="mdi-download">Unduh</v-btn>
          </template>
          <v-list density="compact">
            <v-list-item prepend-icon="mdi-file-check-outline" :title="doc.version > 0 ? 'PDF bertanda tangan (terbaru)' : 'PDF'" @click="download(false)" />
            <v-list-item prepend-icon="mdi-file-outline" title="PDF asli (sebelum TTD)" @click="download(true)" />
          </v-list>
        </v-menu>
        <v-btn v-if="doc.canManage && doc.status === 'IN_PROGRESS'" variant="text" color="grey-darken-1" prepend-icon="mdi-cancel" @click="cancelDialog = true">Batalkan</v-btn>
      </div>
    </div>

    <v-row>
      <v-col cols="12" lg="7">
        <v-card class="card-border" flat>
          <div class="d-flex align-center px-4 py-2">
            <span class="text-body-2 text-medium-emphasis">Pratinjau versi {{ doc.version }} · {{ doc.pageCount }} halaman</span>
          </div>
          <iframe v-if="pdfUrl" :src="pdfUrl" class="pdf-frame" title="Pratinjau PDF" />
          <div v-else class="pdf-frame d-flex align-center justify-center"><v-progress-circular indeterminate /></div>
        </v-card>
      </v-col>

      <v-col cols="12" lg="5">
        <v-card class="card-border mb-4" flat>
          <v-card-title class="pt-4">Penandatangan</v-card-title>
          <v-card-text>
            <v-timeline side="end" density="compact" align="start" truncate-line="both">
              <v-timeline-item
                v-for="s in doc.signatures"
                :key="s.id"
                :dot-color="SIG_STATUS[s.status].color"
                :icon="SIG_STATUS[s.status].icon"
                size="small"
              >
                <div class="d-flex align-center flex-wrap ga-2">
                  <span class="font-weight-medium">{{ s.signerName }}</span>
                  <v-chip v-if="s.roleLabel" size="x-small" label>{{ s.roleLabel }}</v-chip>
                  <StatusChip :status="s.status" kind="sig" />
                </div>
                <div class="text-caption text-medium-emphasis">{{ [s.signerPosition, s.signerDepartment].filter(Boolean).join(' · ') }}</div>
                <div v-if="s.signedAt" class="text-caption mt-1">
                  <v-icon size="14">mdi-clock-outline</v-icon> {{ fmtDateTime(s.signedAt) }} · hal. {{ s.pageNumber }}
                </div>
                <div v-if="s.note" class="text-caption mt-1 font-italic">“{{ s.note }}”</div>
                <div v-if="s.verificationUrl" class="mt-1">
                  <v-btn size="x-small" variant="text" color="primary" prepend-icon="mdi-open-in-new" :href="s.verificationUrl" target="_blank" class="px-1">
                    Halaman verifikasi
                  </v-btn>
                  <v-btn size="x-small" variant="text" prepend-icon="mdi-content-copy" class="px-1" @click="copy(s.verificationUrl)">Salin tautan</v-btn>
                </div>
              </v-timeline-item>
            </v-timeline>
          </v-card-text>
        </v-card>

        <v-card class="card-border" flat>
          <v-card-title class="pt-4">Detail Dokumen</v-card-title>
          <v-card-text>
            <v-row dense>
              <v-col cols="6"><div class="label">Nomor</div><div class="value">{{ doc.documentNumber }}</div></v-col>
              <v-col cols="6"><div class="label">Tanggal dokumen</div><div class="value">{{ fmtDate(doc.documentDate) }}</div></v-col>
              <v-col cols="6"><div class="label">Diajukan oleh</div><div class="value">{{ doc.createdByName }}</div></v-col>
              <v-col cols="6"><div class="label">Diajukan</div><div class="value">{{ fmtDateTime(doc.createdAt) }}</div></v-col>
              <v-col v-if="doc.completedAt" cols="12"><div class="label">Selesai</div><div class="value">{{ fmtDateTime(doc.completedAt) }}</div></v-col>
              <v-col cols="12"><div class="label">Nama file</div><div class="value">{{ doc.originalFilename }}</div></v-col>
              <v-col v-if="doc.description" cols="12"><div class="label">Keterangan</div><div class="value" style="white-space: pre-line">{{ doc.description }}</div></v-col>
              <v-col cols="12"><div class="label">SHA-256 versi terbaru</div><div class="mono">{{ doc.currentHash }}</div></v-col>
              <v-col cols="12"><div class="label">SHA-256 file asli</div><div class="mono">{{ doc.originalHash }}</div></v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <QrPlacementDialog
      v-model="placement"
      :document-id="doc.id"
      :page-count="doc.pageCount"
      :signer-name="auth.user?.fullName"
      @signed="onSigned"
      @error="(m) => notify(m, 'error')"
    />

    <v-dialog v-model="rejectDialog" max-width="480">
      <v-card>
        <v-card-title class="pt-4">Tolak Dokumen</v-card-title>
        <v-card-text>
          <v-textarea v-model="rejectNote" label="Alasan penolakan *" rows="3" auto-grow />
          <div class="text-caption text-medium-emphasis">Dokumen akan berstatus Ditolak dan proses tanda tangan berhenti.</div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="rejectDialog = false">Batal</v-btn>
          <v-btn color="error" variant="flat" :disabled="!rejectNote.trim()" :loading="busy" @click="reject">Tolak</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="cancelDialog" max-width="440">
      <v-card>
        <v-card-title class="pt-4">Batalkan Dokumen?</v-card-title>
        <v-card-text>Tanda tangan yang sudah ada akan ditampilkan sebagai <b>dibatalkan</b> di halaman verifikasi.</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="cancelDialog = false">Tidak</v-btn>
          <v-btn color="error" variant="flat" :loading="busy" @click="cancelDoc">Ya, batalkan</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </template>
</template>

<script setup>
import { inject, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api, { downloadFile, errorMessage } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { SIG_STATUS, fmtDate, fmtDateTime } from '@/utils/format'
import StatusChip from '@/components/StatusChip.vue'
import TypeChip from '@/components/TypeChip.vue'
import QrPlacementDialog from '@/components/QrPlacementDialog.vue'

const route = useRoute()
const auth = useAuthStore()
const notify = inject('notify')
const doc = ref(null)
const loadError = ref('')
const pdfUrl = ref(null)
const placement = ref(false)
const rejectDialog = ref(false)
const rejectNote = ref('')
const cancelDialog = ref(false)
const busy = ref(false)

async function load() {
  try {
    doc.value = (await api.get(`/documents/${route.params.id}`)).data
    loadPreview()
  } catch (e) {
    loadError.value = await errorMessage(e, 'Dokumen tidak dapat dimuat')
  }
}

async function loadPreview() {
  const res = await api.get(`/documents/${doc.value.id}/file`, { responseType: 'blob' })
  if (pdfUrl.value) URL.revokeObjectURL(pdfUrl.value)
  pdfUrl.value = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
}

function download(original) {
  const name = doc.value.documentNumber.replace(/[^A-Za-z0-9._-]+/g, '_') + (original ? '_original.pdf' : '.pdf')
  downloadFile(`/documents/${doc.value.id}/file?original=${original}&download=true`, name).catch(async (e) =>
    notify(await errorMessage(e), 'error')
  )
}

function onSigned(d) {
  doc.value = d
  loadPreview()
  notify('Dokumen berhasil ditandatangani')
}

async function reject() {
  busy.value = true
  try {
    doc.value = (await api.post(`/documents/${doc.value.id}/reject`, { note: rejectNote.value })).data
    rejectDialog.value = false
    notify('Dokumen ditolak', 'warning')
  } catch (e) {
    notify(await errorMessage(e), 'error')
  } finally {
    busy.value = false
  }
}

async function cancelDoc() {
  busy.value = true
  try {
    doc.value = (await api.post(`/documents/${doc.value.id}/cancel`)).data
    cancelDialog.value = false
    notify('Dokumen dibatalkan', 'warning')
  } catch (e) {
    notify(await errorMessage(e), 'error')
  } finally {
    busy.value = false
  }
}

async function copy(text) {
  await navigator.clipboard?.writeText(text)
  notify('Tautan disalin')
}

onMounted(load)
onBeforeUnmount(() => pdfUrl.value && URL.revokeObjectURL(pdfUrl.value))
</script>

<style scoped>
.pdf-frame { width: 100%; height: 78vh; border: 0; background: #E9EDF2; }
</style>
