<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from './store/user'
import { initialChar, avatarColor } from './utils/format'

const router = useRouter()
const store = useUserStore()

function goLogin() {
  router.push('/login')
}

function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push(`/user/${store.user.id}`)
  } else if (cmd === 'edit') {
    router.push(`/user/${store.user.id}?edit=1`)
  } else if (cmd === 'logout') {
    store.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<template>
  <header class="topbar">
    <div class="container topbar-inner">
      <router-link to="/" class="brand">
        <span class="brand-dot"></span>
        CampusHub
        <span class="brand-tag">校园社区</span>
      </router-link>

      <div style="display: flex; align-items: center; gap: 12px">
        <el-button type="primary" @click="router.push('/post/create')">
          发帖
        </el-button>

        <template v-if="store.isLogin && store.user">
          <el-dropdown @command="handleCommand">
            <span style="display: flex; align-items: center; gap: 8px; cursor: pointer">
              <span
                class="avatar-circle"
                :style="{ background: avatarColor(store.user.id) }"
              >{{ initialChar(store.user) }}</span>
              <span class="clamp-1" style="max-width: 100px">{{ store.user.nickname || store.user.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">我的主页</el-dropdown-item>
                <el-dropdown-item command="edit">编辑资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-button v-else @click="goLogin">登录 / 注册</el-button>
      </div>
    </div>
  </header>

  <router-view />

  <footer class="footer">
    CampusHub · 校园论坛社区 · Spring Boot + Vue3
  </footer>
</template>
