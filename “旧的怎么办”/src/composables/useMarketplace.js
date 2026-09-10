import { computed, reactive } from 'vue'

const API_BASE = 'http://localhost:8080/api'
const STATIC_BASE = 'http://localhost:8080'
const TOKEN_KEY = 'campus_trade_token'
const USER_KEY = 'campus_trade_user'
const ADDRESS_KEY = 'campus_trade_address'
const SETTINGS_KEY = 'campus_trade_settings'
const CART_KEY = 'campus_trade_cart'
const CHAT_HISTORY_KEY = 'campus_trade_chat_history'
const DEFAULT_CATEGORY_OPTIONS = [
  '数码设备',
  '学习资料',
  '宿舍电器',
  '出行代步',
  '家居用品',
  '服饰鞋包',
  '文体乐器',
  '美妆个护',
  '办公配件',
  '票券周边'
]

const state = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  currentUser: readStoredUser(),
  campusLocation: '望江校区',
  backendOnline: false,
  items: [],
  orders: [],
  reviews: [],
  disputes: [],
  favorites: [],
  cart: readStoredJson(CART_KEY, []),
  chatHistory: readStoredJson(CHAT_HISTORY_KEY, []),
  history: [],
  wantedPosts: [],
  myItems: [],
  adminUsers: [],
  adminReports: [],
  adminDisputes: [],
  profile: {},
  dashboard: {},
  loading: {
    login: false,
    register: false,
    nicknameCheck: false,
    items: false,
    orders: false,
    favorites: false,
    reviewHistory: false,
    disputeHistory: false,
    profile: false,
    profileSave: false,
    avatarSave: false,
    passwordSave: false,
    publish: false,
    review: false,
    dispute: false,
    wanted: false,
    wantedSubmit: false,
    verify: false,
    myItems: false,
    messages: false,
    admin: false,
    audit: false,
    orderAction: false,
    disputeHandle: false
  },
  message: {
    text: '',
    type: 'info'
  },
  loginForm: {
    account: '',
    password: '',
    loginType: 'NICKNAME'
  },
  registerForm: {
    nickname: '',
    password: '',
    confirmPassword: ''
  },
  profileNicknameForm: {
    nickname: ''
  },
  passwordForm: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  },
  verifyForm: {
    studentNo: '20230001',
    campus: '望江校区'
  },
  addressForm: readStoredJson(ADDRESS_KEY, {
    receiver: '张同学',
    phone: '13800000000',
    campus: '望江校区',
    detail: '四川大学望江校区东园宿舍 3 舍'
  }),
  settingsForm: readStoredJson(SETTINGS_KEY, {
    orderNotice: true,
    messageNotice: true,
    darkMode: false
  }),
  publishForm: {
    title: '',
    category: '数码设备',
    price: '',
    stock: 1,
    description: '',
    imageUrls: []
  },
  reviewForm: {
    orderId: '',
    score: 5,
    content: ''
  },
  disputeForm: {
    orderId: '',
    reason: ''
  },
  wantedForm: {
    title: '',
    budget: '',
    description: ''
  },
  adminAuditForm: {
    itemId: '',
    result: 'APPROVED',
    reason: '',
    userAction: 'NONE'
  },
  filters: {
    keyword: '',
    category: ''
  }
})

function readStoredUser() {
  const stored = readStoredJson(USER_KEY, {})
  return {
    ...stored,
    avatarUrl: normalizeImageUrl(stored.avatarUrl)
  }
}

function readStoredJson(key, fallback) {
  const raw = localStorage.getItem(key)
  if (!raw) return fallback
  try {
    return JSON.parse(raw)
  } catch {
    return fallback
  }
}

function persistUser() {
  localStorage.setItem(USER_KEY, JSON.stringify(state.currentUser))
}

function persistCart() {
  localStorage.setItem(CART_KEY, JSON.stringify(state.cart))
}

function persistChatHistory() {
  localStorage.setItem(CHAT_HISTORY_KEY, JSON.stringify(state.chatHistory))
}

function setMessage(text, type = 'info') {
  state.message.text = text
  state.message.type = type
  window.clearTimeout(setMessage.timer)
  setMessage.timer = window.setTimeout(() => {
    state.message.text = ''
  }, 3000)
}

function normalizeImageUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  return `${STATIC_BASE}${url}`
}

function normalizeItem(item) {
  const rawImageUrls = Array.isArray(item.imageUrls)
    ? item.imageUrls
    : typeof item.imageUrls === 'string' && item.imageUrls
      ? item.imageUrls.split('||')
      : []
  const imageUrls = rawImageUrls.map(normalizeImageUrl).filter(Boolean)
  const imageUrl = normalizeImageUrl(item.imageUrl) || imageUrls[0] || ''
  return {
    ...item,
    sellerId: Number(item.sellerId ?? 0),
    stock: Number(item.stock ?? 0),
    favoriteCount: Number(item.favoriteCount ?? 0),
    imageUrl,
    imageUrls: imageUrl && imageUrls.length === 0 ? [imageUrl] : imageUrls
  }
}

function normalizeWantedPost(post) {
  return {
    ...post,
    publisherName: post.publisherName || post.publisher || '校园用户'
  }
}

