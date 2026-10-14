<template>
  <v-chip v-if="type" size="small" label variant="flat" :color="type.color || 'primary'" class="font-weight-medium">
    {{ type.code }}
    <v-tooltip activator="parent" location="top">{{ type.name }}</v-tooltip>
  </v-chip>
</template>

<script setup>
defineProps({ type: Object })
</script>
