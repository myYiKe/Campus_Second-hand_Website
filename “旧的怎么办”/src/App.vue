<template>
  <div class="jd-page">
    <header class="shortcut-bar">
      <div class="page-container shortcut-inner">
        <div class="shortcut-links">
          <span>川大认证</span>
          <span>望江 / 江安</span>
        </div>
        <div class="shortcut-links">
          <span>{{ state.backendOnline ? '已连接' : '未连接' }}</span>
          <button class="mini-link" @click="handleHealthCheck">检测后端</button>
        </div>
      </div>
    </header>

    <header class="search-header">
      <div class="page-container header-main">
        <RouterLink class="brand-block brand-link" to="/">
          <div class="brand-logo">旧</div>
          <div>
            <h1>旧的怎么办</h1>
            <p>四川大学校园二手交易平台</p>
          </div>
        </RouterLink>

        <div
          ref="searchBlockRef"
          :class="['search-block', { 'active-panel': panelVisible }]"
          @focusin="isSearchFocused = true"
          @focusout="handleSearchFocusOut"
        >
          <div class="search-row">
            <input
              ref="searchInputRef"
              v-model="state.filters.keyword"
              class="search-input"
              :placeholder="searchPlaceholder"
              @focus="openSearchPanel"
              @click="openSearchPanel"
              @input="openSearchPanel"
              @keyup.enter="searchGoods"
            />
            <button class="search-btn" @click="searchGoods">搜索</button>
          </div>
          <div class="search-suggest-panel">
            <div class="search-suggest-title">热门搜索</div>
            <div class="search-suggest-list">
              <button v-for="word in hotKeywords" :key="word" class="hot-word" @click="prefillKeyword(word)">
                {{ word }}
              </button>
            </div>
            <div class="search-suggest-title category-title">分类列表</div>
            <div class="search-suggest-list">
              <button
                v-for="category in categoryOptions"
                :key="category"
                :class="['hot-word', 'category-word', { active: currentCategoryLabel === category }]"
                @click="prefillCategory(category)"
              >
                {{ category }}
              </button>
            </div>
          </div>
        </div>

        <div class="user-entry">
          <template v-if="state.token">
            <div class="user-entry-main">
              <RouterLink class="user-avatar-link" to="/profile">
                <img v-if="headerAvatarUrl" :src="headerAvatarUrl" :alt="headerDisplayName" class="user-avatar-image" />
                <span v-else class="user-avatar-fallback">{{ headerInitial }}</span>
              </RouterLink>
              <div class="user-entry-copy">
                <p class="user-greet">你好，{{ headerDisplayName }}</p>
                <div class="user-badges">
                  <span>{{ isAdmin ? '管理员' : '学生用户' }}</span>
                  <span>{{ state.profile.campusVerified ? '已认证' : '待认证' }}</span>
                  <span>信用 {{ state.profile.creditScore ?? '-' }}</span>
                </div>
              </div>
              <button class="user-exit-btn" @click="handleLogout">退出</button>
            </div>
            <RouterLink class="user-profile-btn link-btn" to="/profile">个人中心</RouterLink>
          </template>
          <template v-else>
            <div class="user-entry-main guest">
              <span class="user-avatar-fallback guest-avatar">登</span>
              <div class="user-entry-copy">
                <p class="user-greet">你好，请登录</p>
                <div class="user-badges">
                  <span>浏览商品</span>
                  <span>登录后下单</span>
                </div>
              </div>
            </div>
            <button class="search-btn small-btn user-entry-login-btn" @click="router.push('/profile')">立即登录</button>
          </template>
        </div>
      </div>
    </header>

    <nav class="main-nav">
      <div class="page-container nav-inner">
        <RouterLink
          v-for="tab in tabs"
          :key="tab.to"
          :to="tab.to"
          class="nav-item"
          active-class="active"
        >
          <span>{{ tab.label }}</span>
          <span v-if="tab.badge" class="nav-badge">{{ tab.badge }}</span>
        </RouterLink>
      </div>
    </nav>

    <main class="page-container main-content">
      <div v-if="state.message.text" class="message-banner" :class="state.message.type">
        {{ state.message.text }}
      </div>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useMarketplace } from './composables/useMarketplace'

