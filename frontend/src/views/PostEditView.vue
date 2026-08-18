<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'

const route = useRoute()
const router = useRouter()

const form = reactive({
  title: '',
  content: ''
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 64, message: '标题最长 64 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { max: 20000, message: '内容最长 20000 个字符', trigger: 'blur' }
  ]
}

const formRef = ref()
const submitting = ref(false)

onMounted(async () => {
  const post = await http.get(`/posts/${route.params.id}`)
  form.title = post.title
  form.content = post.content
})

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await http.put(`/posts/${route.params.id}`, { title: form.title, content: form.content })
    ElMessage.success('修改成功')
    router.push(`/post/${route.params.id}`)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="container" style="max-width: 760px">
      <div class="card" style="padding: 30px 34px">
        <h2 style="font-size: 20px; font-weight: 700; margin-bottom: 22px">编辑帖子</h2>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item label="正文" prop="content">
            <el-input v-model="form.content" type="textarea" :rows="12" maxlength="20000" />
          </el-form-item>
          <div style="display: flex; gap: 12px">
            <el-button type="primary" size="large" :loading="submitting" @click="submit">
              保存
            </el-button>
            <el-button size="large" @click="router.back()">取消</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>
