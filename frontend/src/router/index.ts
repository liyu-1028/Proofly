import { createRouter, createWebHistory } from 'vue-router'

import DashboardView from '@/views/admin/dashboard/DashboardView.vue'
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
      path: '/admin/dashboard',
      name: 'dashboard',
      component: DashboardView,
    },
    {
      path: '/admin/projects',
      name: 'projects',
      component: ProjectsView,
    },
    {
      path: '/admin/staff',
      name: 'staff',
      component: StaffView,
    },
    {
      path: '/admin/settings',
      name: 'settings',
      component: SettingsView,
    },
    {
      path: '/review/:token',
      name: 'public-review',
      component: ReviewView,
    },
  ],
})

export default router
