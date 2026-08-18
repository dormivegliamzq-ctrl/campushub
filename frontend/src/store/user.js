import { defineStore } from 'pinia'
import http from '../api'

const TOKEN_KEY = 'campushub_token'
const USER_KEY = 'campushub_user'

/**
 * 用户状态：token + 当前用户信息，持久化到 localStorage（刷新不掉登录态）
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.token
  },
  actions: {
    async login(payload) {
      const data = await http.post('/auth/login', payload)
      this.token = data.token
      this.user = data.user
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    },
    async register(payload) {
      await http.post('/auth/register', payload)
    },
    async fetchMe() {
      this.user = await http.get('/user/info')
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    async updateMe(payload) {
      this.user = await http.put('/user/info', payload)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
