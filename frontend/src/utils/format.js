const dtf = new Intl.DateTimeFormat('id-ID', {
  day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit', timeZoneName: 'short'
})
const df = new Intl.DateTimeFormat('id-ID', { day: '2-digit', month: 'long', year: 'numeric' })

export const fmtDateTime = (v) => (v ? dtf.format(new Date(v)) : '-')
export const fmtDate = (v) => (v ? df.format(new Date(v.length === 10 ? v + 'T00:00:00' : v)) : '-')

export const DOC_STATUS = {
  IN_PROGRESS: { text: 'Proses Tanda Tangan', color: 'warning', icon: 'mdi-progress-clock' },
  COMPLETED: { text: 'Selesai', color: 'success', icon: 'mdi-check-decagram' },
  REJECTED: { text: 'Ditolak', color: 'error', icon: 'mdi-close-octagon' },
  CANCELLED: { text: 'Dibatalkan', color: 'grey', icon: 'mdi-cancel' }
}

export const SIG_STATUS = {
  PENDING: { text: 'Menunggu', color: 'warning', icon: 'mdi-clock-outline' },
  SIGNED: { text: 'Ditandatangani', color: 'success', icon: 'mdi-check-circle' },
  REJECTED: { text: 'Ditolak', color: 'error', icon: 'mdi-close-circle' }
}
