import { createRouter, createWebHistory } from 'vue-router'

import { useSessionStore } from '@/stores/session'
import DashboardView from '@/views/admin/dashboard/DashboardView.vue'
import LoginView from '@/views/auth/LoginView.vue'
import ProjectsView from '@/views/admin/projects/ProjectsView.vue'
import SettingsView from '@/views/admin/settings/SettingsView.vue'
import StaffView from '@/views/admin/staff/StaffView.vue'
import ReviewView from '@/views/public/review/ReviewView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/admin/dashboard',
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        public: true,
      },
    },
    {
      path: '/admin/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/admin/projects',
      name: 'projects',
      component: ProjectsView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/admin/staff',
      name: 'staff',
      component: StaffView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/admin/settings',
      name: 'settings',
      component: SettingsView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/review/:token',
      name: 'public-review',
      component: ReviewView,
      meta: {
        public: true,
      },
    },
  ],
})

router.beforeEach(async (to) => {
  const session = useSessionStore()
  await session.initialize()

  if (to.meta.requiresAuth && !session.isAuthenticated) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (to.name === 'login' && session.isAuthenticated) {
    return '/admin/dashboard'
  }
})

export default router
