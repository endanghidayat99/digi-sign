<template>
  <div class="mb-6">
    <div class="page-title">Profil Saya</div>
    <div class="page-subtitle">Nama & jabatan ini yang tercetak pada QR tanda tangan Anda.</div>
  </div>

  <v-row>
    <v-col cols="12" md="6">
      <v-card class="card-border" flat>
        <v-card-title class="pt-4">Data Diri</v-card-title>
        <v-card-text>
          <v-text-field :model-value="auth.user?.username" label="Username" disabled />
          <v-text-field v-model="profile.fullName" label="Nama lengkap *" />
          <v-text-field v-model="profile.position" label="Jabatan" />
          <v-text-field v-model="profile.department" label="Divisi / Unit" />
          <v-text-field v-model="profile.email" label="Email" type="email" />
          <v-btn color="primary" :loading="savingProfile" :disabled="!profile.fullName" @click="saveProfile">Simpan</v-btn>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" md="6">
      <v-card class="card-border" flat>
        <v-card-title class="pt-4">Ganti Password</v-card-title>
        <v-card-text>
          <v-text-field v-model="pwd.oldPassword" label="Password lama" type="password" />
          <v-text-field v-model="pwd.newPassword" label="Password baru (min. 6)" type="password" />
          <v-text-field v-model="pwd.confirm" label="Ulangi password baru" type="password" :error-messages="pwd.confirm && pwd.confirm !== pwd.newPassword ? 'Tidak sama' : ''" />
          <v-btn color="primary" :loading="savingPwd" :disabled="!pwd.oldPassword || pwd.newPassword.length < 6 || pwd.newPassword !== pwd.confirm" @click="changePwd">
            Ganti Password
          </v-btn>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup>
import { inject, reactive, ref } from 'vue'
import api, { errorMessage } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const notify = inject('notify')
const profile = reactive({
  fullName: auth.user?.fullName || '',
  position: auth.user?.position || '',
  department: auth.user?.department || '',
  email: auth.user?.email || ''
})
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const savingProfile = ref(false)
const savingPwd = ref(false)

async function saveProfile() {
  savingProfile.value = true
  try {
    auth.setUser((await api.put('/auth/me', { ...profile, email: profile.email || null })).data)
    notify('Profil disimpan')
  } catch (e) {
    notify(await errorMessage(e), 'error')
  } finally {
    savingProfile.value = false
  }
}

async function changePwd() {
  savingPwd.value = true
  try {
    await api.post('/auth/change-password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    Object.assign(pwd, { oldPassword: '', newPassword: '', confirm: '' })
    notify('Password diganti')
  } catch (e) {
    notify(await errorMessage(e), 'error')
  } finally {
    savingPwd.value = false
  }
}
</script>