function normalizeOrder(order) {
  const createdAt = order.createdAt || order.created_at || ''
  const displayDate = createdAt ? new Date(createdAt) : null
  const compactDate = displayDate && !Number.isNaN(displayDate.getTime())
    ? `${displayDate.getFullYear()}${String(displayDate.getMonth() + 1).padStart(2, '0')}${String(displayDate.getDate()).padStart(2, '0')}`
    : '00000000'
  return {
    ...order,
    id: Number(order.id ?? 0),
    itemId: Number(order.itemId ?? 0),
    buyerId: Number(order.buyerId ?? 0),
    sellerId: Number(order.sellerId ?? 0),
    amount: Number(order.amount ?? 0),
    paymentRemainingSeconds: Number(order.paymentRemainingSeconds ?? 0),
    orderNo: order.orderNo || `CT${compactDate}${String(order.id ?? '').padStart(6, '0')}`
  }
}

function normalizeCartItem(item) {
  const normalized = normalizeItem(item)
  return {
    id: Number(normalized.id),
    title: normalized.title,
    price: Number(normalized.price),
    category: normalized.category,
    sellerId: Number(normalized.sellerId ?? 0),
    sellerName: normalized.sellerName,
    imageUrl: normalized.imageUrl,
    imageUrls: normalized.imageUrls,
    description: normalized.description,
    stock: Number(normalized.stock ?? 0),
    status: normalized.status,
    publishTime: normalized.publishTime,
    createdAt: normalized.createdAt
  }
}

