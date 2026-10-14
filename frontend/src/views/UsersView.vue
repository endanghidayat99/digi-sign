<template>
  <div class="d-flex align-center flex-wrap ga-3 mb-6">
    <div>
      <div class="page-title">Pengguna</div>
      <div class="page-subtitle">Kelola akun pegawai yang dapat mengajukan & menandatangani dokumen.</div>
    </div>
    <v-spacer />
    <v-btn color="primary" prepend-icon="mdi-account-plus-outline" @click="openForm()">Tambah Pengguna</v-btn>
  </div>

  <v-card class="card-border" flat>
    <v-card-text>
      <v-text-field v-model="search" prepend-inner-icon="mdi-magnify" label="Cari" hide-details clearable style="max-width: 360px" />
    </v-card-text>
    <v-data-table :headers="headers" :items="items" :search="search" :loading="loading" items-per-page="25">
      <template #item.fullName="{ item }">
        <div class="font-weight-medium">{{ item.fullName }}</div>
        <div class="text-caption text-medium-emphasis">@{{ item.username }}<span v-if="item.employeeNo"> · NIP {{ item.employeeNo }}</span></div>
      </template>
      <template #item.position="{ item }">
        <div>{{ item.position || '-' }}</div>
        <div class="text-caption text-medium-emphasis">{{ item.department }}</div>
      </template>
      <template #item.role="{ item }">
        <v-chip size="small" label :color="item.role === 'ADMIN' ? 'accent' : 'primary'" variant="tonal">{{ item.role }}</v-chip>
      </template>
      <template #item.active="{ item }">
        <v-chip :color="item.active ? 'success' : 'grey'" size="small" variant="tonal" label>{{ item.active ? 'Aktif' : 'Nonaktif' }}</v-chip>
      </template>
      <template #item.actions="{ item }">
        <v-btn icon="mdi-pencil-outline" size="small" variant="text" @click="openForm(item)" />
      </template>
    </v-data-table>
  </v-card>

  <v-dialog v-model="dialog" max-width="620">
    <v-card>
      <v-card-title class="pt-4">{{ form.id ? 'Ubah' : 'Tambah' }} Pengguna</v-card-title>
      <v-card-text>
        <v-form ref="formRef">
          <v-row dense>
            <v-col cols="12" sm="6"><v-text-field v-model="form.username" label="Username *" :rules="[req]" /></v-col>
            <v-col cols="12" sm="6">
              <v-text-field v-model="form.password" :label="form.id ? 'Password baru (kosongkan jika tetap)' : 'Password *'" type="password" :rules="form.id ? [optPwd] : [req, optPwd]" />
            </v-col>
            <v-col cols="12"><v-text-field v-model="form.fullName" label="Nama lengkap (tampil di tanda tangan) *" :rules="[req]" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model="form.position" label="Jabatan" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model="form.department" label="Divisi / Unit" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model="form.employeeNo" label="NIP / No. Pegawai" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model="form.email" label="Email" type="email" /></v-col>
            <v-col cols="6"><v-select v-model="form.role" :items="['USER', 'ADMIN']" label="Role" /></v-col>
            <v-col cols="6"><v-switch v-model="form.active" label="Aktif" color="primary" /></v-col>
          </v-row>
        </v-form>
        <v-alert v-if="error" type="error" variant="tonal">{{ error }}</v-alert>
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

const notify = inject('notify')
const items = ref([])
const loading = ref(false)
const search = ref('')
const dialog = ref(false)
const saving = ref(false)
const error = ref('')
const formRef = ref()
const form = ref({})
const req = (v) => !!(v && String(v).trim()) || 'Wajib diisi'
const optPwd = (v) => !v || v.length >= 6 || 'Minimal 6 karakter'

const headers = [
  { title: 'Nama', key: 'fullName' },
  { title: 'Jabatan', key: 'position' },
  { title: 'Email', key: 'email' },
  { title: 'Role', key: 'role' },
  { title: 'Status', key: 'active' },
  { title: '', key: 'actions', sortable: false, align: 'end' }
]

async function load() {
  loading.value = true
  try {
    items.value = (await api.get('/users')).data
  } finally {
    loading.value = false
  }
}

function openForm(item) {
  error.value = ''
  form.value = item ? { ...item, password: '' } : { username: '', password: '', fullName: '', position: '', department: '', employeeNo: '', email: '', role: 'USER', active: true }
  dialog.value = true
}

async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return
  saving.value = true
  error.value = ''
  try {
    const body = { ...form.value, password: form.value.password || null, email: form.value.email || null }
    delete body.id
    if (form.value.id) await api.put(`/users/${form.value.id}`, body)
    else await api.post('/users', body)
    dialog.value = false
    notify('Pengguna disimpan')
    load()
  } catch (e) {
    error.value = await errorMessage(e)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
