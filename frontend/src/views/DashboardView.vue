<template>
  <div class="d-flex align-center flex-wrap ga-3 mb-6">
    <div>
      <div class="page-title">Halo, {{ auth.user?.fullName }}</div>
      <div class="page-subtitle">Ringkasan dokumen dan tanda tangan yang menunggu Anda.</div>
    </div>
    <v-spacer />
    <v-btn color="primary" prepend-icon="mdi-file-upload-outline" to="/documents/new">Ajukan Dokumen</v-btn>
  </div>

  <v-row>
    <v-col v-for="s in stats" :key="s.label" cols="6" md="3">
      <v-card class="card-border pa-4" flat>
        <div class="d-flex align-center">
          <div>
            <div class="label">{{ s.label }}</div>
            <div class="text-h4 font-weight-bold mt-1" :class="`text-${s.color}`">{{ s.value ?? '–' }}</div>
          </div>
          <v-spacer />
          <v-avatar :color="s.color" variant="tonal" rounded="lg" size="48">
            <v-icon>{{ s.icon }}</v-icon>
          </v-avatar>
        </div>
      </v-card>
    </v-col>
  </v-row>

  <v-card class="card-border mt-6" flat>
    <v-card-title class="d-flex align-center pt-4">
      <v-icon class="mr-2" color="warning">mdi-draw-pen</v-icon>
      Menunggu Tanda Tangan Saya
    </v-card-title>
    <v-card-text>
      <v-progress-linear v-if="loading" indeterminate color="primary" />
      <div v-else-if="!data?.pending?.length" class="text-center py-8 text-medium-emphasis">
        <v-icon size="48" color="success">mdi-check-all</v-icon>
        <div class="mt-2">Tidak ada dokumen yang menunggu tanda tangan Anda.</div>
      </div>
      <v-list v-else lines="two">
        <v-list-item
          v-for="p in data.pending"
          :key="p.signatureId"
          :to="`/documents/${p.documentId}`"
          rounded="lg"
          class="mb-1"
        >
          <template #prepend><TypeChip :type="p.type" class="mr-3" /></template>
          <v-list-item-title class="font-weight-medium">{{ p.title }}</v-list-item-title>
          <v-list-item-subtitle>
            {{ p.documentNumber }}<span v-if="p.roleLabel"> · sebagai {{ p.roleLabel }}</span> · diajukan {{ fmtDateTime(p.requestedAt) }}
          </v-list-item-subtitle>
          <template #append>
            <v-btn size="small" color="primary" variant="tonal">Tandatangani</v-btn>
          </template>
        </v-list-item>
      </v-list>
    </v-card-text>
  </v-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '@/api'
import { useAuthStore } from '@/stores/auth'
import { fmtDateTime } from '@/utils/format'
import TypeChip from '@/components/TypeChip.vue'

const auth = useAuthStore()
const data = ref(null)
const loading = ref(true)

const stats = computed(() => [
  { label: 'Menunggu Saya', value: data.value?.myPending, icon: 'mdi-clock-alert-outline', color: 'warning' },
  { label: 'Dalam Proses', value: data.value?.inProgress, icon: 'mdi-progress-clock', color: 'info' },
  { label: 'Selesai', value: data.value?.completed, icon: 'mdi-check-decagram', color: 'success' },
  { label: 'TTD Bulan Ini', value: data.value?.signedThisMonth, icon: 'mdi-draw', color: 'primary' }
])

onMounted(async () => {
  try {
    data.value = (await api.get('/documents/dashboard')).data
  } finally {
    loading.value = false
  }
})
</script>
