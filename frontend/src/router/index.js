import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  {
    path: '/post/create',
    name: 'post-create',
    component: () => import('../views/PostCreateView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/post/:id/edit',
    name: 'post-edit',
    component: () => import('../views/PostEditView.vue'),
    meta: { requiresAuth: true }
  },
  { path: '/post/:id', name: 'post-detail', component: () => import('../views/PostDetailView.vue') },
  { path: '/user/:id', name: 'profile', component: () => import('../views/ProfileView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：需要登录的页面未登录时跳登录页
router.beforeEach((to) => {
  const store = useUserStore()
  if (to.meta.requiresAuth && !store.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
