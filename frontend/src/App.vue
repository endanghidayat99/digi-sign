<template>
  <v-app>
    <router-view />
    <v-snackbar v-model="toast.show" :color="toast.color" location="top right" timeout="4000">
      {{ toast.text }}
    </v-snackbar>
  </v-app>
</template>

<script setup>
import { provide, reactive } from 'vue'

const toast = reactive({ show: false, text: '', color: 'success' })
provide('notify', (text, color = 'success') => {
  toast.text = text
  toast.color = color
  toast.show = true
})
</script>
