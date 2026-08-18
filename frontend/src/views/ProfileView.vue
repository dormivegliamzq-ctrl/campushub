<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'
import { useUserStore } from '../store/user'
import { formatTime, initialChar, avatarColor } from '../utils/format'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const profile = ref(null)
const posts = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const editVisible = ref(false)
const editForm = reactive({ nickname: '', bio: '' })
const editLoading = ref(false)

const listVisible = ref(false) // 关注/粉丝列表弹窗
const listMode = ref('following')
const listData = ref([])

const isSelf = computed(() => store.isLogin && store.user && Number(route.params.id) === store.user.id)

async function loadProfile() {
  profile.value = await http.get(`/users/${route.params.id}`)
}

async function loadPosts() {
  const data = await http.get(`/users/${route.params.id}/posts`, {
    params: { page: page.value, size: size.value }
  })
  posts.value = data.records
  total.value = data.total
}

async function toggleFollow() {
  if (!store.isLogin) {
    router.push('/login')
    return
  }
  if (profile.value.followed) {
    await http.delete(`/follow/${profile.value.id}`)
    profile.value.followed = false
    profile.value.followerCount -= 1
    ElMessage.success('已取消关注')
  } else {
    await http.post(`/follow/${profile.value.id}`)
    profile.value.followed = true
    profile.value.followerCount += 1
    ElMessage.success('关注成功')
  }
}

function openEdit() {
  editForm.nickname = profile.value.nickname || ''
  editForm.bio = profile.value.bio || ''
  editVisible.value = true
}

async function saveEdit() {
  editLoading.value = true
  try {
    await store.updateMe({ nickname: editForm.nickname, bio: editForm.bio })
    ElMessage.success('资料已更新')
    editVisible.value = false
    loadProfile()
  } finally {
    editLoading.value = false
  }
}

async function openList(mode) {
  listMode.value = mode
  listVisible.value = true
  listData.value = await http.get(`/follow/${mode}`)
}

function goDetail(id) {
  router.push(`/post/${id}`)
}

function goUser(id) {
  router.push(`/user/${id}`)
}

onMounted(() => {
  loadProfile()
  loadPosts()
  if (route.query.edit === '1' && isSelf.value) {
    openEdit()
  }
})
</script>

<template>
  <div class="page">
    <div class="container" style="max-width: 820px">
      <!-- 用户信息卡 -->
      <div v-if="profile" class="card profile-card">
        <span
          class="avatar-circle lg"
          :style="{ background: avatarColor(profile.id) }"
        >{{ initialChar({ nickname: profile.nickname, username: profile.username }) }}</span>

        <div class="profile-info">
          <div class="profile-name">
            {{ profile.nickname || profile.username }}
            <span class="faint" style="font-size: 13px; font-weight: 400">@{{ profile.username }}</span>
          </div>
          <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>
          <p v-else class="faint" style="font-size: 13px">这个人很懒，还没有写签名</p>
          <p class="faint" style="font-size: 12px">加入于 {{ formatTime(profile.createTime) }}</p>
        </div>

        <div class="profile-actions">
          <el-button v-if="isSelf" @click="openEdit">编辑资料</el-button>
          <el-button
            v-else-if="store.isLogin"
            :type="profile.followed ? 'default' : 'primary'"
            @click="toggleFollow"
          >
            {{ profile.followed ? '已关注' : '+ 关注' }}
          </el-button>
          <el-button v-else @click="router.push('/login')">+ 关注</el-button>
        </div>
      </div>

      <!-- 统计条 -->
      <div v-if="profile" class="card stats-card">
        <div class="stat" @click="isSelf && openList('following')">
          <div class="stat-num">{{ profile.followingCount }}</div>
          <div class="stat-label">关注</div>
        </div>
        <div class="stat" @click="isSelf && openList('followers')">
          <div class="stat-num">{{ profile.followerCount }}</div>
          <div class="stat-label">粉丝</div>
        </div>
        <div class="stat">
          <div class="stat-num">{{ profile.postCount }}</div>
          <div class="stat-label">帖子</div>
        </div>
      </div>

      <!-- 帖子列表 -->
      <div class="card" style="padding: 6px 22px 22px">
        <div class="side-title" style="margin: 16px 0">TA 的帖子</div>
        <el-empty v-if="posts.length === 0" description="还没有发过帖子" />
        <article v-for="p in posts" :key="p.id" class="card post-card" @click="goDetail(p.id)">
          <h3 class="post-title">{{ p.title }}</h3>
          <p class="post-excerpt clamp-2">{{ p.content }}</p>
          <div class="post-meta">
            <span>{{ formatTime(p.createTime) }}</span>
            <span>浏览 {{ p.viewCount }}</span>
            <span>赞 {{ p.likeCount }}</span>
            <span>评论 {{ p.commentCount }}</span>
          </div>
        </article>
        <el-pagination
          v-if="total > size"
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadPosts"
        />
      </div>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editVisible" title="编辑资料" width="420px">
      <el-form label-position="top">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 关注/粉丝列表弹窗 -->
    <el-dialog v-model="listVisible" :title="listMode === 'following' ? '我关注的人' : '我的粉丝'" width="400px">
      <div v-if="listData.length === 0" class="faint" style="text-align: center; padding: 20px">
        暂无数据
      </div>
      <div v-for="u in listData" :key="u.id" class="list-user" @click="goUser(u.id)">
        <span class="avatar-circle" :style="{ background: avatarColor(u.id) }">
          {{ initialChar(u) }}
        </span>
        <span style="flex: 1">{{ u.nickname || u.username }}</span>
        <span class="faint" style="font-size: 12px">@{{ u.username }}</span>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 26px 30px;
  margin-bottom: 14px;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-size: 20px;
  font-weight: 700;
}

.profile-bio {
  color: var(--text-2);
  font-size: 14px;
  margin: 6px 0 2px;
}

.stats-card {
  display: flex;
  padding: 16px 0;
  margin-bottom: 14px;
}

.stat {
  flex: 1;
  text-align: center;
  cursor: default;
}

.stat + .stat {
  border-left: 1px solid var(--line);
}

.list-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
}

.list-user:last-child {
  border-bottom: none;
}

.list-user:hover {
  color: var(--brand);
}
</style>