function normalizeMessageRecord(entry) {
  const normalizedMessageType = String(entry.messageType || 'TEXT').toUpperCase()
  return {
    ...entry,
    id: entry.id ?? `msg-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    orderId: entry.orderId ? Number(entry.orderId) : null,
    itemId: entry.itemId ? Number(entry.itemId) : null,
    fromUserId: entry.fromUserId ? Number(entry.fromUserId) : null,
    toUserId: entry.toUserId ? Number(entry.toUserId) : null,
    createdAt: entry.createdAt || new Date().toISOString(),
    messageType: normalizedMessageType,
    entryType: normalizedMessageType === 'TEXT'
      ? 'chat'
      : normalizedMessageType === 'REVIEW'
        ? 'review-feedback'
        : normalizedMessageType === 'DISPUTE'
          ? 'dispute-feedback'
          : normalizedMessageType.toLowerCase()
  }
}

function isInvalidPublicItemError(error) {
  return /商品不存在|商品已下架或不存在|ITEM_NOT_FOUND/i.test(String(error?.message || error || ''))
}

async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  })

  let payload = null
  try {
    payload = await response.json()
  } catch {
    payload = null
  }

  if (!response.ok || payload?.success === false) {
    if (response.status === 401) {
      logout(false)
    }
    throw new Error(payload?.message || `请求失败: ${response.status}`)
  }

  return payload?.data
}

function applyAuthPayload(data) {
  state.token = data.token
  state.currentUser = {
    userId: data.userId,
    nickname: data.nickname,
    avatarUrl: normalizeImageUrl(data.avatarUrl),
    isAdmin: Boolean(data.isAdmin)
  }
  localStorage.setItem(TOKEN_KEY, data.token)
  persistUser()
}

async function requestForm(path, formData, options = {}) {
  const headers = {
    ...(options.headers || {})
  }
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    method: options.method || 'POST',
    headers,
    body: formData
  })

  let payload = null
  try {
    payload = await response.json()
  } catch {
    payload = null
  }

  if (!response.ok || payload?.success === false) {
    if (response.status === 401) {
      logout(false)
    }
    throw new Error(payload?.message || `请求失败: ${response.status}`)
  }

  return payload?.data
}

async function checkHealth() {
  try {
    const response = await fetch(`${API_BASE}/health`)
    state.backendOnline = response.ok
  } catch {
    state.backendOnline = false
  }
}

async function loadPublicData() {
  await Promise.all([loadItems(), loadWanted()])
}

async function login() {
  if (!state.loginForm.account.trim() || !state.loginForm.password.trim()) {
    setMessage('请输入账号和密码', 'error')
    return false
  }
  state.loading.login = true
  try {
    const data = await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        account: state.loginForm.account.trim(),
        password: state.loginForm.password.trim(),
        loginType: state.loginForm.loginType
      })
    })
    applyAuthPayload(data)
    setMessage(`欢迎回来，${data.nickname}`, 'success')
    await loadInitialData()
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.login = false
  }
}

async function register() {
  const { nickname, password, confirmPassword } = state.registerForm
  if (!nickname.trim() || !password.trim() || !confirmPassword.trim()) {
    setMessage('请完整填写注册信息', 'error')
    return false
  }
  if (password !== confirmPassword) {
    setMessage('两次输入的密码不一致', 'error')
    return false
  }
  state.loading.register = true
  try {
    const data = await request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        nickname: nickname.trim(),
        password: password.trim()
      })
    })
    applyAuthPayload(data)
    state.registerForm = {
      nickname: '',
      password: '',
      confirmPassword: ''
    }
    setMessage(`注册成功，欢迎你 ${data.nickname}`, 'success')
    await loadInitialData()
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.register = false
  }
}

async function checkNicknameAvailability(nickname, options = {}) {
  const normalizedNickname = String(nickname || '').trim()
  if (!normalizedNickname) {
    return { available: false, nickname: '' }
  }
  if (!options.silent) {
    state.loading.nicknameCheck = true
  }
  try {
    return await request(`/auth/nickname-available?nickname=${encodeURIComponent(normalizedNickname)}`)
  } catch (error) {
    if (!options.silent) {
      setMessage(error.message, 'error')
    }
    return { available: false, nickname: normalizedNickname }
  } finally {
    if (!options.silent) {
      state.loading.nicknameCheck = false
    }
  }
}

async function updateNickname() {
  const nickname = state.profileNicknameForm.nickname.trim()
  if (!nickname) {
    setMessage('请输入新的昵称', 'error')
    return false
  }
  state.loading.profileSave = true
  try {
    const data = await request('/profile/nickname', {
      method: 'PATCH',
      body: JSON.stringify({ nickname })
    })
    applyAuthPayload(data)
    state.profileNicknameForm.nickname = data.nickname
    setMessage('昵称修改成功', 'success')
    await Promise.all([loadProfile(), loadMessages(), loadAdminData()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.profileSave = false
  }
}

async function changePassword() {
  const { oldPassword, newPassword, confirmPassword } = state.passwordForm
  if (!oldPassword.trim() || !newPassword.trim() || !confirmPassword.trim()) {
    setMessage('请完整填写密码信息', 'error')
    return false
  }
  if (newPassword !== confirmPassword) {
    setMessage('两次输入的新密码不一致', 'error')
    return false
  }
  state.loading.passwordSave = true
  try {
    await request('/profile/password', {
      method: 'PATCH',
      body: JSON.stringify({
        oldPassword: oldPassword.trim(),
        newPassword: newPassword.trim()
      })
    })
    state.passwordForm = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
    setMessage('密码修改成功，请牢记新密码', 'success')
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.passwordSave = false
  }
}

function logout(showMessage = true) {
  state.token = ''
  state.currentUser = {}
  state.orders = []
  state.reviews = []
  state.disputes = []
  state.favorites = []
  state.cart = []
  state.history = []
  state.myItems = []
  state.adminUsers = []
  state.adminDisputes = []
  state.profile = {}
  state.dashboard = {}
  state.profileNicknameForm.nickname = ''
  state.passwordForm = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(CART_KEY)
  if (showMessage) {
    setMessage('已退出登录', 'info')
  }
  void loadPublicData()
}

async function loadInitialData() {
  const tasks = [
    loadItems(),
    loadOrders(),
    loadTradeRecords(),
    loadMessages(),
    loadFavoritesAndHistory(),
    loadProfile(),
    loadWanted(),
    loadMyItems()
  ]
  await cleanupInvalidCartItems()
  if (state.currentUser.isAdmin) {
    tasks.push(loadAdminData())
  }
  await Promise.all(tasks)
}

async function loadItems(options = {}) {
  state.loading.items = true
  try {
    const params = new URLSearchParams()
    const campus = Object.prototype.hasOwnProperty.call(options, 'campus') ? String(options.campus || '') : state.campusLocation
    if (state.filters.keyword.trim()) params.set('keyword', state.filters.keyword.trim())
    if (state.filters.category.trim()) params.set('category', state.filters.category.trim())
    if (campus.trim()) params.set('campus', campus.trim())
    const query = params.toString() ? `?${params.toString()}` : ''
    state.items = (await request(`/items${query}`)).map(normalizeItem)
    syncCartSnapshot()
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    state.loading.items = false
  }
}

async function loadOrders(status = '') {
  if (!state.token) {
    state.orders = []
    return
  }
  state.loading.orders = true
  try {
    const query = status ? `?status=${encodeURIComponent(status)}` : ''
    state.orders = (await request(`/orders${query}`)).map(normalizeOrder)
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    state.loading.orders = false
  }
}

async function loadReviewHistory() {
  if (!state.token) {
    state.reviews = []
    return
  }
  state.loading.reviewHistory = true
  try {
    state.reviews = await request('/reviews')
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    state.loading.reviewHistory = false
  }
}

async function loadDisputeHistory() {
  if (!state.token) {
    state.disputes = []
    return
  }
  state.loading.disputeHistory = true
  try {
    state.disputes = await request('/disputes')
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    state.loading.disputeHistory = false
  }
}

async function loadTradeRecords() {
  if (!state.token) {
    state.reviews = []
    state.disputes = []
    return
  }
  await Promise.all([loadReviewHistory(), loadDisputeHistory()])
}

async function loadMessages(options = {}) {
  if (!state.token) {
    state.chatHistory = []
    persistChatHistory()
    return []
  }
  state.loading.messages = true
  try {
    const params = new URLSearchParams()
    if (options.orderId) params.set('orderId', String(options.orderId))
    if (options.itemId) params.set('itemId', String(options.itemId))
    const query = params.toString() ? `?${params.toString()}` : ''
    const data = (await request(`/messages${query}`)).map(normalizeMessageRecord)
    if (!options.orderId && !options.itemId) {
      state.chatHistory = data
      persistChatHistory()
    }
    return data
  } catch (error) {
    setMessage(error.message, 'error')
    return []
  } finally {
    state.loading.messages = false
  }
}

async function loadFavoritesAndHistory() {
  if (!state.token) {
    state.favorites = []
    state.history = []
    return
  }
  state.loading.favorites = true
  try {
    const [favoriteData, historyData] = await Promise.all([
      request('/favorites'),
      request('/history')
    ])
    state.favorites = favoriteData
    state.history = historyData
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    state.loading.favorites = false
  }
}

async function loadProfile() {
  if (!state.token) {
    state.profile = {}
    state.profileNicknameForm.nickname = ''
    return
  }
  state.loading.profile = true
  try {
    const data = await request('/profile')
    state.profile = {
      ...data,
      avatarUrl: normalizeImageUrl(data.avatarUrl)
    }
    state.profileNicknameForm.nickname = data.nickname || ''
    state.currentUser = {
      ...state.currentUser,
      userId: data.userId ?? state.currentUser.userId,
      nickname: data.nickname ?? state.currentUser.nickname,
      avatarUrl: normalizeImageUrl(data.avatarUrl) || state.currentUser.avatarUrl,
      isAdmin: Boolean(data.isAdmin),
      status: data.status ?? state.currentUser.status
    }
    persistUser()
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    state.loading.profile = false
  }
}

async function loadWanted() {
  state.loading.wanted = true
  try {
    state.wantedPosts = (await request('/wanted')).map(normalizeWantedPost)
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    state.loading.wanted = false
  }
}

async function fetchItemDetail(itemId) {
  const data = await request(`/items/${itemId}`)
  return normalizeItem(data)
}

async function cleanupInvalidCartItems(options = {}) {
  if (!state.cart.length) return 0
  const { notify = false } = options
  const latestById = new Map()
  const invalidIds = new Set()
  await Promise.all(
    [...new Set(state.cart.map(item => Number(item.id)).filter(Boolean))].map(async itemId => {
      try {
        const latest = await fetchItemDetail(itemId)
        latestById.set(Number(itemId), latest)
      } catch (error) {
        if (isInvalidPublicItemError(error)) {
          invalidIds.add(Number(itemId))
        }
      }
    })
  )
  if (!invalidIds.size && !latestById.size) {
    return 0
  }
  const nextCart = state.cart
    .filter(item => !invalidIds.has(Number(item.id)))
    .map(item => {
      const latest = latestById.get(Number(item.id))
      return latest
        ? {
            ...item,
            price: Number(latest.price),
            title: latest.title,
            imageUrl: latest.imageUrl,
            imageUrls: latest.imageUrls,
            description: latest.description,
            category: latest.category,
            sellerName: latest.sellerName,
            sellerId: latest.sellerId,
            stock: Number(latest.stock ?? item.stock ?? 0),
            status: latest.status,
            publishTime: latest.publishTime,
            createdAt: latest.createdAt
          }
        : item
    })
  const removedCount = state.cart.length - nextCart.length
  state.cart = nextCart
  persistCart()
  if (removedCount > 0 && notify) {
    setMessage(`已自动清理 ${removedCount} 件失效商品`, 'info')
  }
  return removedCount
}

async function loadMyItems() {
  if (!state.token) {
    state.myItems = []
    return
  }
  state.loading.myItems = true
  try {
    state.myItems = (await request('/my/items')).map(normalizeItem)
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    state.loading.myItems = false
  }
}

async function loadAdminData() {
  if (!state.token || !isAdmin.value) {
    state.dashboard = {}
    state.adminUsers = []
    state.adminReports = []
    state.adminDisputes = []
    return
  }
  state.loading.admin = true
  try {
    const [dashboardData, userData, reportData, disputeData] = await Promise.all([
      request('/admin/dashboard'),
      request('/admin/users'),
      request('/reports'),
      request('/disputes')
    ])
    state.dashboard = dashboardData
    state.adminUsers = userData
    state.adminReports = reportData
    state.adminDisputes = disputeData
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    state.loading.admin = false
  }
}

async function toggleFavorite(item) {
  if (!state.token) {
    setMessage('请先登录后再收藏', 'error')
    return
  }
  try {
    if (favoriteIds.value.has(Number(item.id))) {
      await request(`/favorites/${item.id}`, { method: 'DELETE' })
      setMessage('已取消收藏', 'success')
    } else {
      await request('/favorites', {
        method: 'POST',
        body: JSON.stringify({ itemId: Number(item.id) })
      })
      setMessage('收藏成功', 'success')
    }
    await loadFavoritesAndHistory()
  } catch (error) {
    setMessage(error.message, 'error')
  }
}

async function removeFavorite(itemId) {
  try {
    await request(`/favorites/${itemId}`, { method: 'DELETE' })
    setMessage('已取消收藏', 'success')
    await loadFavoritesAndHistory()
  } catch (error) {
    setMessage(error.message, 'error')
  }
}

async function createOrder(item) {
  if (!state.token) {
    setMessage('请先登录后再下单', 'error')
    return
  }
  try {
    const currentUserId = Number(state.currentUser.userId || state.profile.userId || 0)
    if (currentUserId && Number(item.sellerId || 0) === currentUserId) {
      setMessage('不能下单自己发布的商品', 'error')
      return
    }
    if (Number(item.stock ?? 0) < 1 || String(item.status) === 'SOLD') {
      setMessage('该商品已售罄，暂时无法下单', 'error')
      return
    }
    const verified = await ensureCampusVerified('下单')
    if (!verified) {
      return
    }
    if (!state.addressForm.receiver.trim() || !state.addressForm.phone.trim() || !state.addressForm.detail.trim()) {
      setMessage('请先在个人中心完善收货地址后再下单', 'error')
      return
    }
    await request('/orders', {
      method: 'POST',
      body: JSON.stringify({
        itemId: Number(item.id),
        receiverName: state.addressForm.receiver.trim(),
        receiverPhone: state.addressForm.phone.trim(),
        receiverCampus: state.addressForm.campus,
        receiverDetail: state.addressForm.detail.trim()
      })
    })
    setMessage(`已为 ${item.title} 创建待支付订单`, 'success')
    await Promise.all([loadOrders(), loadItems(), loadMyItems()])
  } catch (error) {
    setMessage(error.message, 'error')
  }
}

function addToCart(item) {
  const currentUserId = Number(state.currentUser.userId || state.profile.userId || 0)
  if (currentUserId && Number(item.sellerId || 0) === currentUserId) {
    setMessage('自己的商品无需加入购物车', 'error')
    return false
  }
  if (Number(item.stock ?? 0) < 1 || String(item.status) === 'SOLD') {
    setMessage('该商品已售罄，无法加入购物车', 'error')
    return false
  }
  const cartItem = normalizeCartItem(item)
  if (state.cart.some(entry => Number(entry.id) === Number(cartItem.id))) {
    setMessage('该商品已在购物车中', 'info')
    return false
  }
  state.cart.unshift(cartItem)
  persistCart()
  setMessage(`已将 ${cartItem.title} 加入购物车`, 'success')
  return true
}

function removeFromCart(itemId) {
  state.cart = state.cart.filter(item => Number(item.id) !== Number(itemId))
  persistCart()
  setMessage('已从购物车移除', 'success')
}

function clearCart(itemIds = []) {
  if (!itemIds.length) {
    state.cart = []
  } else {
    const idSet = new Set(itemIds.map(id => Number(id)))
    state.cart = state.cart.filter(item => !idSet.has(Number(item.id)))
  }
  persistCart()
}

async function checkoutCart(itemIds = []) {
  if (!state.token) {
    setMessage('请先登录后再结算', 'error')
    return { success: false, count: 0 }
  }
  const ids = itemIds.length ? itemIds.map(id => Number(id)) : state.cart.map(item => Number(item.id))
  const candidates = state.cart.filter(item => ids.includes(Number(item.id)))
  const validItems = candidates.filter(item => Number(item.stock ?? 0) > 0 && String(item.status) !== 'SOLD')
  if (!validItems.length) {
    setMessage('购物车里没有可结算商品', 'error')
    return { success: false, count: 0 }
  }
  const verified = await ensureCampusVerified('结算购物车')
  if (!verified) {
    return { success: false, count: 0 }
  }
  if (!state.addressForm.receiver.trim() || !state.addressForm.phone.trim() || !state.addressForm.detail.trim()) {
    setMessage('请先在个人中心完善收货地址后再结算', 'error')
    return { success: false, count: 0 }
  }
  let successCount = 0
  for (const item of validItems) {
    try {
      await request('/orders', {
        method: 'POST',
        body: JSON.stringify({
          itemId: Number(item.id),
          receiverName: state.addressForm.receiver.trim(),
          receiverPhone: state.addressForm.phone.trim(),
          receiverCampus: state.addressForm.campus,
          receiverDetail: state.addressForm.detail.trim()
        })
      })
      successCount += 1
    } catch (error) {
      setMessage(error.message, 'error')
    }
  }
  if (successCount > 0) {
    clearCart(validItems.map(item => item.id))
    setMessage(`已创建 ${successCount} 笔待支付订单`, 'success')
    await Promise.all([loadOrders(), loadItems(), loadMyItems()])
    return { success: true, count: successCount }
  }
  return { success: false, count: 0 }
}

function appendChatRecord(record) {
  if (record.fromUserId && record.toUserId && Number(record.fromUserId) === Number(record.toUserId)) {
    return
  }
  state.chatHistory = [
    {
      id: record.id || `chat-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      createdAt: record.createdAt || new Date().toISOString(),
      ...record
    },
    ...state.chatHistory
  ].slice(0, 300)
  persistChatHistory()
}

