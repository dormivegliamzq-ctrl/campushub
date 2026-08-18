<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'
import { useUserStore } from '../store/user'
import { formatTime, initialChar, avatarColor } from '../utils/format'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const post = ref(null)
const comments = ref([])
const commentText = ref('')
const replyTo = ref(null) // { id, username } 正在回复谁
const submitting = ref(false)
const liking = ref(false)

async function load() {
  post.value = await http.get(`/posts/${route.params.id}`)
  comments.value = await http.get(`/posts/${route.params.id}/comments`)
}

async function toggleLike() {
  if (!store.isLogin) {
    router.push('/login')
    return
  }
  liking.value = true
  try {
    if (post.value.liked) {
      await http.delete(`/posts/${post.value.id}/like`)
      post.value.liked = false
      post.value.likeCount -= 1
    } else {
      await http.post(`/posts/${post.value.id}/like`)
      post.value.liked = true
      post.value.likeCount += 1
    }
  } finally {
    liking.value = false
  }
}

function startReply(c) {
  replyTo.value = { id: c.id, username: c.username }
}

function cancelReply() {
  replyTo.value = null
}

async function submitComment() {
  if (!store.isLogin) {
    router.push('/login')
    return
  }
  const content = commentText.value.trim()
  if (!content) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  submitting.value = true
  try {
    await http.post(`/posts/${post.value.id}/comments`, {
      content,
      parentId: replyTo.value ? replyTo.value.id : undefined
    })
    commentText.value = ''
    replyTo.value = null
    ElMessage.success('评论成功')
    comments.value = await http.get(`/posts/${post.value.id}/comments`)
    post.value.commentCount += 1
  } finally {
    submitting.value = false
  }
}

async function removeComment(c) {
  await http.delete(`/comments/${c.id}`)
  ElMessage.success('评论已删除')
  comments.value = await http.get(`/posts/${post.value.id}/comments`)
  post.value.commentCount -= 1
}

async function removePost() {
  await http.delete(`/posts/${post.value.id}`)
  ElMessage.success('帖子已删除')
  router.push('/')
}

function goUser(id) {
  router.push(`/user/${id}`)
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="container" style="max-width: 820px">
      <div v-if="post" class="card detail-card">
        <h1 class="detail-title">{{ post.title }}</h1>

        <div class="detail-author">
          <span
            class="avatar-circle"
            :style="{ background: avatarColor(post.userId) }"
            @click="goUser(post.userId)"
            style="cursor: pointer"
          >{{ initialChar({ nickname: post.nickname, username: post.username }) }}</span>
          <div>
            <div class="author-name" @click="goUser(post.userId)" style="cursor: pointer">
              {{ post.nickname || post.username }}
              <span class="faint" style="font-size: 12px; margin-left: 6px">@{{ post.username }}</span>
            </div>
            <div class="faint" style="font-size: 12px">{{ formatTime(post.createTime) }}</div>
          </div>
        </div>

        <div class="detail-content">{{ post.content }}</div>

        <div class="detail-actions">
          <el-button
            :type="post.liked ? 'warning' : 'default'"
            :loading="liking"
            round
            @click="toggleLike"
          >
            {{ post.liked ? '❤ 已赞' : '♡ 点赞' }} {{ post.likeCount }}
          </el-button>
          <span class="faint" style="font-size: 13px">
            浏览 {{ post.viewCount }} · 评论 {{ post.commentCount }}
          </span>
          <span style="flex: 1"></span>
          <el-dropdown v-if="store.user && store.user.id === post.userId">
            <el-button text size="small" class="faint">···</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push(`/post/${post.id}/edit`)">编辑</el-dropdown-item>
                <el-dropdown-item @click="removePost" style="color: #c45656">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="card comment-card">
        <div class="side-title">评论 {{ post?.commentCount || 0 }}</div>

        <div class="comment-input">
          <div v-if="replyTo" class="replying">
            回复 @{{ replyTo.username }}
            <el-button link size="small" @click="cancelReply">取消</el-button>
          </div>
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            :placeholder="store.isLogin ? '友善评论，理性发言…' : '登录后参与评论'"
          />
          <div style="text-align: right; margin-top: 10px">
            <el-button type="primary" :loading="submitting" @click="submitComment">
              发表评论
            </el-button>
          </div>
        </div>

        <el-empty v-if="comments.length === 0" description="还没有评论，来抢沙发" />

        <div v-for="c in comments" :key="c.id" class="comment-item">
          <span
            class="avatar-circle"
            :style="{ background: avatarColor(c.userId) }"
            @click="goUser(c.userId)"
            style="cursor: pointer"
          >{{ initialChar({ nickname: c.nickname, username: c.username }) }}</span>
          <div class="comment-body">
            <div class="comment-head">
              <span class="author-name" @click="goUser(c.userId)" style="cursor: pointer">
                {{ c.nickname || c.username }}
              </span>
              <span v-if="c.replyToUsername" class="faint" style="font-size: 13px">
                回复 @{{ c.replyToUsername }}
              </span>
              <span class="faint" style="font-size: 12px">{{ formatTime(c.createTime) }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
            <div class="comment-foot">
              <el-button link size="small" @click="startReply(c)">回复</el-button>
              <el-button
                v-if="store.user && store.user.id === c.userId"
                link
                size="small"
                style="color: #c45656"
                @click="removeComment(c)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-card {
  padding: 30px 34px;
  margin-bottom: 16px;
}

.detail-title {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
  margin-bottom: 16px;
}

.detail-author {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.author-name {
  font-size: 14px;
  font-weight: 600;
}

.detail-content {
  padding: 22px 0;
  font-size: 15px;
  line-height: 2;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.comment-card {
  padding: 22px 34px 28px;
}

.comment-input {
  margin-bottom: 24px;
}

.replying {
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 6px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
}

.comment-content {
  margin: 6px 0 2px;
  word-break: break-word;
}

.comment-foot {
  display: flex;
  gap: 4px;
}
</style>
