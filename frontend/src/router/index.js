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
        path: 'packages',
        name: 'MyPackages',
        component: () => import('@/views/MyPackagesView.vue'),
      },
      {
        path: 'track',
        name: 'Track',
        component: () => import('@/views/TrackView.vue'),
      },
      {
        path: 'appeal',
        name: 'Appeal',
        component: () => import('@/views/AppealView.vue'),
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
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManageView.vue'),
        meta: { roles: ['ADMIN'] },
      },
      {
        path: 'admin/appeals',
        name: 'AdminAppeals',
        component: () => import('@/views/admin/AppealManageView.vue'),
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

  if (to.meta.guest && token) {
    return next({ name: 'Dashboard' })
  }
  if (to.meta.requiresAuth && !token) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }
  if (to.meta.roles && user && !to.meta.roles.includes(user.role)) {
    return next({ name: 'Dashboard' })
  }
  next()
})

export default router