function syncCartSnapshot() {
  if (!state.cart.length) return
  const itemMap = new Map(state.items.map(item => [Number(item.id), item]))
  state.cart = state.cart.map(item => {
    const latest = itemMap.get(Number(item.id))
    return latest
      ? {
          ...item,
          price: Number(latest.price),
          title: latest.title,
          imageUrl: latest.imageUrl,
          imageUrls: latest.imageUrls,
          description: latest.description,
          category: latest.category,
          sellerId: latest.sellerId,
          sellerName: latest.sellerName,
          stock: Number(latest.stock ?? item.stock ?? 0),
          status: latest.status,
          publishTime: latest.publishTime,
          createdAt: latest.createdAt
        }
      : item
  })
  persistCart()
}

async function uploadItemImages(files) {
  if (!state.token) {
    setMessage('请先登录后再上传图片', 'error')
    return []
  }
  const verified = await ensureCampusVerified('上传商品图片')
  if (!verified) {
    return []
  }
  const imageUrls = []
  for (const file of files) {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(`${API_BASE}/items/upload`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${state.token}`
      },
      body: formData
    })
    const payload = await response.json().catch(() => null)
    if (!response.ok || payload?.success === false) {
      throw new Error(payload?.message || '图片上传失败')
    }
    imageUrls.push(payload.data.imageUrl)
  }
  return imageUrls
}

async function publishItem() {
  const form = state.publishForm
  if (!form.title || !form.category || !form.price || !form.description) {
    setMessage('请完整填写商品信息', 'error')
    return false
  }
  if (!form.stock || Number(form.stock) < 1) {
    setMessage('库存至少填写 1', 'error')
    return false
  }
  const verified = await ensureCampusVerified('发布商品')
  if (!verified) {
    return false
  }
  state.loading.publish = true
  try {
    const title = form.title.trim().replace(/\s+/g, ' ')
    const description = form.description.trim().replace(/\n{3,}/g, '\n\n')
    const imageUrls = form.imageUrls.map(url => url.replace(STATIC_BASE, ''))
    await request('/items', {
      method: 'POST',
      body: JSON.stringify({
        title,
        category: form.category,
        price: Number(form.price),
        stock: Number(form.stock),
        description,
        imageUrls
      })
    })
    state.publishForm = {
      title: '',
      category: '数码设备',
      price: '',
      stock: 1,
      description: '',
      imageUrls: []
    }
    setMessage('商品已成功上架', 'success')
    await Promise.all([loadItems(), loadMyItems(), loadAdminData()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.publish = false
  }
}

function fillReview(order) {
  state.reviewForm.orderId = order.id
  state.reviewForm.content = `对商品「${order.itemTitle}」的评价：`
}

async function submitReview() {
  if (!state.reviewForm.orderId || !state.reviewForm.content) {
    setMessage('请先填写订单和评价内容', 'error')
    return false
  }
  state.loading.review = true
  try {
    const orderId = Number(state.reviewForm.orderId)
    const score = Number(state.reviewForm.score)
    const content = state.reviewForm.content
    const created = await request('/reviews', {
      method: 'POST',
      body: JSON.stringify({
        orderId,
        score,
        content
      })
    })
    state.reviewForm = { orderId: '', score: 5, content: '' }
    setMessage('评价提交成功', 'success')
    await Promise.all([loadTradeRecords(), loadMessages()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.review = false
  }
}

function fillDispute(order) {
  state.disputeForm.orderId = order.id
  state.disputeForm.reason = `关于订单「${order.itemTitle}」的纠纷：`
}

async function createDispute() {
  if (!state.disputeForm.orderId || !state.disputeForm.reason) {
    setMessage('请填写订单 ID 和纠纷原因', 'error')
    return false
  }
  state.loading.dispute = true
  try {
    const orderId = Number(state.disputeForm.orderId)
    const reason = state.disputeForm.reason
    await request('/disputes', {
      method: 'POST',
      body: JSON.stringify({
        orderId,
        reason
      })
    })
    state.disputeForm = { orderId: '', reason: '' }
    setMessage('纠纷已提交', 'success')
    await Promise.all([loadTradeRecords(), loadMessages()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.dispute = false
  }
}

async function payOrder(orderId, paymentMethod) {
  state.loading.orderAction = true
  try {
    await request('/orders/pay', {
      method: 'PATCH',
      body: JSON.stringify({
        orderId: Number(orderId),
        paymentMethod
      })
    })
    setMessage('支付成功，订单已进入待发货', 'success')
    await Promise.all([loadOrders(), loadTradeRecords(), loadMessages()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.orderAction = false
  }
}

async function shipOrder(orderId) {
  state.loading.orderAction = true
  try {
    await request('/orders/ship', {
      method: 'PATCH',
      body: JSON.stringify({ orderId: Number(orderId) })
    })
    setMessage('已标记为已发货', 'success')
    await Promise.all([loadOrders(), loadTradeRecords(), loadMessages()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.orderAction = false
  }
}

async function completeOrder(orderId) {
  state.loading.orderAction = true
  try {
    await request('/orders/complete', {
      method: 'PATCH',
      body: JSON.stringify({ orderId: Number(orderId) })
    })
    setMessage('已确认收货，订单完成', 'success')
    await Promise.all([loadOrders(), loadTradeRecords(), loadMessages()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.orderAction = false
  }
}

async function cancelOrder(orderId, cancelReason) {
  const normalizedReason = String(cancelReason || '').trim()
  if (!normalizedReason) {
    setMessage('请填写取消原因', 'error')
    return false
  }
  state.loading.orderAction = true
  try {
    await request('/orders/cancel', {
      method: 'PATCH',
      body: JSON.stringify({
        orderId: Number(orderId),
        cancelReason: normalizedReason
      })
    })
    setMessage('订单已取消', 'success')
    await Promise.all([loadOrders(), loadTradeRecords(), loadMessages(), loadItems(), loadMyItems()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.orderAction = false
  }
}

async function handleDispute(disputeId, status, handleResult) {
  const normalizedResult = String(handleResult || '').trim()
  if (!normalizedResult) {
    setMessage('请填写处理结果', 'error')
    return false
  }
  state.loading.disputeHandle = true
  try {
    await request('/disputes/handle', {
      method: 'PATCH',
      body: JSON.stringify({
        disputeId: Number(disputeId),
        status,
        handleResult: normalizedResult
      })
    })
    setMessage('纠纷处理结果已提交', 'success')
    await Promise.all([loadOrders(), loadTradeRecords(), loadMessages(), loadAdminData(), loadItems(), loadMyItems()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.disputeHandle = false
  }
}

async function verifyStudent() {
  if (!state.verifyForm.studentNo || !state.verifyForm.campus) {
    setMessage('请填写学号和校区', 'error')
    return false
  }
  state.loading.verify = true
  try {
    await request('/auth/student/verify', {
      method: 'POST',
      body: JSON.stringify(state.verifyForm)
    })
    setMessage('校园认证成功', 'success')
    await loadProfile()
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.verify = false
  }
}

async function uploadProfileAvatar(file) {
  if (!file) {
    setMessage('请选择要上传的头像图片', 'error')
    return false
  }
  const formData = new FormData()
  formData.append('file', file)
  state.loading.avatarSave = true
  try {
    const data = await requestForm('/profile/avatar', formData)
    state.profile = {
      ...state.profile,
      ...data,
      avatarUrl: normalizeImageUrl(data.avatarUrl)
    }
    state.currentUser = {
      ...state.currentUser,
      nickname: data.nickname ?? state.currentUser.nickname,
      avatarUrl: normalizeImageUrl(data.avatarUrl) || state.currentUser.avatarUrl
    }
    persistUser()
    setMessage('头像已更新', 'success')
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.avatarSave = false
  }
}

async function createItemReport(itemId, reason) {
  const normalizedReason = String(reason || '').trim()
  if (!state.token) {
    setMessage('请先登录后再举报商品', 'error')
    return false
  }
  if (!normalizedReason) {
    setMessage('请填写举报原因', 'error')
    return false
  }
  try {
    await request('/reports', {
      method: 'POST',
      body: JSON.stringify({
        targetType: 'ITEM',
        targetId: Number(itemId),
        reason: normalizedReason
      })
    })
    setMessage('举报已提交，商品已转入待审', 'success')
    await Promise.all([loadItems(), loadAdminData(), loadMyItems()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  }
}

async function submitWanted() {
  const form = state.wantedForm
  if (!form.title || !form.budget || !form.description) {
    setMessage('请完整填写求购信息', 'error')
    return false
  }
  const verified = await ensureCampusVerified('发布求购')
  if (!verified) {
    return false
  }
  state.loading.wantedSubmit = true
  try {
    await request('/wanted', {
      method: 'POST',
      body: JSON.stringify({
        title: form.title,
        budget: Number(form.budget),
        description: form.description
      })
    })
    state.wantedForm = { title: '', budget: '', description: '' }
    setMessage('求购信息已发布', 'success')
    await loadWanted()
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.wantedSubmit = false
  }
}

async function updateMyItemStatus(itemId, status) {
  try {
    await request(`/my/items/${itemId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    })
    setMessage(`商品状态已更新为 ${status}`, 'success')
    await Promise.all([loadMyItems(), loadItems()])
    await cleanupInvalidCartItems({ notify: true })
  } catch (error) {
    setMessage(error.message, 'error')
  }
}

