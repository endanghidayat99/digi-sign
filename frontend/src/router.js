import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  { path: '/verify/:token', name: 'verify', component: () => import('@/views/VerifyView.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
      { path: 'documents', name: 'documents', component: () => import('@/views/DocumentsView.vue') },
      { path: 'documents/new', name: 'document-new', component: () => import('@/views/DocumentCreateView.vue') },
      { path: 'documents/:id', name: 'document-detail', component: () => import('@/views/DocumentDetailView.vue') },
      { path: 'document-types', name: 'document-types', component: () => import('@/views/DocumentTypesView.vue'), meta: { admin: true } },
      { path: 'users', name: 'users', component: () => import('@/views/UsersView.vue'), meta: { admin: true } },
      { path: 'settings', name: 'settings', component: () => import('@/views/SettingsView.vue'), meta: { admin: true } },
      { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true
  if (!auth.isLoggedIn) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.admin && !auth.isAdmin) return { name: 'dashboard' }
  return true
})

export default router
