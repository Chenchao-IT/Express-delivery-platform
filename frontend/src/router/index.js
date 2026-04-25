import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { guest: true },
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: { guest: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/PackageCenterView.vue'),
      },
      {
        path: 'track',
        name: 'Track',
        component: () => import('@/views/TrackView.vue'),
      },
      {
        path: 'tasks',
        name: 'TaskHall',
        component: () => import('@/views/TaskHallView.vue'),
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/NotificationsView.vue'),
      },
      {
        path: 'admin/packages',
        name: 'AdminPackages',
        component: () => import('@/views/admin/PackageManageView.vue'),
        meta: { roles: ['ADMIN', 'COURIER'] },
      },
      {
        path: 'admin/deliveries',
        name: 'AdminDeliveries',
        component: () => import('@/views/admin/DeliveryManageView.vue'),
        meta: { roles: ['ADMIN', 'COURIER'] },
      },
      {
        path: 'admin/dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { roles: ['ADMIN'] },
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManageView.vue'),
        meta: { roles: ['ADMIN'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  if (to.meta.guest && token && to.name !== 'Forbidden') {
    return next({ name: 'Dashboard' })
  }

  if (to.meta.requiresAuth && !token) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  if (to.meta.roles && user && !to.meta.roles.includes(user.role)) {
    return next({ name: 'Forbidden', query: { from: to.fullPath } })
  }

  next()
})

export default router
