<template>
  <v-main class="verify-bg">
    <v-container style="max-width: 760px" class="py-6 py-md-10">
      <!-- header perusahaan -->
      <div class="d-flex align-center ga-3 mb-6 text-white">
        <v-avatar size="52" rounded="lg" color="white">
          <v-img v-if="branding.logoUrl" :src="branding.logoUrl" contain />
          <v-icon v-else color="primary">mdi-qrcode-scan</v-icon>
        </v-avatar>
        <div>
          <div class="text-h6 font-weight-bold" style="line-height:1.2">{{ branding.companyName }}</div>
          <div class="text-body-2" style="opacity:.75">Verifikasi Tanda Tangan Digital</div>
        </div>
      </div>

      <v-card v-if="loading" class="pa-10 text-center" rounded="xl">
        <v-progress-circular indeterminate color="primary" size="48" />
        <div class="mt-4 text-medium-emphasis">Memeriksa tanda tangan…</div>
      </v-card>

      <v-card v-else-if="notFound" rounded="xl" class="overflow-hidden">
        <div class="status-band bg-error">
          <v-icon size="56">mdi-shield-alert</v-icon>
          <div class="text-h5 font-weight-bold mt-2">TIDAK TERVERIFIKASI</div>
          <div class="text-body-2 mt-1">{{ notFound }}</div>
        </div>
        <v-card-text class="text-body-2 pa-6">
          QR Code ini tidak dikenali oleh sistem {{ branding.companyName }}. Jangan mempercayai dokumen ini sebelum
          dikonfirmasi langsung ke pihak terkait.
        </v-card-text>
      </v-card>

      <template v-else-if="v">
        <v-card rounded="xl" class="overflow-hidden mb-4">
          <div class="status-band" :class="band.cls">
            <v-icon size="56">{{ band.icon }}</v-icon>
            <div class="text-h5 font-weight-bold mt-2">{{ band.title }}</div>
            <div class="text-body-2 mt-1" style="max-width: 520px">{{ v.message }}</div>
          </div>

          <v-card-text class="pa-6">
            <div class="section-title">Penandatangan (QR ini)</div>
            <div class="d-flex align-center ga-4 mb-6">
              <v-avatar color="primary" size="56">
                <span class="text-h6 text-white">{{ initials(v.signature.name) }}</span>
              </v-avatar>
              <div>
                <div class="text-h6 font-weight-bold" style="line-height:1.2">{{ v.signature.name }}</div>
                <div class="text-body-2 text-medium-emphasis">{{ [v.signature.position, v.signature.department].filter(Boolean).join(' · ') || '-' }}</div>
                <div v-if="v.signature.roleLabel" class="mt-1"><v-chip size="small" label color="secondary" variant="tonal">{{ v.signature.roleLabel }}</v-chip></div>
              </div>
            </div>

            <v-row dense>
              <v-col cols="12" sm="6">
                <div class="label">Tanggal & waktu tanda tangan</div>
                <div class="value">{{ fmtDateTime(v.signature.signedAt) }}</div>
              </v-col>
              <v-col cols="12" sm="6">
                <div class="label">Nomor dokumen</div>
                <div class="value">{{ v.documentNumber }}</div>
              </v-col>
              <v-col cols="12" sm="6">
                <div class="label">Jenis dokumen</div>
                <div class="value">{{ v.documentType }}</div>
              </v-col>
              <v-col cols="12" sm="6">
                <div class="label">Tanggal dokumen</div>
                <div class="value">{{ fmtDate(v.documentDate) }}</div>
              </v-col>
              <v-col cols="12">
                <div class="label">Judul / perihal</div>
                <div class="value">{{ v.documentTitle }}</div>
              </v-col>
              <v-col cols="12" sm="6">
                <div class="label">Status dokumen</div>
                <StatusChip :status="v.documentStatus" class="mt-1" />
              </v-col>
              <v-col cols="12" sm="6">
                <div class="label">Jumlah halaman</div>
                <div class="value">{{ v.pageCount }}</div>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card rounded="xl" class="mb-4">
          <v-card-text class="pa-6">
            <div class="section-title">Seluruh penandatangan dokumen</div>
            <v-list density="compact" class="pa-0">
              <v-list-item v-for="s in v.signers" :key="s.signOrder" class="px-0">
                <template #prepend>
                  <v-icon :color="SIG_STATUS[s.status].color" class="mr-3">{{ SIG_STATUS[s.status].icon }}</v-icon>
                </template>
                <v-list-item-title class="font-weight-medium">
                  {{ s.name }} <span v-if="s.roleLabel" class="text-medium-emphasis font-weight-regular">— {{ s.roleLabel }}</span>
                </v-list-item-title>
                <v-list-item-subtitle>
                  {{ s.position || '' }}
                  <template v-if="s.signedAt"> · {{ fmtDateTime(s.signedAt) }}</template>
                  <template v-else> · {{ SIG_STATUS[s.status].text }}</template>
                </v-list-item-subtitle>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>

        <v-card rounded="xl" class="mb-4">
          <v-card-text class="pa-6">
            <div class="section-title">Cek keaslian file</div>
            <div class="text-body-2 text-medium-emphasis mb-3">
              Punya file PDF dokumen ini? Unggah untuk memastikan isinya sama persis dengan yang tercatat di sistem.
              File hanya dihitung sidik jarinya (SHA-256), tidak disimpan.
            </div>
            <div class="d-flex ga-2 align-start flex-wrap">
              <v-file-input v-model="checkFile" accept="application/pdf" label="Pilih file PDF" prepend-icon="" prepend-inner-icon="mdi-file-pdf-box" hide-details style="min-width: 240px" class="flex-grow-1" />
              <v-btn color="primary" size="large" :loading="checking" :disabled="!pickedFile" @click="check">Cek</v-btn>
            </div>
            <v-alert v-if="checkResult" class="mt-4" :type="checkResult.match ? 'success' : 'error'" variant="tonal">
              {{ checkResult.message }}
              <div class="mono mt-2" style="opacity:.8">SHA-256: {{ checkResult.uploadedHash }}</div>
            </v-alert>
            <v-btn v-if="v.publicDownload && v.documentStatus !== 'CANCELLED'" class="mt-4" variant="tonal" prepend-icon="mdi-download" :href="`/api/public/verify/${token}/file`" target="_blank">
              Lihat dokumen
            </v-btn>
            <div class="mt-4">
              <div class="label">Sidik jari (SHA-256) dokumen versi terakhir</div>
              <div class="mono">{{ v.currentHash }}</div>
            </div>
          </v-card-text>
        </v-card>
      </template>

      <div class="text-center text-caption mt-6 text-medium-emphasis">
        Diverifikasi oleh sistem DigiSign {{ branding.companyName }} · {{ fmtDateTime(now) }}
      </div>
    </v-container>
  </v-main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api, { errorMessage } from '@/api'
