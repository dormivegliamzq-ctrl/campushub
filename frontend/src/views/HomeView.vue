<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'
import { formatTime, initialChar, avatarColor } from '../utils/format'

const router = useRouter()

const activeTab = ref('latest')
const posts = ref([])
const hotPosts = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

async function loadPosts() {
  loading.value = true
  try {
    const data = await http.get('/posts', { params: { page: page.value, size: size.value } })
    posts.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadHot() {
  hotPosts.value = await http.get('/posts/hot', { params: { limit: 10 } })
}

function onTabChange() {
  if (activeTab.value === 'latest') loadPosts()
  else loadHot()
}

function goDetail(id) {
  router.push(`/post/${id}`)
}

function goUser(id) {
  router.push(`/user/${id}`)
}

onMounted(() => {
  loadPosts()
  loadHot()
})
</script>

<template>
  <div class="page">
    <div class="container home-layout">
      <!-- 主区 -->
      <main class="home-main">
        <el-tabs v-model="activeTab" @tab-change="onTabChange">
          <el-tab-pane label="最新帖子" name="latest" />
          <el-tab-pane label="热门榜单" name="hot" />
        </el-tabs>

        <div v-loading="loading">
          <el-empty
            v-if="!loading && activeTab === 'latest' && posts.length === 0"
            description="还没有帖子，来发第一帖吧"
          />
          <el-empty
            v-if="!loading && activeTab === 'hot' && hotPosts.length === 0"
            description="热门榜还是空的"
          />

          <template v-if="activeTab === 'latest'">
            <article v-for="p in posts" :key="p.id" class="card post-card" @click="goDetail(p.id)">
              <h3 class="post-title">{{ p.title }}</h3>
              <p class="post-excerpt clamp-2">{{ p.content }}</p>
              <div class="post-meta">
                <span class="author" @click.stop="goUser(p.userId)">
                  <span class="avatar-circle" :style="{ background: avatarColor(p.userId) }">
                    {{ initialChar({ nickname: p.nickname, username: p.username }) }}
                  </span>
                  {{ p.nickname || p.username }}
                </span>
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
          </template>

          <template v-else>
            <article
              v-for="(p, i) in hotPosts"
              :key="p.id"
              class="card post-card hot-card"
              @click="goDetail(p.id)"
            >
              <span class="hot-rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
              <div>
                <h3 class="post-title">{{ p.title }}</h3>
                <div class="post-meta">
                  <span>{{ p.nickname || p.username }}</span>
                  <span>赞 {{ p.likeCount }}</span>
                  <span>评论 {{ p.commentCount }}</span>
                  <span>浏览 {{ p.viewCount }}</span>
                </div>
              </div>
            </article>
          </template>
        </div>
      </main>

      <!-- 侧栏 -->
      <aside class="home-side">
        <div class="card side-card">
          <div class="side-title">热门帖子</div>
          <div v-if="hotPosts.length === 0" class="faint" style="font-size: 13px">暂无数据</div>
          <div
            v-for="(p, i) in hotPosts.slice(0, 5)"
            :key="p.id"
            class="hot-item"
            @click="goDetail(p.id)"
          >
            <span class="hot-rank mini" :class="{ top: i < 3 }">{{ i + 1 }}</span>
            <span class="clamp-1" style="flex: 1; font-size: 13px">{{ p.title }}</span>
          </div>
        </div>

        <div class="card side-card">
          <div class="side-title">关于社区</div>
          <p class="faint" style="font-size: 13px; line-height: 1.9">
            一个面向校园的轻社区：发帖、评论、点赞、关注，以及实时热度榜单。
            后端 Spring Boot 3 + Redis + MySQL，前端 Vue 3 + Element Plus。
          </p>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.home-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.home-main {
  flex: 1;
  min-width: 0;
}

.home-side {
  width: 280px;
  flex-shrink: 0;
  position: sticky;
  top: 84px;
}

@media (max-width: 860px) {
  .home-layout {
    flex-direction: column;
  }
  .home-side {
    width: 100%;
    position: static;
  }
}

.hot-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.hot-rank {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: var(--bg);
  color: var(--text-3);
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hot-rank.top {
  background: var(--accent-soft);
  color: var(--accent);
}

.hot-rank.mini {
  width: 22px;
  height: 22px;
  font-size: 12px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  cursor: pointer;
  border-bottom: 1px dashed var(--line);
}

.hot-item:last-child {
  border-bottom: none;
}

.hot-item:hover {
  color: var(--brand);
}
</style>
