import axios from 'axios'

const api = axios.create({ baseURL: '/api', timeout: 120000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('digisign.token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    if (error.response?.status === 401 && !error.config.url.includes('/auth/login')) {
      localStorage.removeItem('digisign.token')
      localStorage.removeItem('digisign.user')
      if (!location.pathname.startsWith('/verify') && location.pathname !== '/login') {
        location.href = '/login?expired=1&redirect=' + encodeURIComponent(location.pathname)
      }
    }
    return Promise.reject(error)
  }
)

/** Ambil pesan error yang ramah dari respons API (termasuk respons blob). */
export async function errorMessage(e, fallback = 'Terjadi kesalahan') {
  const data = e?.response?.data
  if (data instanceof Blob) {
    try {
      return JSON.parse(await data.text()).message || fallback
    } catch {
      return fallback
    }
  }
  return data?.message || e?.message || fallback
}

/** Unduh file dari endpoint terproteksi. */
export async function downloadFile(url, filename) {
  const res = await api.get(url, { responseType: 'blob' })
  const href = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = href
  a.download = filename
  a.click()
  setTimeout(() => URL.revokeObjectURL(href), 1000)
}

export default api
