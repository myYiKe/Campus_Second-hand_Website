<template>
  <section class="page-stack">
    <section class="floor-card">
      <div class="floor-header">
        <div>
          <h3>我的</h3>
        </div>
      </div>

      <div v-if="!state.token" class="profile-auth-shell">
        <section class="profile-auth-stage">
          <div class="form-card profile-auth-card">
            <div class="profile-auth-tabs">
              <button :class="['auth-tab-btn', { active: authTab === 'login' }]" @click="authTab = 'login'">账号登录</button>
              <button :class="['auth-tab-btn', { active: authTab === 'register' }]" @click="authTab = 'register'">注册账号</button>
            </div>

            <div v-if="authTab === 'login'" class="profile-auth-panel">
              <div class="profile-auth-head">
                <strong>登录</strong>
              </div>

              <div class="profile-auth-form-grid">
                <label class="auth-field">
                  <span>{{ state.loginForm.loginType === 'ADMIN' ? '管理员账号' : state.loginForm.loginType === 'NICKNAME' ? '昵称' : '学号' }}</span>
                  <input
                    v-model="state.loginForm.account"
                    :placeholder="state.loginForm.loginType === 'ADMIN' ? '请输入管理员账号' : state.loginForm.loginType === 'NICKNAME' ? '请输入昵称' : '请输入已认证学号'"
                  />
                </label>
                <label class="auth-field">
                  <span>密码</span>
                  <input v-model="state.loginForm.password" type="password" placeholder="请输入密码" @keyup.enter="login" />
                </label>
              </div>

              <div class="profile-login-mode-wrap">
                <span class="login-mode-caption">选择登录方式</span>
                <div class="profile-login-mode">
                  <button
                    :class="['mini-outline', { active: state.loginForm.loginType === 'NICKNAME' }]"
                    @click="state.loginForm.loginType = 'NICKNAME'"
                  >
                    昵称登录
                  </button>
                  <button
                    :class="['mini-outline', { active: state.loginForm.loginType === 'STUDENT_NO' }]"
                    @click="state.loginForm.loginType = 'STUDENT_NO'"
                  >
                    学号登录
                  </button>
                  <button
                    :class="['mini-outline', { active: state.loginForm.loginType === 'ADMIN' }]"
                    @click="state.loginForm.loginType = 'ADMIN'"
                  >
                    管理员登录
                  </button>
                </div>
              </div>

              <div class="profile-auth-actions">
                <button class="search-btn full-btn" :disabled="state.loading.login" @click="login">
                  {{ state.loading.login ? '登录中...' : '立即登录' }}
                </button>
              </div>

              <div class="third-party-login-panel">
                <span class="third-party-title">第三方登录方式</span>
                <div class="third-party-login-grid">
                  <RouterLink class="third-party-login-btn wechat link-btn" to="/wechat-auth">微信登录</RouterLink>
                  <button class="third-party-login-btn campus" @click="setMessage('学工一体化登录入口预留中', 'info')">
                    学工一体化登录
                  </button>
                </div>
              </div>
            </div>

            <div v-else class="profile-auth-panel">
              <div class="profile-auth-head">
                <span class="auth-panel-tag">网页端注册</span>
                <strong>快速创建测试账号</strong>
                <p class="muted">注册只需要昵称和密码，后续可在个人中心完成校园认证。</p>
              </div>

              <div class="profile-auth-form-grid">
                <label class="auth-field">
                  <span>昵称</span>
                  <input v-model="state.registerForm.nickname" placeholder="请输入 2-20 位昵称" />
                </label>
                <p v-if="registerNicknameStatus.text" :class="['nickname-status', registerNicknameStatus.type]">
                  {{ registerNicknameStatus.text }}
                </p>
                <label class="auth-field">
                  <span>密码</span>
                  <input v-model="state.registerForm.password" type="password" placeholder="请输入 6-20 位密码" />
                </label>
                <label class="auth-field">
                  <span>确认密码</span>
                  <input v-model="state.registerForm.confirmPassword" type="password" placeholder="请再次输入密码" @keyup.enter="register" />
                </label>
              </div>

              <div class="profile-auth-actions">
                <button class="search-btn full-btn" :disabled="state.loading.register" @click="register">
                  {{ state.loading.register ? '注册中...' : '注册并登录' }}
                </button>
              </div>
            </div>
          </div>
        </section>
      </div>

      <template v-else>
        <section class="profile-overview-card">
          <div class="profile-overview-main">
            <div class="profile-overview-avatar">
              <img v-if="avatarUrl" :src="avatarUrl" :alt="currentNickname || '校园用户'" class="profile-overview-avatar-image" />
              <span v-else>{{ avatarInitial }}</span>
            </div>
            <div class="profile-overview-copy">
              <strong>{{ currentNickname || '校园用户' }}</strong>
              <p>{{ profileOverviewSubtitle }}</p>
              <div class="profile-overview-stats">
                <div v-for="item in overviewStats" :key="item.label" class="profile-overview-stat">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </div>
          </div>
          <div class="profile-overview-actions">
            <input ref="avatarInputRef" class="hidden-file-input" type="file" accept=".jpg,.jpeg,.png,.webp" @change="handleAvatarChange" />
            <button class="mini-outline" :disabled="state.loading.avatarSave" @click="triggerAvatarSelect">
              {{ state.loading.avatarSave ? '上传中...' : '上传头像' }}
            </button>
          </div>
        </section>

        <div v-if="!isAdmin" class="verify-status-card" :class="state.profile.campusVerified ? 'verified' : 'pending'">
          <div>
            <strong>{{ state.profile.campusVerified ? '川大认证已完成' : '暂未完成川大认证' }}</strong>
            <p>{{ state.profile.campusVerified ? `已认证校区：${state.profile.campus || '未填写'}` : '认证后可使用学号登录和发布下单等功能' }}</p>
          </div>
          <div class="verify-status-meta">
            <span>学号：{{ state.profile.studentNo || '未提交' }}</span>
            <span>认证时间：{{ formatTime(state.profile.verifiedAt) }}</span>
          </div>
        </div>

        <div class="profile-section-list">
          <section class="profile-section-block">
            <div class="profile-section-head">
              <strong>快捷入口</strong>
              <span>常用页面快速直达</span>
            </div>
            <div class="profile-shortcut-grid">
              <RouterLink v-for="entry in quickEntries" :key="entry.to" class="profile-shortcut-card link-btn" :to="entry.to">
                <strong>{{ entry.title }}</strong>
              </RouterLink>
            </div>
          </section>

          <section v-for="group in groupedProfileEntries" :key="group.title" class="profile-section-block">
            <div class="profile-section-head">
              <strong>{{ group.title }}</strong>
              <span>{{ group.description }}</span>
            </div>
            <div class="profile-entry-grid">
              <button v-for="entry in group.items" :key="entry.key" class="profile-entry-card" @click="openModal(entry.key)">
                <strong>{{ entry.title }}</strong>
              </button>
            </div>
          </section>
        </div>
      </template>
    </section>

    <div v-if="state.token && activeModal" class="profile-modal-backdrop" @click.self="closeModal">
      <section class="profile-modal-card">
        <div class="floor-header profile-modal-header">
          <div>
            <h3>{{ currentModal?.title }}</h3>
          </div>
          <button class="outline-btn" @click="closeModal">关闭</button>
        </div>

        <div v-if="activeModal === 'userInfo'" class="list-stack compact">
          <div class="list-card">
            <div>
              <strong>{{ currentNickname }}</strong>
              <p class="muted">账号状态：{{ state.profile.status || 'ACTIVE' }} · 角色：{{ isAdmin ? '管理员' : '普通用户' }}</p>
            </div>
            <RouterLink class="mini-outline link-btn" to="/messages" @click="closeModal">去消息</RouterLink>
          </div>
          <div v-if="!isAdmin" class="list-card">
            <div>
              <strong>{{ state.profile.campus || '未认证校区' }}</strong>
              <p class="muted">认证状态：{{ state.profile.campusVerified ? '已完成川大认证' : '待认证' }}</p>
            </div>
            <RouterLink class="mini-outline link-btn" to="/orders" @click="closeModal">查看订单</RouterLink>
          </div>
          <div v-else class="list-card">
            <div>
              <strong>管理员控制台</strong>
              <p class="muted">可进入后台查看统计数据、审核商品和管理用户状态。</p>
            </div>
            <RouterLink class="mini-outline link-btn" to="/admin" @click="closeModal">进入后台</RouterLink>
          </div>
        </div>

        <div v-else-if="activeModal === 'nickname'" class="form-card compact-form-card profile-modal-body">
          <label>
            <span>新昵称</span>
            <input v-model="state.profileNicknameForm.nickname" placeholder="请输入新的昵称" />
          </label>
          <p v-if="profileNicknameStatus.text" :class="['nickname-status', profileNicknameStatus.type]">
            {{ profileNicknameStatus.text }}
          </p>
          <button class="search-btn" :disabled="state.loading.profileSave" @click="handleUpdateNickname">
            {{ state.loading.profileSave ? '保存中...' : '保存昵称' }}
          </button>
        </div>

        <div v-else-if="activeModal === 'password'" class="form-card compact-form-card profile-modal-body">
          <label>
            <span>原密码</span>
            <input v-model="state.passwordForm.oldPassword" type="password" placeholder="请输入原密码" />
          </label>
          <label>
            <span>新密码</span>
            <input v-model="state.passwordForm.newPassword" type="password" placeholder="请输入新密码" />
          </label>
          <label>
            <span>确认新密码</span>
            <input v-model="state.passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
          </label>
          <button class="search-btn" :disabled="state.loading.passwordSave" @click="changePassword">
            {{ state.loading.passwordSave ? '保存中...' : '修改密码' }}
          </button>
        </div>

        <div v-else-if="activeModal === 'verify'" class="form-card compact-form-card profile-modal-body">
          <label>
            <span>学号</span>
            <input v-model="state.verifyForm.studentNo" placeholder="例如 20230001" />
          </label>
          <label>
            <span>校区</span>
            <select v-model="state.verifyForm.campus">
              <option v-for="campus in campusOptions" :key="campus" :value="campus">
                {{ campus }}
              </option>
            </select>
          </label>
          <button class="search-btn" :disabled="state.loading.verify" @click="verifyStudent">
            {{ state.loading.verify ? '提交中...' : state.profile.campusVerified ? '更新认证信息' : '提交认证' }}
          </button>
        </div>

        <div v-else-if="activeModal === 'orders'" class="list-stack compact">
          <div class="modal-actions">
            <button class="outline-btn" :disabled="!state.token" @click="loadOrders()">刷新</button>
            <RouterLink class="mini-outline link-btn" to="/orders" @click="closeModal">进入订单页</RouterLink>
          </div>
          <div v-for="order in state.orders.slice(0, 6)" :key="order.id" class="list-card">
            <div>
              <RouterLink v-if="order.itemId" class="goods-title-link" :to="`/item/${order.itemId}`" @click="closeModal">
                <strong>{{ order.itemTitle }}</strong>
              </RouterLink>
              <strong v-else>{{ order.itemTitle }}</strong>
              <p class="muted">状态：{{ order.status }} · 金额：￥{{ order.amount }}</p>
            </div>
            <RouterLink class="mini-outline link-btn" to="/orders" @click="closeModal">去处理</RouterLink>
          </div>
          <div v-if="!state.orders.length" class="empty-inline">你还没有订单记录。</div>
        </div>

        <div v-else-if="activeModal === 'address'" class="form-card compact-form-card profile-modal-body">
          <label>
            <span>收货人</span>
            <input v-model="state.addressForm.receiver" placeholder="例如 张同学" />
          </label>
          <label>
            <span>手机号</span>
            <input v-model="state.addressForm.phone" placeholder="例如 13800000000" />
          </label>
          <label>
            <span>校区</span>
            <select v-model="state.addressForm.campus">
              <option v-for="campus in campusOptions" :key="campus" :value="campus">
                {{ campus }}
              </option>
            </select>
          </label>
          <label>
            <span>详细地址</span>
            <textarea v-model="state.addressForm.detail" rows="3" placeholder="例如 四川大学江安校区女生宿舍 12 舍"></textarea>
          </label>
          <button class="search-btn" @click="saveAddress">保存地址</button>
        </div>

        <div v-else-if="activeModal === 'settings'" class="form-card compact-form-card profile-modal-body">
          <label class="switch-row">
            <span>订单通知</span>
            <input v-model="state.settingsForm.orderNotice" type="checkbox" />
          </label>
          <label class="switch-row">
            <span>消息通知</span>
            <input v-model="state.settingsForm.messageNotice" type="checkbox" />
          </label>
          <label class="switch-row">
            <span>深色模式预留</span>
            <input v-model="state.settingsForm.darkMode" type="checkbox" />
          </label>
          <button class="search-btn" @click="saveSettings">保存设置</button>
        </div>

        <div v-else-if="activeModal === 'favorites'" class="list-stack compact">
          <div class="modal-actions">
            <button class="outline-btn" :disabled="!state.token" @click="loadFavoritesAndHistory">刷新</button>
          </div>
          <div v-if="state.favorites.length" class="favorite-card-grid">
            <article
              v-for="favorite in state.favorites.slice(0, 6)"
              :key="favorite.id"
              class="favorite-product-card"
              @click="openItemDetail(favorite.itemId)"
            >
              <div class="favorite-product-cover">
                <img v-if="resolveItemCover(favorite.itemId, favorite.imageUrl)" :src="resolveItemCover(favorite.itemId, favorite.imageUrl)" :alt="favorite.title" />
                <span v-else>校园二手</span>
              </div>
              <div class="favorite-product-body">
                <strong>{{ favorite.title }}</strong>
                <p class="muted">￥{{ favorite.price }} · {{ resolveItemCategory(favorite.itemId, favorite.category) }}</p>
                <div class="favorite-product-actions">
                  <button class="mini-outline" @click.stop="removeFavorite(favorite.itemId)">取消收藏</button>
                </div>
              </div>
            </article>
          </div>
          <div v-if="groupedHistory.length" class="history-group-list">
            <section v-for="group in groupedHistory" :key="group.label" class="history-group-section">
              <div class="search-suggest-title">{{ group.label }}</div>
              <div class="history-card-list">
                <article
                  v-for="history in group.items"
                  :key="history.id || history.itemId || history.browsedAt"
                  class="history-product-card"
                  @click="openHistoryTarget(history.itemId)"
                >
                  <div class="favorite-product-cover history-product-cover">
                    <img v-if="resolveHistoryCover(history)" :src="resolveHistoryCover(history)" :alt="history.title || history.itemTitle || '最近浏览'" />
                    <span v-else>校园二手</span>
                  </div>
                  <div class="history-product-main">
                    <strong>{{ history.title || history.itemTitle || '最近浏览' }}</strong>
                    <p class="muted">{{ resolveHistoryCategory(history) }}</p>
                  </div>
                  <span class="history-product-time">{{ formatHistoryTime(history.browsedAt || history.createdAt) }}</span>
                </article>
              </div>
            </section>
          </div>
          <div v-if="!state.favorites.length && !state.history.length" class="empty-inline">你还没有收藏或浏览记录。</div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useMarketplace } from '../composables/useMarketplace'

