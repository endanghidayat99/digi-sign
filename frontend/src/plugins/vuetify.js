import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import { id } from 'vuetify/locale'

export default createVuetify({
  locale: { locale: 'id', messages: { id } },
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        colors: {
          primary: '#0F2A4A',
          secondary: '#2EC4B6',
          accent: '#E4A11B',
          background: '#F4F6F9',
          surface: '#FFFFFF',
          success: '#1B8A5A',
          error: '#C62828',
          warning: '#E08A00',
          info: '#1565C0'
        }
      }
    }
  },
  defaults: {
    VTextField: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VTextarea: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VSelect: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VAutocomplete: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VFileInput: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VCard: { rounded: 'lg' },
    VBtn: { rounded: 'lg' }
  }
})
