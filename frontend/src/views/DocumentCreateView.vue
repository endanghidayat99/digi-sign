<template>
  <div class="mb-6">
    <div class="page-title">Ajukan Dokumen</div>
    <div class="page-subtitle">Unggah PDF dan tentukan siapa saja yang harus menandatangani.</div>
  </div>

  <v-form ref="formRef" @submit.prevent="submit">
    <v-row>
      <v-col cols="12" md="7">
        <v-card class="card-border" flat>
          <v-card-title class="pt-4">Informasi Dokumen</v-card-title>
          <v-card-text>
            <v-select
              v-model="form.documentTypeId"
              :items="types"
              item-title="name"
              item-value="id"
              label="Jenis dokumen *"
              :rules="[required]"
            >
              <template #item="{ props, item }">
                <v-list-item v-bind="props" :subtitle="item.raw.description">
                  <template #prepend><TypeChip :type="item.raw" class="mr-3" /></template>
                </v-list-item>
              </template>
            </v-select>

            <v-radio-group v-model="numberMode" inline hide-details class="mb-2">
              <v-radio label="Nomor otomatis" value="auto" />
              <v-radio label="Nomor manual" value="manual" />
            </v-radio-group>
            <v-text-field
              v-if="numberMode === 'manual'"
              v-model="form.documentNumber"
              label="Nomor dokumen *"
              :rules="[required]"
            />
            <v-alert v-else-if="selectedType" type="info" variant="tonal" density="compact" class="mb-4">
              Nomor dibuat otomatis saat disimpan, contoh format: <b>{{ selectedType.numberExample }}</b>
            </v-alert>

            <v-text-field v-model="form.title" label="Judul / perihal *" :rules="[required]" counter="255" />
            <v-text-field v-model="form.documentDate" label="Tanggal dokumen" type="date" />
            <v-textarea v-model="form.description" label="Keterangan" rows="3" auto-grow />
            <v-file-input
              v-model="file"
              label="File PDF *"
              accept="application/pdf"
              prepend-icon=""
              prepend-inner-icon="mdi-file-pdf-box"
              show-size
              :rules="[(v) => !!(Array.isArray(v) ? v.length : v) || 'File wajib diisi']"
            />
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="5">
        <v-card class="card-border" flat>
          <v-card-title class="pt-4 d-flex align-center">
            Penandatangan
            <v-spacer />
            <v-btn size="small" variant="tonal" prepend-icon="mdi-plus" @click="addSigner">Tambah</v-btn>
          </v-card-title>
          <v-card-text>
            <div class="text-caption text-medium-emphasis mb-3">
              Setiap penandatangan akan menempelkan QR Code miliknya sendiri pada dokumen.
            </div>
            <div v-for="(s, i) in form.signers" :key="s.key" class="signer-row pa-3 mb-3">
              <div class="d-flex align-center mb-2">
                <v-avatar size="24" color="primary" class="mr-2"><span class="text-caption">{{ i + 1 }}</span></v-avatar>
                <span class="text-body-2 font-weight-medium">Penandatangan {{ i + 1 }}</span>
                <v-spacer />
                <v-btn icon="mdi-arrow-up" size="x-small" variant="text" :disabled="i === 0" @click="move(i, -1)" />
                <v-btn icon="mdi-arrow-down" size="x-small" variant="text" :disabled="i === form.signers.length - 1" @click="move(i, 1)" />
                <v-btn icon="mdi-delete-outline" size="x-small" variant="text" color="error" :disabled="form.signers.length === 1" @click="form.signers.splice(i, 1)" />
              </div>
              <v-autocomplete
                v-model="s.userId"
                :items="users"
                item-title="fullName"
                item-value="id"
                label="Pilih user *"
                :rules="[required, uniqueSigner]"
              >
                <template #item="{ props, item }">
                  <v-list-item v-bind="props" :subtitle="[item.raw.position, item.raw.department].filter(Boolean).join(' · ')" />
                </template>
              </v-autocomplete>
              <v-combobox
                v-model="s.roleLabel"
                :items="roleSuggestions"
                label="Peran (opsional)"
                variant="outlined"
                density="comfortable"
                hide-details
              />
            </div>
            <v-btn variant="text" size="small" prepend-icon="mdi-account-check-outline" @click="addMe">Tambahkan saya</v-btn>
          </v-card-text>
        </v-card>

        <v-alert v-if="error" type="error" variant="tonal" class="mt-4">{{ error }}</v-alert>
        <v-btn type="submit" color="primary" size="large" block class="mt-4" :loading="saving" prepend-icon="mdi-send">
          Simpan & Kirim ke Penandatangan
        </v-btn>
      </v-col>
    </v-row>
  </v-form>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api, { errorMessage } from '@/api'
import { useAuthStore } from '@/stores/auth'
import TypeChip from '@/components/TypeChip.vue'

const router = useRouter()
const auth = useAuthStore()
const notify = inject('notify')
const formRef = ref()
const types = ref([])
const users = ref([])
const file = ref(null)
const numberMode = ref('auto')
const saving = ref(false)
const error = ref('')
let keySeq = 1

const roleSuggestions = ['Pembuat', 'Pemeriksa', 'Penguji (Tester)', 'Menyetujui', 'Mengetahui', 'Pihak Pertama', 'Pihak Kedua']

const form = reactive({
  documentTypeId: null,
  documentNumber: '',
  title: '',
  documentDate: new Date().toISOString().slice(0, 10),
  description: '',
  signers: [{ key: keySeq++, userId: null, roleLabel: '' }]
})

const selectedType = computed(() => types.value.find((t) => t.id === form.documentTypeId))
const required = (v) => (v !== null && v !== undefined && String(v).trim() !== '') || 'Wajib diisi'
const uniqueSigner = (v) => !v || form.signers.filter((s) => s.userId === v).length <= 1 || 'User sudah dipilih'

function addSigner() {
  form.signers.push({ key: keySeq++, userId: null, roleLabel: '' })
}
function addMe() {
  if (form.signers.some((s) => s.userId === auth.user.id)) return
  const empty = form.signers.find((s) => !s.userId)
  if (empty) empty.userId = auth.user.id
  else form.signers.push({ key: keySeq++, userId: auth.user.id, roleLabel: '' })
}
function move(i, d) {
  const [x] = form.signers.splice(i, 1)
  form.signers.splice(i + d, 0, x)
}

onMounted(async () => {
  const [t, u] = await Promise.all([api.get('/document-types', { params: { activeOnly: true } }), api.get('/users/active')])
  types.value = t.data
  users.value = u.data
})

async function submit() {
  const { valid } = await formRef.value.validate()
  if (!valid) return
  const f = Array.isArray(file.value) ? file.value[0] : file.value
  saving.value = true
  error.value = ''
  try {
    const data = {
      documentTypeId: form.documentTypeId,
      documentNumber: numberMode.value === 'manual' ? form.documentNumber : null,
      title: form.title,
      documentDate: form.documentDate || null,
      description: form.description,
      signers: form.signers.map((s) => ({ userId: s.userId, roleLabel: s.roleLabel || null }))
    }
    const fd = new FormData()
    fd.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    fd.append('file', f)
    const res = await api.post('/documents', fd)
    notify(`Dokumen ${res.data.documentNumber} berhasil diajukan`)
    router.push(`/documents/${res.data.id}`)
  } catch (e) {
    error.value = await errorMessage(e, 'Gagal menyimpan dokumen')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.signer-row { border: 1px solid #E3E8EF; border-radius: 10px; background: #FAFBFC; }
</style>
