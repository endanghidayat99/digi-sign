<template>
  <div class="mb-6">
    <div class="page-title">Pengaturan & Logo QR</div>
    <div class="page-subtitle">Identitas perusahaan yang tampil di halaman verifikasi dan logo di tengah QR Code.</div>
  </div>

  <v-row>
    <v-col cols="12" md="7">
      <v-card class="card-border mb-4" flat>
        <v-card-title class="pt-4">Identitas Perusahaan</v-card-title>
        <v-card-text>
          <v-text-field v-model="values['company.name']" label="Nama perusahaan" />
          <v-textarea v-model="values['company.address']" label="Alamat" rows="2" auto-grow />
          <div class="label mb-1">Ukuran logo di tengah QR ({{ Math.round(ratio * 100) }}% dari QR)</div>
          <v-slider v-model="ratio" :min="0.1" :max="0.28" :step="0.01" color="primary" hide-details />
          <div class="text-caption text-medium-emphasis mb-4">
            QR memakai koreksi galat level H (±30%). Logo terlalu besar dapat membuat QR sulit dipindai — disarankan 0.18–0.24.
          </div>
          <v-btn color="primary" :loading="saving" prepend-icon="mdi-content-save" @click="save">Simpan</v-btn>
        </v-card-text>
      </v-card>

      <v-card class="card-border" flat>
        <v-card-title class="pt-4">Logo Perusahaan</v-card-title>
        <v-card-text>
          <div class="d-flex align-center ga-4 mb-4">
            <div class="logo-box">
              <img v-if="hasLogo" :src="`/api/public/branding/logo?v=${ver}`" alt="Logo" />
              <span v-else class="text-caption text-medium-emphasis">Belum ada logo</span>
            </div>
            <div class="text-body-2 text-medium-emphasis">
              Gunakan PNG berlatar transparan, bentuk persegi, minimal 256×256 px, maksimal 2MB.
            </div>
          </div>
          <div class="d-flex ga-2 flex-wrap align-start">
            <v-file-input v-model="logoFile" accept="image/png,image/jpeg" label="Pilih gambar logo" prepend-icon="" prepend-inner-icon="mdi-image-outline" hide-details style="min-width: 240px" class="flex-grow-1" />
            <v-btn color="primary" size="large" :disabled="!pickedLogo" :loading="uploading" @click="upload">Unggah</v-btn>
            <v-btn v-if="hasLogo" variant="text" color="error" size="large" @click="removeLogo">Hapus</v-btn>
          </div>
        </v-card-text>
      </v-card>
    </v-col>

    <v-col cols="12" md="5">
      <v-card class="card-border" flat>
        <v-card-title class="pt-4">Pratinjau QR Code</v-card-title>
        <v-card-text class="text-center">
          <img :src="previewUrl" alt="Pratinjau QR" class="qr-preview" />
          <div class="text-caption text-medium-emphasis mt-2">Coba pindai dengan kamera HP untuk memastikan QR terbaca.</div>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref } from 'vue'
import api, { errorMessage } from '@/api'
import { useBrandingStore } from '@/stores/branding'

const notify = inject('notify')
const branding = useBrandingStore()
const values = ref({})
const ratio = ref(0.22)
const hasLogo = ref(false)
const saving = ref(false)
const uploading = ref(false)
const logoFile = ref(null)
const previewUrl = ref('')
const ver = ref(Date.now())
const pickedLogo = computed(() => (Array.isArray(logoFile.value) ? logoFile.value[0] : logoFile.value))

function apply(data) {
  values.value = { ...data.values }
  ratio.value = parseFloat(data.values['qr.logo_ratio'] || '0.22')
  hasLogo.value = data.hasLogo
  ver.value = Date.now()
  refreshPreview()
  branding.load(true)
}

async function refreshPreview() {
  const res = await api.get('/settings/qr-preview', { responseType: 'blob' })
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(res.data)
}

async function save() {
  saving.value = true
  try {
    const { data } = await api.put('/settings', {
      'company.name': values.value['company.name'] || '',
      'company.address': values.value['company.address'] || '',
      'qr.logo_ratio': String(ratio.value)
    })
    apply(data)
    notify('Pengaturan disimpan')
  } catch (e) {
    notify(await errorMessage(e), 'error')
  } finally {
    saving.value = false
  }
}

async function upload() {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', pickedLogo.value)
    apply((await api.post('/settings/logo', fd)).data)
    logoFile.value = null
    notify('Logo diperbarui')
  } catch (e) {
    notify(await errorMessage(e), 'error')
  } finally {
    uploading.value = false
  }
}

async function removeLogo() {
  apply((await api.delete('/settings/logo')).data)
  notify('Logo dihapus')
}

onMounted(async () => apply((await api.get('/settings')).data))
onBeforeUnmount(() => previewUrl.value && URL.revokeObjectURL(previewUrl.value))
</script>

<style scoped>
.logo-box { width: 96px; height: 96px; border: 1px dashed #C5CED9; border-radius: 12px; display: flex; align-items: center; justify-content: center; padding: 8px; flex-shrink: 0; text-align: center; }
.logo-box img { max-width: 100%; max-height: 100%; }
.qr-preview { width: 100%; max-width: 300px; border: 1px solid #E3E8EF; border-radius: 12px; }
</style>