const router = useRouter()
const {
  state,
  isAdmin,
  login,
  register,
  checkNicknameAvailability,
  updateNickname,
  changePassword,
  loadProfile,
  loadOrders,
  loadMyItems,
  fetchItemDetail,
  verifyStudent,
  loadFavoritesAndHistory,
  removeFavorite,
  saveAddress,
  saveSettings,
  uploadProfileAvatar,
  setMessage
} = useMarketplace()

const campusOptions = ['望江校区', '江安校区']
const authTab = ref('login')
const activeModal = ref('')
const avatarInputRef = ref(null)
const registerNicknameStatus = ref({ text: '', type: 'info' })
const profileNicknameStatus = ref({ text: '', type: 'info' })
const currentNickname = computed(() => state.profile.nickname || state.currentUser.nickname || '')
const avatarUrl = computed(() => state.profile.avatarUrl || state.currentUser.avatarUrl || '')
const avatarInitial = computed(() => {
  const name = currentNickname.value.trim()
  return name ? name.slice(0, 1).toUpperCase() : '旧'
})
const profileOverviewSubtitle = computed(() => {
  if (isAdmin.value) {
    return `管理员账号 · 当前状态 ${state.profile.status || 'ACTIVE'}`
  }
  return `校园交易用户 · ${state.profile.campusVerified ? '已完成校园认证' : '未完成校园认证'}`
})
const overviewStats = computed(() => {
  if (isAdmin.value) {
    return [
      { label: '昵称', value: currentNickname.value || '-' },
      { label: '账号角色', value: '管理员' },
      { label: '账号状态', value: state.profile.status || 'ACTIVE' },
      { label: '发布数', value: publishedItemCount.value }
    ]
  }
  return [
    { label: '昵称', value: currentNickname.value || '-' },
    { label: '校园认证', value: state.profile.campusVerified ? '已认证' : '未认证' },
    { label: '收藏数', value: state.profile.favoriteCount ?? 0 },
    { label: '发布数', value: publishedItemCount.value }
  ]
})
const quickEntries = computed(() => {
  if (isAdmin.value) {
    return [
      { to: '/admin', title: '管理员端' },
      { to: '/messages', title: '消息中心' },
      { to: '/items', title: '商品列表' },
      { to: '/profile', title: '我的资料' }
    ]
  }
  const baseEntries = [
    { to: '/cart', title: '购物车' },
    { to: '/orders', title: '订单' },
    { to: '/messages', title: '消息' },
    { to: '/profile', title: '我的' }
  ]
  return baseEntries
})
const profileEntries = [
  { key: 'userInfo', title: '用户信息' },
  { key: 'nickname', title: '修改昵称' },
  { key: 'password', title: '修改密码' },
  { key: 'verify', title: '校园认证' },
  { key: 'orders', title: '我的订单' },
  { key: 'address', title: '收货地址' },
  { key: 'settings', title: '页面设置' },
  { key: 'favorites', title: '收藏足迹' }
]
const groupedProfileEntries = computed(() => {
  if (isAdmin.value) {
    return [
      {
        title: '账号设置',
        description: '维护管理员资料与登录安全',
        items: [
          { key: 'userInfo', title: '用户信息' },
          { key: 'nickname', title: '修改昵称' },
          { key: 'password', title: '修改密码' },
          { key: 'settings', title: '页面设置' }
        ]
      }
    ]
  }
  return [
    {
      title: '账号设置',
      description: '维护个人资料与账号安全',
      items: [
        { key: 'userInfo', title: '用户信息' },
        { key: 'nickname', title: '修改昵称' },
        { key: 'password', title: '修改密码' },
        { key: 'verify', title: '校园认证' }
      ]
    },
    {
      title: '交易与服务',
      description: '处理订单、地址与使用记录',
      items: [
        { key: 'orders', title: '我的订单' },
        { key: 'address', title: '收货地址' },
        { key: 'favorites', title: '收藏足迹' },
        { key: 'settings', title: '页面设置' }
      ]
    }
  ]
})
const currentModal = computed(() => profileEntries.find(entry => entry.key === activeModal.value) || null)
const itemLookup = computed(() => Object.fromEntries(state.items.map(item => [Number(item.id), item])))
const publishedItemCount = computed(() => state.myItems.filter(item => item.status !== 'DELETED').length)
const groupedHistory = computed(() => {
  const groups = new Map()
  state.history.forEach(entry => {
    const rawTime = entry.browsedAt || entry.createdAt
    const date = rawTime ? new Date(rawTime) : null
    const label = date ? date.toLocaleDateString('zh-CN') : '更早'
    if (!groups.has(label)) {
      groups.set(label, [])
    }
    groups.get(label).push(entry)
  })
  return Array.from(groups.entries()).map(([label, items]) => ({ label, items: items.slice(0, 6) }))
})

