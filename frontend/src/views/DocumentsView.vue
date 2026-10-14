<template>
  <div class="d-flex align-center flex-wrap ga-3 mb-6">
    <div>
      <div class="page-title">Dokumen</div>
      <div class="page-subtitle">Dokumen yang Anda ajukan atau harus Anda tandatangani.</div>
    </div>
    <v-spacer />
    <v-btn color="primary" prepend-icon="mdi-file-upload-outline" to="/documents/new">Ajukan Dokumen</v-btn>
  </div>

  <v-card class="card-border" flat>
    <v-card-text>
      <v-row dense>
        <v-col cols="12" md="4">
          <v-text-field v-model="q" label="Cari nomor / judul" prepend-inner-icon="mdi-magnify" clearable hide-details @update:model-value="debouncedLoad" />
        </v-col>
        <v-col cols="6" md="3">
          <v-select v-model="typeId" :items="types" item-title="name" item-value="id" label="Jenis dokumen" clearable hide-details @update:model-value="reload" />
        </v-col>
        <v-col cols="6" md="3">
          <v-select v-model="status" :items="statusItems" label="Status" clearable hide-details @update:model-value="reload" />
        </v-col>
        <v-col v-if="auth.isAdmin" cols="12" md="2" class="d-flex align-center">
          <v-switch v-model="all" label="Semua dokumen" color="primary" hide-details density="compact" @update:model-value="reload" />
        </v-col>
      </v-row>
    </v-card-text>

    <v-data-table-server
      v-model:items-per-page="size"
      v-model:page="page"
      :headers="headers"
      :items="items"
      :items-length="total"
      :loading="loading"
      hover
      @update:options="load"
      @click:row="(_, { item }) => router.push(`/documents/${item.id}`)"
      style="cursor: pointer"
    >
      <template #item.documentNumber="{ item }">
        <div class="font-weight-medium">{{ item.documentNumber }}</div>
      </template>
      <template #item.type="{ item }"><TypeChip :type="item.type" /></template>
      <template #item.progress="{ item }">
        <div class="d-flex align-center ga-2" style="min-width: 110px">
          <v-progress-linear :model-value="(item.signedCount / item.signerCount) * 100" color="success" rounded height="6" />
          <span class="text-caption">{{ item.signedCount }}/{{ item.signerCount }}</span>
        </div>
      </template>
      <template #item.status="{ item }"><StatusChip :status="item.status" /></template>
      <template #item.createdAt="{ item }">
        <div class="text-body-2">{{ fmtDateTime(item.createdAt) }}</div>
        <div class="text-caption text-medium-emphasis">{{ item.createdByName }}</div>
      </template>
      <template #no-data>
        <div class="py-8 text-medium-emphasis">Belum ada dokumen.</div>
      </template>
    </v-data-table-server>
  </v-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { useAuthStore } from '@/stores/auth'
import { DOC_STATUS, fmtDateTime } from '@/utils/format'
import StatusChip from '@/components/StatusChip.vue'
import TypeChip from '@/components/TypeChip.vue'

const router = useRouter()
const auth = useAuthStore()
const items = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const q = ref('')
const typeId = ref(null)
const status = ref(null)
const all = ref(false)
const types = ref([])
const statusItems = Object.entries(DOC_STATUS).map(([value, s]) => ({ value, title: s.text }))

const headers = [
  { title: 'Nomor Dokumen', key: 'documentNumber', sortable: false },
  { title: 'Jenis', key: 'type', sortable: false },
  { title: 'Judul', key: 'title', sortable: false },
  { title: 'Progres TTD', key: 'progress', sortable: false },
  { title: 'Status', key: 'status', sortable: false },
  { title: 'Dibuat', key: 'createdAt', sortable: false }
]

let timer
function debouncedLoad() {
  clearTimeout(timer)
  timer = setTimeout(reload, 350)
}

function reload() {
  page.value = 1
  load()
}

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/documents', {
      params: { q: q.value || '', typeId: typeId.value || undefined, status: status.value || undefined, all: all.value, page: page.value - 1, size: size.value }
    })
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  types.value = (await api.get('/document-types')).data
})
</script>
