<template>
  <v-navigation-drawer v-model="drawer" color="primary" width="260">
    <div class="d-flex align-center pa-4 ga-3">
      <v-avatar rounded="lg" size="40" color="white">
        <v-img v-if="branding.logoUrl" :src="branding.logoUrl" contain />
        <v-icon v-else color="primary">mdi-qrcode-scan</v-icon>
      </v-avatar>
      <div class="overflow-hidden">
        <div class="text-subtitle-1 font-weight-bold">DigiSign</div>
        <div class="text-caption text-truncate" style="opacity: .75">{{ branding.companyName }}</div>
      </div>
    </div>
    <v-divider class="mb-2" style="opacity:.2" />
    <v-list nav density="comfortable">
      <v-list-item prepend-icon="mdi-view-dashboard-outline" title="Dashboard" to="/dashboard" />
      <v-list-item prepend-icon="mdi-file-document-multiple-outline" title="Dokumen" to="/documents" exact />
      <v-list-item prepend-icon="mdi-file-upload-outline" title="Ajukan Dokumen" to="/documents/new" />
      <template v-if="auth.isAdmin">
        <v-list-subheader class="text-white" style="opacity:.6">ADMINISTRASI</v-list-subheader>
        <v-list-item prepend-icon="mdi-shape-outline" title="Jenis Dokumen" to="/document-types" />
        <v-list-item prepend-icon="mdi-account-group-outline" title="Pengguna" to="/users" />
        <v-list-item prepend-icon="mdi-cog-outline" title="Pengaturan & Logo QR" to="/settings" />
      </template>
    </v-list>
  </v-navigation-drawer>

  <v-app-bar flat color="surface" class="card-border" style="border-width: 0 0 1px 0 !important">
    <v-app-bar-nav-icon @click="drawer = !drawer" />
    <v-spacer />
    <v-menu>
      <template #activator="{ props }">
        <v-btn v-bind="props" variant="text" class="text-none">
          <v-avatar size="30" color="secondary" class="mr-2">
            <span class="text-white text-caption font-weight-bold">{{ initials }}</span>
          </v-avatar>
          <div class="text-left d-none d-sm-block">
            <div class="text-body-2 font-weight-medium">{{ auth.user?.fullName }}</div>
            <div class="text-caption text-medium-emphasis" style="line-height:1">{{ auth.user?.position || auth.user?.role }}</div>
          </div>
          <v-icon end>mdi-chevron-down</v-icon>
        </v-btn>
      </template>
      <v-list density="compact">
        <v-list-item prepend-icon="mdi-account-circle-outline" title="Profil & Password" to="/profile" />
        <v-list-item prepend-icon="mdi-logout" title="Keluar" @click="logout" />
      </v-list>
    </v-menu>
  </v-app-bar>

  <v-main>
    <v-container fluid class="pa-4 pa-md-6" style="max-width: 1400px">
      <router-view />
    </v-container>
  </v-main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useBrandingStore } from '@/stores/branding'

const auth = useAuthStore()
const branding = useBrandingStore()
const router = useRouter()
const drawer = ref(null)

const initials = computed(() =>
  (auth.user?.fullName || '?').split(' ').filter(Boolean).slice(0, 2).map((s) => s[0].toUpperCase()).join('')
)

onMounted(() => {
  branding.load()
  auth.refresh().catch(() => {})
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