async function deleteMyItem(itemId) {
  try {
    await request(`/my/items/${itemId}`, { method: 'DELETE' })
    setMessage('商品已删除', 'success')
    await Promise.all([loadMyItems(), loadItems()])
    await cleanupInvalidCartItems({ notify: true })
  } catch (error) {
    setMessage(error.message, 'error')
  }
}

async function auditItem(override = null) {
  const form = override
    ? {
        itemId: override.itemId ?? state.adminAuditForm.itemId,
        result: override.result ?? state.adminAuditForm.result,
        reason: override.reason ?? state.adminAuditForm.reason,
        userAction: override.userAction ?? state.adminAuditForm.userAction
      }
    : state.adminAuditForm
  if (!form.itemId) {
    setMessage('请填写要审核的商品 ID', 'error')
    return false
  }
  state.loading.audit = true
  try {
    await request('/admin/items/audit', {
      method: 'PATCH',
      body: JSON.stringify({
        itemId: Number(form.itemId),
        result: form.result,
        reason: form.reason,
        userAction: form.userAction
      })
    })
    setMessage('审核提交成功', 'success')
    if (!override) {
      state.adminAuditForm = {
        itemId: '',
        result: 'APPROVED',
        reason: '',
        userAction: 'NONE'
      }
    }
    await Promise.all([loadItems(), loadAdminData(), loadMyItems()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  } finally {
    state.loading.audit = false
  }
}

async function updateUserStatus(userId, status) {
  try {
    await request('/admin/users/status', {
      method: 'PATCH',
      body: JSON.stringify({ userId, status })
    })
    setMessage(`用户状态已更新为 ${status}`, 'success')
    await loadAdminData()
  } catch (error) {
    setMessage(error.message, 'error')
  }
}

async function deleteUser(userId) {
  try {
    await request(`/admin/users/${userId}`, { method: 'DELETE' })
    setMessage('用户已删除', 'success')
    await Promise.all([loadAdminData(), loadItems(), loadMyItems()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  }
}

async function ensureCampusVerified(actionText = '使用该功能') {
  if (!state.token) {
    setMessage('请先登录', 'error')
    return false
  }
  if (!state.profile.userId) {
    await loadProfile()
  }
  if (state.profile.campusVerified) {
    return true
  }
  setMessage(`请先完成校园认证后再${actionText}`, 'error')
  return false
}

async function ensureBootstrapped() {
  await checkHealth()
  await loadPublicData()
  await cleanupInvalidCartItems()
  if (state.token) {
    const tasks = [
      loadOrders(),
      loadTradeRecords(),
      loadMessages(),
      loadFavoritesAndHistory(),
      loadProfile(),
      loadMyItems()
    ]
    await Promise.all(tasks)
    if (isAdmin.value) {
      await loadAdminData()
    }
  }
}

async function sendChatMessage(payload) {
  const created = await request('/messages', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  await loadMessages()
  return normalizeMessageRecord(created)
}

async function deleteOrderRecord(orderId) {
  try {
    await request(`/orders/${orderId}`, { method: 'DELETE' })
    setMessage('订单记录已删除', 'success')
    await Promise.all([loadOrders(), loadTradeRecords(), loadMessages()])
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  }
}

async function deleteMessageThread({ orderId = '', itemId = '' } = {}) {
  try {
    const params = new URLSearchParams()
    if (orderId) params.set('orderId', String(orderId))
    if (itemId) params.set('itemId', String(itemId))
    await request(`/messages/thread?${params.toString()}`, { method: 'DELETE' })
    setMessage('聊天记录已删除', 'success')
    await loadMessages()
    return true
  } catch (error) {
    setMessage(error.message, 'error')
    return false
  }
}

function saveAddress() {
  localStorage.setItem(ADDRESS_KEY, JSON.stringify(state.addressForm))
  setMessage('收货地址已保存', 'success')
}

function saveSettings() {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(state.settingsForm))
  setMessage('设置已更新', 'success')
}

const favoriteIds = computed(() => new Set(state.favorites.map(item => Number(item.itemId))))
const cartIds = computed(() => new Set(state.cart.map(item => Number(item.id))))
const cartCount = computed(() => state.cart.length)
const isAdmin = computed(() => Boolean(state.profile.isAdmin ?? state.currentUser.isAdmin))
const categoryOptions = computed(() => {
  const set = new Set([...DEFAULT_CATEGORY_OPTIONS, ...state.items.map(item => item.category).filter(Boolean)])
  return ['全部商品', ...set]
})
const adminMetricList = computed(() => [
  { label: '用户数', value: state.dashboard.userCount ?? 0 },
  { label: '商品数', value: state.dashboard.itemCount ?? 0 },
  { label: '订单数', value: state.dashboard.orderCount ?? 0 },
  { label: '待审商品', value: state.dashboard.pendingAuditCount ?? 0 },
  { label: '待处理纠纷', value: state.dashboard.pendingDisputeCount ?? 0 },
  { label: '举报数', value: state.dashboard.reportCount ?? 0 }
])

export function useMarketplace() {
  return {
    state,
    favoriteIds,
    cartIds,
    cartCount,
    isAdmin,
    categoryOptions,
    adminMetricList,
    setMessage,
    ensureBootstrapped,
    checkHealth,
    login,
    register,
    checkNicknameAvailability,
    updateNickname,
    changePassword,
    logout,
    loadItems,
    loadOrders,
    loadReviewHistory,
    loadDisputeHistory,
    loadTradeRecords,
    loadMessages,
    loadFavoritesAndHistory,
    loadProfile,
    loadWanted,
    fetchItemDetail,
    loadMyItems,
    loadAdminData,
    toggleFavorite,
    removeFavorite,
    addToCart,
    cleanupInvalidCartItems,
    appendChatRecord,
    removeFromCart,
    clearCart,
    checkoutCart,
    createOrder,
    payOrder,
    shipOrder,
    completeOrder,
    cancelOrder,
    sendChatMessage,
    deleteOrderRecord,
    deleteMessageThread,
    publishItem,
    fillReview,
    submitReview,
    fillDispute,
    createDispute,
    verifyStudent,
    uploadProfileAvatar,
    createItemReport,
    submitWanted,
    uploadItemImages,
    updateMyItemStatus,
    deleteMyItem,
    auditItem,
    handleDispute,
    updateUserStatus,
    deleteUser,
    saveAddress,
    saveSettings
  }
}