const hotKeywords = ['iPad', '考研资料', '宿舍台灯', '小冰箱', '电动车']
const rotatingSearchHints = [
  '搜川大教材、平板、台灯、自行车',
  '看看望江校区有没有便宜显示器',
  '搜江安面交的考研资料和耳机',
  '找宿舍小冰箱、路由器、收纳架',
  '试试搜索二手平板、球拍、打印机'
]

const router = useRouter()
const { state, isAdmin, cartCount, categoryOptions, ensureBootstrapped, loadItems, checkHealth, logout, setMessage } = useMarketplace()
const showHotSearch = ref(false)
const isSearchFocused = ref(false)
const searchBlockRef = ref(null)
const searchInputRef = ref(null)
const rotatingHintIndex = ref(0)
let rotatingHintTimer = null
const currentCategoryLabel = computed(() => state.filters.category || '全部商品')
const panelVisible = computed(() => showHotSearch.value || isSearchFocused.value)
const searchPlaceholder = computed(() => rotatingSearchHints[rotatingHintIndex.value] || rotatingSearchHints[0])
const headerDisplayName = computed(() => state.profile.nickname || state.currentUser.nickname || '校园用户')
const headerAvatarUrl = computed(() => state.profile.avatarUrl || state.currentUser.avatarUrl || '')
const headerInitial = computed(() => {
  const name = headerDisplayName.value.trim()
  return name ? name.slice(0, 1).toUpperCase() : '旧'
})
const tabs = computed(() => {
  if (isAdmin.value) {
    return [
      { to: '/', label: '首页' },
      { to: '/items', label: '商品' },
      { to: '/messages', label: '消息' },
      { to: '/admin', label: '审核' },
      { to: '/profile', label: '我的' }
    ]
  }
  const baseTabs = [
    { to: '/', label: '首页' },
    { to: '/items', label: '商品' },
    { to: '/cart', label: '购物车', badge: cartCount.value ? String(cartCount.value) : '' },
    { to: '/publish', label: '发布' },
    { to: '/orders', label: '订单' },
    { to: '/messages', label: '消息' },
    { to: '/profile', label: '我的' }
  ]
  return baseTabs
})

function prefillKeyword(word) {
  state.filters.keyword = word
  showHotSearch.value = false
  searchGoods()
}

function openSearchPanel() {
  showHotSearch.value = true
}

function handleSearchFocusOut() {
  requestAnimationFrame(() => {
    isSearchFocused.value = searchBlockRef.value?.contains(document.activeElement) ?? false
    if (!isSearchFocused.value) {
      showHotSearch.value = false
    }
  })
}

function prefillCategory(category) {
  state.filters.category = category === '全部商品' ? '' : category
  showHotSearch.value = false
  searchGoods()
}

async function searchGoods() {
  showHotSearch.value = false
  const query = buildItemQuery()
  await router.push({ path: '/items', query })
  await loadItems({ campus: query.campus || '' })
}

function buildItemQuery() {
  const query = {}
  if (state.filters.keyword.trim()) query.keyword = state.filters.keyword.trim()
  if (state.filters.category.trim()) query.category = state.filters.category.trim()
  if (!state.filters.keyword.trim() && state.campusLocation.trim()) {
    query.campus = state.campusLocation.trim()
  }
  return query
}

async function handleHealthCheck() {
  await checkHealth()
  setMessage(state.backendOnline ? '后端服务连接正常' : '后端服务暂时不可用', state.backendOnline ? 'success' : 'error')
}

function handleLogout() {
  logout(true)
  router.push('/')
}

function handleDocumentClick(event) {
  if (!searchBlockRef.value?.contains(event.target)) {
    showHotSearch.value = false
    isSearchFocused.value = false
  }
}

onMounted(async () => {
  document.addEventListener('click', handleDocumentClick)
  startRotatingHints()
  await ensureBootstrapped()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  stopRotatingHints()
})

function startRotatingHints() {
  stopRotatingHints()
  rotatingHintTimer = window.setInterval(() => {
    if (state.filters.keyword.trim()) return
    rotatingHintIndex.value = (rotatingHintIndex.value + 1) % rotatingSearchHints.length
  }, 2500)
}

function stopRotatingHints() {
  if (rotatingHintTimer) {
    window.clearInterval(rotatingHintTimer)
    rotatingHintTimer = null
  }
}
</script>