let registerTimer = 0
let profileTimer = 0

onMounted(async () => {
  if (state.token) {
    await Promise.all([loadProfile(), loadFavoritesAndHistory(), loadOrders(), loadMyItems()])
  }
})

watch(
  () => state.registerForm.nickname,
  value => {
    window.clearTimeout(registerTimer)
    const nickname = String(value || '').trim()
    if (!nickname) {
      registerNicknameStatus.value = { text: '', type: 'info' }
      return
    }
    if (nickname.length < 2) {
      registerNicknameStatus.value = { text: '昵称至少需要 2 个字符', type: 'error' }
      return
    }
    registerNicknameStatus.value = { text: '正在检查昵称可用性...', type: 'info' }
    registerTimer = window.setTimeout(async () => {
      const data = await checkNicknameAvailability(nickname, { silent: true })
      if (state.registerForm.nickname.trim() !== nickname) return
      registerNicknameStatus.value = data.available
        ? { text: '该昵称可用', type: 'success' }
        : { text: '该昵称已被占用', type: 'error' }
    }, 300)
  }
)

watch(
  () => state.profileNicknameForm.nickname,
  value => {
    window.clearTimeout(profileTimer)
    if (!state.token) return
    const nickname = String(value || '').trim()
    if (!nickname) {
      profileNicknameStatus.value = { text: '', type: 'info' }
      return
    }
    if (nickname === currentNickname.value) {
      profileNicknameStatus.value = { text: '当前昵称无需修改', type: 'info' }
      return
    }
    if (nickname.length < 2) {
      profileNicknameStatus.value = { text: '昵称至少需要 2 个字符', type: 'error' }
      return
    }
    profileNicknameStatus.value = { text: '正在检查昵称可用性...', type: 'info' }
    profileTimer = window.setTimeout(async () => {
      const data = await checkNicknameAvailability(nickname, { silent: true })
      if (state.profileNicknameForm.nickname.trim() !== nickname) return
      profileNicknameStatus.value = data.available
        ? { text: '该昵称可用', type: 'success' }
        : { text: '该昵称已被占用', type: 'error' }
    }, 300)
  }
)

