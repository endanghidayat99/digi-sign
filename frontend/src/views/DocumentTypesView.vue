<template>
  <div class="d-flex align-center flex-wrap ga-3 mb-6">
    <div>
      <div class="page-title">Jenis Dokumen</div>
      <div class="page-subtitle">Tambah jenis dokumen baru dan atur format penomorannya.</div>
    </div>
    <v-spacer />
    <v-btn color="primary" prepend-icon="mdi-plus" @click="openForm()">Tambah Jenis</v-btn>
  </div>

  <v-card class="card-border" flat>
    <v-data-table :headers="headers" :items="items" :loading="loading" items-per-page="25">
      <template #item.code="{ item }"><TypeChip :type="item" /></template>
      <template #item.name="{ item }">
        <div class="font-weight-medium">{{ item.name }}</div>
        <div class="text-caption text-medium-emphasis">{{ item.description }}</div>
      </template>
      <template #item.numberFormat="{ item }">
        <div class="mono">{{ item.numberFormat }}</div>
        <div class="text-caption text-medium-emphasis">contoh: {{ item.numberExample }}</div>
      </template>
      <template #item.active="{ item }">
        <v-chip :color="item.active ? 'success' : 'grey'" size="small" variant="tonal" label>{{ item.active ? 'Aktif' : 'Nonaktif' }}</v-chip>
      </template>
      <template #item.actions="{ item }">
        <v-btn icon="mdi-pencil-outline" size="small" variant="text" @click="openForm(item)" />
        <v-btn icon="mdi-delete-outline" size="small" variant="text" color="error" :disabled="item.documentCount > 0" @click="remove(item)" />
      </template>
    </v-data-table>
  </v-card>

  <v-dialog v-model="dialog" max-width="620">
    <v-card>
      <v-card-title class="pt-4">{{ form.id ? 'Ubah' : 'Tambah' }} Jenis Dokumen</v-card-title>
      <v-card-text>
        <v-form ref="formRef">
          <v-row dense>
            <v-col cols="4"><v-text-field v-model="form.code" label="Kode *" :rules="[req]" hint="mis. FT, BAST" persistent-hint @update:model-value="form.code = (form.code || '').toUpperCase()" /></v-col>
            <v-col cols="8"><v-text-field v-model="form.name" label="Nama *" :rules="[req]" /></v-col>
            <v-col cols="12"><v-textarea v-model="form.description" label="Deskripsi" rows="2" auto-grow /></v-col>
            <v-col cols="12">
              <v-text-field v-model="form.numberFormat" label="Format nomor *" :rules="[req]" class="mono-input" @update:model-value="preview" />
              <div class="text-caption mb-2">
                Token: <code>{SEQ:4}</code> nomor urut (reset tiap tahun), <code>{CODE}</code>, <code>{YEAR}</code>, <code>{YY}</code>,
                <code>{MONTH}</code>, <code>{ROMAN_MONTH}</code>, <code>{DAY}</code>
              </div>
              <v-alert density="compact" variant="tonal" type="info">Contoh hasil: <b>{{ example }}</b></v-alert>
            </v-col>
            <v-col cols="6" class="mt-3">
              <div class="label mb-1">Warna label</div>
              <div class="d-flex ga-2 flex-wrap">
                <v-btn v-for="c in colors" :key="c" :color="c" icon size="x-small" @click="form.color = c">
                  <v-icon v-if="form.color === c" color="white">mdi-check</v-icon>
                </v-btn>
              </div>
            </v-col>
            <v-col cols="6" class="mt-3"><v-switch v-model="form.active" label="Aktif" color="primary" hide-details /></v-col>
          </v-row>
        </v-form>
        <v-alert v-if="error" type="error" variant="tonal" class="mt-3">{{ error }}</v-alert>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="dialog = false">Batal</v-btn>
        <v-btn color="primary" variant="flat" :loading="saving" @click="save">Simpan</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import api, { errorMessage } from '@/api'
import TypeChip from '@/components/TypeChip.vue'

const notify = inject('notify')
const items = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const error = ref('')
const formRef = ref()
const form = ref({})
const example = ref('')
const colors = ['#1565C0', '#2E7D32', '#EF6C00', '#6A1B9A', '#C62828', '#00838F', '#4E342E', '#37474F']
const req = (v) => !!(v && String(v).trim()) || 'Wajib diisi'

const headers = [
  { title: 'Kode', key: 'code' },
  { title: 'Nama', key: 'name' },
  { title: 'Format Nomor', key: 'numberFormat', sortable: false },
  { title: 'Dokumen', key: 'documentCount', align: 'end' },
  { title: 'Status', key: 'active' },
  { title: '', key: 'actions', sortable: false, align: 'end' }
]

async function load() {
  loading.value = true
  try {
    items.value = (await api.get('/document-types')).data
  } finally {
    loading.value = false
  }
}

function openForm(item) {
  error.value = ''
  form.value = item
    ? { ...item }
    : { code: '', name: '', description: '', numberFormat: '{SEQ:4}/{CODE}/{ROMAN_MONTH}/{YEAR}', color: colors[0], active: true }
  dialog.value = true
  preview()
}

let t
function preview() {
  clearTimeout(t)
  t = setTimeout(async () => {
    if (!form.value.numberFormat) return (example.value = '')
    example.value = (await api.get('/document-types/preview-number', { params: { format: form.value.numberFormat, code: form.value.code || 'KODE' } })).data
  }, 250)
}

async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return
  saving.value = true
  error.value = ''
  try {
    const body = { code: form.value.code, name: form.value.name, description: form.value.description, numberFormat: form.value.numberFormat, color: form.value.color, active: form.value.active }
    if (form.value.id) await api.put(`/document-types/${form.value.id}`, body)
    else await api.post('/document-types', body)
    dialog.value = false
    notify('Jenis dokumen disimpan')
    load()
  } catch (e) {
    error.value = await errorMessage(e)
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  if (!confirm(`Hapus jenis dokumen "${item.name}"?`)) return
  try {
    await api.delete(`/document-types/${item.id}`)
    notify('Jenis dokumen dihapus')
    load()
  } catch (e) {
    notify(await errorMessage(e), 'error')
  }
}

onMounted(load)
</script>
