import { defineStore } from 'pinia'
import api from '@/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('digisign.token'),
    user: JSON.parse(localStorage.getItem('digisign.user') || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    async login(username, password) {
      const { data } = await api.post('/auth/login', { username, password })
      this.token = data.token
      this.setUser(data.user)
      localStorage.setItem('digisign.token', data.token)
    },
    setUser(user) {
      this.user = user
      localStorage.setItem('digisign.user', JSON.stringify(user))
    },
    async refresh() {
      if (!this.token) return
      const { data } = await api.get('/auth/me')
      this.setUser(data)
    },
    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('digisign.token')
      localStorage.removeItem('digisign.user')
    }
  }
})