function formatTime(value) {
  if (!value) return '未认证'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function openModal(key) {
  activeModal.value = key
  if (key === 'nickname') {
    state.profileNicknameForm.nickname = currentNickname.value
    profileNicknameStatus.value = { text: '当前昵称无需修改', type: 'info' }
  }
}

function closeModal() {
  activeModal.value = ''
}

function triggerAvatarSelect() {
  avatarInputRef.value?.click()
}

async function handleAvatarChange(event) {
  const input = event.target
  const [file] = input?.files || []
  if (!file) {
    return
  }
  const updated = await uploadProfileAvatar(file)
  if (updated) {
    await loadProfile()
  }
  input.value = ''
}

async function handleUpdateNickname() {
  const changed = await updateNickname()
  if (changed) {
    closeModal()
  }
}

async function openItemDetail(itemId) {
  closeModal()
  try {
    await fetchItemDetail(itemId)
    router.push(`/item/${itemId}`)
  } catch {
    setMessage('该商品已失效，已为你返回商品页', 'info')
    router.push('/items')
  }
}

async function openHistoryTarget(itemId) {
  closeModal()
  if (!itemId) {
    router.push('/items')
    return
  }
  try {
    await fetchItemDetail(itemId)
    router.push(`/item/${itemId}`)
  } catch {
    setMessage('该商品已失效，已为你返回商品页', 'info')
    router.push('/items')
  }
}

function resolveItemCover(itemId, fallback = '') {
  const matched = itemLookup.value[Number(itemId)]
  return matched?.imageUrl || fallback || ''
}

function resolveItemCategory(itemId, fallback = '') {
  const matched = itemLookup.value[Number(itemId)]
  return matched?.category || fallback || '未分类'
}

function resolveHistoryCover(history) {
  return history.imageUrl || resolveItemCover(history.itemId) || ''
}

function resolveHistoryCategory(history) {
  return history.category || resolveItemCategory(history.itemId) || '校园商品'
}

function formatHistoryTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleTimeString('zh-CN', { hour12: false })
}
</script>