import { useBrandingStore } from '@/stores/branding'
import { SIG_STATUS, fmtDate, fmtDateTime } from '@/utils/format'
import StatusChip from '@/components/StatusChip.vue'

const route = useRoute()
const branding = useBrandingStore()
const token = route.params.token
const v = ref(null)
const loading = ref(true)
const notFound = ref('')
const checkFile = ref(null)
const checking = ref(false)
const checkResult = ref(null)
const now = new Date()

const pickedFile = computed(() => (Array.isArray(checkFile.value) ? checkFile.value[0] : checkFile.value))

const band = computed(() => {
  if (!v.value.sealValid) return { cls: 'bg-error', icon: 'mdi-shield-alert', title: 'DATA TIDAK VALID' }
  if (v.value.documentStatus === 'CANCELLED') return { cls: 'bg-grey-darken-1', icon: 'mdi-cancel', title: 'DOKUMEN DIBATALKAN' }
  return { cls: 'bg-success', icon: 'mdi-shield-check', title: 'TANDA TANGAN SAH' }
})

const initials = (n) => (n || '?').split(' ').filter(Boolean).slice(0, 2).map((s) => s[0].toUpperCase()).join('')

onMounted(async () => {
  branding.load()
  try {
    v.value = (await api.get(`/public/verify/${encodeURIComponent(token)}`)).data
    document.title = `Verifikasi ${v.value.documentNumber}`
  } catch (e) {
    notFound.value = await errorMessage(e, 'Tanda tangan tidak ditemukan')
  } finally {
    loading.value = false
  }
})

async function check() {
  checking.value = true
  checkResult.value = null
  try {
    const fd = new FormData()
    fd.append('file', pickedFile.value)
    checkResult.value = (await api.post(`/public/verify/${encodeURIComponent(token)}/check`, fd)).data
  } catch (e) {
    checkResult.value = { match: false, message: await errorMessage(e, 'Gagal memeriksa file'), uploadedHash: '-' }
  } finally {
    checking.value = false
  }
}
</script>

<style scoped>
.verify-bg { background: linear-gradient(180deg, #0F2A4A 0, #0F2A4A 320px, #F4F6F9 320px); min-height: 100vh; }
.status-band { color: #fff; text-align: center; padding: 28px 20px; display: flex; flex-direction: column; align-items: center; }
.section-title { font-weight: 700; color: #0F2A4A; margin-bottom: 12px; font-size: 0.95rem; }
</style>
