<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const mode = ref('login') // login | register
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  nickname: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '3-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ]
}

const formRef = ref()

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    if (mode.value === 'login') {
      await store.login({ username: form.username, password: form.password })
      ElMessage.success(`欢迎回来，${store.user.nickname || store.user.username}`)
    } else {
      await store.register({ username: form.username, password: form.password, nickname: form.nickname || undefined })
      ElMessage.success('注册成功，正在登录…')
      await store.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
    }
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}

function switchMode(m) {
  mode.value = m
  formRef.value?.clearValidate()
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <span class="brand-dot"></span>
        <h1>CampusHub</h1>
        <p class="faint">校园里的人和事，都在这里</p>
      </div>

      <el-tabs :model-value="mode" class="login-tabs" @update:model-value="switchMode">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item v-if="mode === 'register'" label="昵称（可选）" prop="nickname">
          <el-input v-model="form.nickname" placeholder="同学们会看到的名字" maxlength="32" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          {{ mode === 'login' ? '登 录' : '注册并登录' }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: var(--shadow);
  padding: 36px 36px 28px;
}

.login-brand {
  text-align: center;
  margin-bottom: 18px;
}

.login-brand .brand-dot {
  display: inline-block;
  margin-bottom: 10px;
}

.login-brand h1 {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
}

.login-brand p {
  font-size: 13px;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--line);
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}
</style>
