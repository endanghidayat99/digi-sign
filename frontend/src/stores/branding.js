import { defineStore } from 'pinia'
import api from '@/api'

export const useBrandingStore = defineStore('branding', {
  state: () => ({ companyName: '', companyAddress: '', hasLogo: false, logoVersion: Date.now(), loaded: false }),
  getters: {
    logoUrl: (s) => (s.hasLogo ? `/api/public/branding/logo?v=${s.logoVersion}` : null)
  },
  actions: {
    async load(force = false) {
      if (this.loaded && !force) return
      try {
        const { data } = await api.get('/public/branding')
        this.companyName = data.companyName
        this.companyAddress = data.companyAddress
        this.hasLogo = data.hasLogo
        this.logoVersion = Date.now()
        this.loaded = true
      } catch {
        /* abaikan */
      }
    }
  }
})
