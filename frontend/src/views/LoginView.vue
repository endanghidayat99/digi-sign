<template>
  <v-main class="login-bg">
    <v-container class="fill-height d-flex align-center justify-center">
      <v-card width="420" class="pa-8" elevation="10">
        <div class="text-center mb-6">
          <v-avatar size="72" rounded="xl" color="grey-lighten-4" class="mb-3">
            <v-img v-if="branding.logoUrl" :src="branding.logoUrl" contain />
            <v-icon v-else size="40" color="primary">mdi-qrcode-scan</v-icon>
          </v-avatar>
          <div class="text-h5 font-weight-bold text-primary">DigiSign</div>
          <div class="text-body-2 text-medium-emphasis">{{ branding.companyName || 'Tanda Tangan Digital Dokumen' }}</div>
        </div>

        <v-alert v-if="route.query.expired" type="info" variant="tonal" density="compact" class="mb-4">
          Sesi berakhir, silakan login kembali.
        </v-alert>
        <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4">{{ error }}</v-alert>

        <v-form @submit.prevent="submit">
          <v-text-field v-model="username" label="Username" prepend-inner-icon="mdi-account-outline" autofocus />
          <v-text-field
            v-model="password"
            label="Password"
            prepend-inner-icon="mdi-lock-outline"
            :type="show ? 'text' : 'password'"
            :append-inner-icon="show ? 'mdi-eye-off' : 'mdi-eye'"
            @click:append-inner="show = !show"
          />
          <v-btn type="submit" color="primary" size="large" block :loading="loading" :disabled="!username || !password">
            Masuk
          </v-btn>
        </v-form>
      </v-card>
    </v-container>
  </v-main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useBrandingStore } from '@/stores/branding'
import { errorMessage } from '@/api'

const auth = useAuthStore()
const branding = useBrandingStore()
const route = useRoute()
const router = useRouter()
const username = ref('')
const password = ref('')
const show = ref(false)
const loading = ref(false)
const error = ref('')

onMounted(() => branding.load())

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/dashboard')
  } catch (e) {
    error.value = await errorMessage(e, 'Login gagal')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  background:
    radial-gradient(circle at 15% 20%, rgba(46, 196, 182, 0.25), transparent 40%),
    radial-gradient(circle at 85% 80%, rgba(228, 161, 27, 0.18), transparent 40%),
    #0F2A4A;
}
</style>
