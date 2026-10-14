<template>
  <v-chip :color="meta.color" size="small" variant="tonal" :prepend-icon="meta.icon" label>
    {{ meta.text }}
  </v-chip>
</template>

<script setup>
import { computed } from 'vue'
import { DOC_STATUS, SIG_STATUS } from '@/utils/format'

const props = defineProps({ status: String, kind: { type: String, default: 'doc' } })
const meta = computed(() => (props.kind === 'sig' ? SIG_STATUS : DOC_STATUS)[props.status] || { text: props.status, color: 'grey' })
</script>
