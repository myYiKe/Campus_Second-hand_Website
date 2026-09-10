const { STATIC_BASE, CATEGORY_OPTIONS } = require('./constants')

function normalizeImageUrl(url) {
  if (!url) return ''
  if (/^https?:\/\/./.test(url) || /^https?:\/\//.test(url)) return url
  return `${STATIC_BASE}${url}`
}

function normalizeItem(item = {}) {
  const rawImageUrls = Array.isArray(item.imageUrls)
    ? item.imageUrls
    : typeof item.imageUrls === 'string' && item.imageUrls
      ? item.imageUrls.split('||')
      : []
  const imageUrls = rawImageUrls.map(normalizeImageUrl).filter(Boolean)
  const imageUrl = normalizeImageUrl(item.imageUrl) || imageUrls[0] || ''
  return {
    ...item,
    id: Number(item.id || 0),
    sellerId: Number(item.sellerId || 0),
    stock: Number(item.stock || 0),
    favoriteCount: Number(item.favoriteCount || 0),
    price: Number(item.price || 0),
    statusText: itemStatusText(item.status),
    imageUrl,
    imageUrls: imageUrls.length ? imageUrls : imageUrl ? [imageUrl] : []
  }
}

function normalizeOrder(order = {}) {
  const createdAt = order.createdAt || order.created_at || ''
  const date = createdAt ? new Date(createdAt) : null
  const compactDate = date && !Number.isNaN(date.getTime())
    ? `${date.getFullYear()}${String(date.getMonth() + 1).padStart(2, '0')}${String(date.getDate()).padStart(2, '0')}`
    : '00000000'
  return {
    ...order,
    id: Number(order.id || 0),
    itemId: Number(order.itemId || 0),
    buyerId: Number(order.buyerId || 0),
    sellerId: Number(order.sellerId || 0),
    amount: Number(order.amount || 0),
    paymentRemainingSeconds: Number(order.paymentRemainingSeconds || 0),
    orderNo: order.orderNo || `CT${compactDate}${String(order.id || '').padStart(6, '0')}`
  }
}

function normalizeWanted(post = {}) {
  return {
    ...post,
    budget: Number(post.budget || 0),
    publisherName: post.publisherName || post.publisher || '校园用户'
  }
}

function normalizeMessage(entry = {}) {
  return {
    ...entry,
    id: entry.id || `msg-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    orderId: entry.orderId ? Number(entry.orderId) : 0,
    itemId: entry.itemId ? Number(entry.itemId) : 0,
    fromUserId: entry.fromUserId ? Number(entry.fromUserId) : 0,
    toUserId: entry.toUserId ? Number(entry.toUserId) : 0,
    createdAt: entry.createdAt || new Date().toISOString(),
    messageType: String(entry.messageType || 'TEXT').toUpperCase()
  }
}

function previewText(value, limit = 34) {
  const text = String(value || '').trim()
  if (!text) return '暂无更多说明'
  return text.length > limit ? `${text.slice(0, limit)}...` : text
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function itemStatusText(value) {
  return ({
    ON_SALE: '在售',
    PENDING_REVIEW: '待审',
    OFF_SHELF: '下架',
    SOLD: '已售'
  })[String(value || '').toUpperCase()] || value || '在售'
}

function orderStatusText(value) {
  return ({
    PENDING_PAYMENT: '待支付',
    PAID: '待发货',
    SHIPPED: '待收货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    PAYMENT_FAILED: '支付失败',
    DISPUTING: '纠纷处理中',
    REFUNDED: '已退款'
  })[String(value || '').toUpperCase()] || value || '-'
}

function getPaymentRemainingSeconds(order = {}, now = Date.now()) {
  const status = String(order.status || '').toUpperCase()
  if (status !== 'PENDING_PAYMENT') {
    return 0
  }
  const expireAtMs = Date.parse(order.paymentExpireAt || '')
  if (Number.isNaN(expireAtMs)) {
    return Math.max(0, Number(order.paymentRemainingSeconds || 0))
  }
  return Math.max(0, Math.floor((expireAtMs - now) / 1000))
}

function formatCountdown(seconds) {
  const normalized = Math.max(0, Number(seconds || 0))
  const minutes = Math.floor(normalized / 60)
  const remainder = normalized % 60
  return `${String(minutes).padStart(2, '0')}:${String(remainder).padStart(2, '0')}`
}

function paymentCountdownText(order = {}, now = Date.now()) {
  const remainingSeconds = getPaymentRemainingSeconds(order, now)
  if (String(order.status || '').toUpperCase() !== 'PENDING_PAYMENT') {
    return ''
  }
  return remainingSeconds > 0 ? `剩余支付时间 ${formatCountdown(remainingSeconds)}` : '支付超时，正在刷新状态...'
}

function disputeStatusText(value) {
  return ({
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED_REFUND: '已退款',
    RESOLVED_COMPLETE: '交易完结',
    REJECTED: '驳回纠纷'
  })[String(value || '').toUpperCase()] || value || '-'
}

function paymentMethodText(value) {
  return ({
    WECHAT: '微信支付',
    ALIPAY: '支付宝',
    CAMPUS_CARD: '校园卡支付'
  })[String(value || '').toUpperCase()] || value || '未支付'
}

function buildCartItem(item) {
  const normalized = normalizeItem(item)
  return {
    id: normalized.id,
    title: normalized.title,
    price: normalized.price,
    category: normalized.category,
    sellerId: normalized.sellerId,
    sellerName: normalized.sellerName,
    imageUrl: normalized.imageUrl,
    imageUrls: normalized.imageUrls,
    description: normalized.description,
    stock: normalized.stock,
    status: normalized.status,
    publishTime: normalized.publishTime,
    createdAt: normalized.createdAt
  }
}

function pickCategoryOptions(items = []) {
  const set = new Set([...CATEGORY_OPTIONS, ...items.map(item => item.category).filter(Boolean)])
  return ['全部商品', ...Array.from(set)]
}

module.exports = {
  normalizeImageUrl,
  normalizeItem,
  normalizeOrder,
  normalizeWanted,
  normalizeMessage,
  previewText,
  formatDateTime,
  formatTime,
  itemStatusText,
  orderStatusText,
  getPaymentRemainingSeconds,
  formatCountdown,
  paymentCountdownText,
  disputeStatusText,
  paymentMethodText,
  buildCartItem,
  pickCategoryOptions
}
