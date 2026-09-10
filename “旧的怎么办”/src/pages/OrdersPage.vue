<template>
  <section class="page-stack">
    <section class="floor-card">
      <div class="floor-header">
        <div>
          <h3>订单中心</h3>
          <p class="muted">订单按待完成、已完成和历史记录分区展示，方便直接处理当前交易。</p>
        </div>
        <button class="outline-btn" :disabled="state.loading.orders || !state.token" @click="loadOrders()">刷新订单</button>
      </div>

      <div v-if="state.token" class="page-stack">
        <section class="order-section-block">
          <div class="floor-header">
            <div>
              <h3>待完成订单</h3>
              <p class="muted">这里显示待支付、待发货、待收货和纠纷处理中订单。</p>
            </div>
            <span class="summary-chip">{{ pendingOrders.length }} 笔</span>
          </div>
          <div class="list-stack">
            <article v-for="order in pendingOrders" :key="order.id" class="list-card trade-order-card">
              <div class="trade-order-main">
                <div class="trade-order-head">
                  <div>
                    <RouterLink v-if="order.itemId" class="goods-title-link" :to="`/item/${order.itemId}`">
                      <strong>{{ order.itemTitle }}</strong>
                    </RouterLink>
                    <strong v-else>{{ order.itemTitle }}</strong>
                    <p class="muted">订单号：{{ order.orderNo || order.id }}</p>
                  </div>
                  <span class="summary-chip order-status-chip">{{ statusText(order.status) }}</span>
                </div>

                <p class="muted">买家：{{ order.buyerName }} · 卖家：{{ order.sellerName }}</p>
                <p class="muted">成交金额：￥{{ order.amount }} · 下单时间：{{ formatTime(order.createdAt) }}</p>
                <p v-if="paymentCountdownText(order)" class="muted">{{ paymentCountdownText(order) }}</p>
                <p class="muted">
                  收货信息：{{ order.receiverName || '-' }} / {{ order.receiverPhone || '-' }} / {{ order.receiverCampus || '-' }}
                </p>
                <p class="muted">详细地址：{{ order.receiverDetail || '-' }}</p>
                <p v-if="order.paymentMethod || order.paymentNo" class="muted">
                  支付信息：{{ paymentMethodText(order.paymentMethod) }}<span v-if="order.paymentNo"> · 流水号 {{ order.paymentNo }}</span>
                </p>
                <p v-if="order.paidAt || order.shippedAt || order.completedAt" class="muted">
                  <span v-if="order.paidAt">支付 {{ formatTime(order.paidAt) }}</span>
                  <span v-if="order.shippedAt"> · 发货 {{ formatTime(order.shippedAt) }}</span>
                  <span v-if="order.completedAt"> · 完成 {{ formatTime(order.completedAt) }}</span>
                </p>
                <p v-if="order.cancelReason" class="muted">取消原因：{{ order.cancelReason }}</p>

                <div class="order-history-inline">
                  <span v-if="latestReviewByOrder[order.id]" class="summary-chip">
                    已评价：{{ latestReviewByOrder[order.id].score }} 星
                  </span>
                  <span v-if="latestDisputeByOrder[order.id]" class="summary-chip">
                    纠纷：{{ disputeStatusText(latestDisputeByOrder[order.id].status) }}
                  </span>
                </div>
              </div>

              <div class="inline-actions trade-order-actions">
                <button v-if="canPay(order)" class="search-btn" :disabled="state.loading.orderAction" @click="openPayModal(order)">去支付</button>
                <button v-if="canCancel(order)" class="mini-outline" :disabled="state.loading.orderAction" @click="openCancelModal(order)">取消订单</button>
                <button v-if="canShip(order)" class="mini-outline" :disabled="state.loading.orderAction" @click="handleShip(order.id)">确认发货</button>
                <button v-if="canComplete(order)" class="mini-outline" :disabled="state.loading.orderAction" @click="handleComplete(order.id)">确认收货</button>
                <button v-if="canReview(order)" class="mini-outline" @click="openReviewModal(order)">评价</button>
                <button v-if="canDispute(order)" class="mini-outline" @click="openDisputeModal(order)">发起纠纷</button>
                <button class="mini-outline" @click="handleDeleteOrder(order.id)">删除</button>
                <RouterLink class="mini-outline link-btn" :to="buildMessageLink(order)">去沟通</RouterLink>
              </div>
            </article>
            <div v-if="!pendingOrders.length" class="empty-inline">当前没有待完成订单。</div>
          </div>
        </section>

        <section class="order-section-block">
          <div class="floor-header">
            <div>
              <h3>已完成订单</h3>
              <p class="muted">这里保留已完成、已退款和已取消等已结束交易。</p>
            </div>
            <span class="summary-chip">{{ finishedOrders.length }} 笔</span>
          </div>
          <div class="list-stack">
            <article v-for="order in finishedOrders" :key="order.id" class="list-card trade-order-card">
              <div class="trade-order-main">
                <div class="trade-order-head">
                  <div>
                    <RouterLink v-if="order.itemId" class="goods-title-link" :to="`/item/${order.itemId}`">
                      <strong>{{ order.itemTitle }}</strong>
                    </RouterLink>
                    <strong v-else>{{ order.itemTitle }}</strong>
                    <p class="muted">订单号：{{ order.orderNo || order.id }}</p>
                  </div>
                  <span class="summary-chip order-status-chip">{{ statusText(order.status) }}</span>
                </div>

                <p class="muted">买家：{{ order.buyerName }} · 卖家：{{ order.sellerName }}</p>
                <p class="muted">成交金额：￥{{ order.amount }} · 下单时间：{{ formatTime(order.createdAt) }}</p>
                <p class="muted">
                  收货信息：{{ order.receiverName || '-' }} / {{ order.receiverPhone || '-' }} / {{ order.receiverCampus || '-' }}
                </p>
                <p class="muted">详细地址：{{ order.receiverDetail || '-' }}</p>
                <p v-if="order.paymentMethod || order.paymentNo" class="muted">
                  支付信息：{{ paymentMethodText(order.paymentMethod) }}<span v-if="order.paymentNo"> · 流水号 {{ order.paymentNo }}</span>
                </p>
                <p v-if="order.paidAt || order.shippedAt || order.completedAt" class="muted">
                  <span v-if="order.paidAt">支付 {{ formatTime(order.paidAt) }}</span>
                  <span v-if="order.shippedAt"> · 发货 {{ formatTime(order.shippedAt) }}</span>
                  <span v-if="order.completedAt"> · 完成 {{ formatTime(order.completedAt) }}</span>
                </p>
                <p v-if="order.cancelReason" class="muted">取消原因：{{ order.cancelReason }}</p>

                <div class="order-history-inline">
                  <span v-if="latestReviewByOrder[order.id]" class="summary-chip">
                    已评价：{{ latestReviewByOrder[order.id].score }} 星
                  </span>
                  <span v-if="latestDisputeByOrder[order.id]" class="summary-chip">
                    纠纷：{{ disputeStatusText(latestDisputeByOrder[order.id].status) }}
                  </span>
                </div>
              </div>

              <div class="inline-actions trade-order-actions">
                <button v-if="canReview(order)" class="mini-outline" @click="openReviewModal(order)">评价</button>
                <button class="mini-outline" @click="handleDeleteOrder(order.id)">删除</button>
                <RouterLink class="mini-outline link-btn" :to="buildMessageLink(order)">去沟通</RouterLink>
              </div>
            </article>
            <div v-if="!finishedOrders.length" class="empty-inline">当前还没有已完成订单。</div>
          </div>
        </section>
      </div>
      <div v-else class="empty-state">
        请先登录
        <RouterLink class="outline-btn link-btn inline-link-btn" to="/profile">去登录</RouterLink>
      </div>
    </section>

    <section v-if="state.token" class="floor-card">
      <div class="floor-header">
        <div>
          <h3>历史记录</h3>
        </div>
      </div>

      <div v-if="historyRecords.length" class="history-record-list">
        <article v-for="entry in historyRecords" :key="entry.id" class="history-record-card">
          <div class="history-record-head">
            <div class="history-record-head-main">
              <strong>{{ entry.title }}</strong>
              <span class="history-record-time">{{ formatTime(entry.detailCreatedAt) }}</span>
            </div>
            <span class="summary-chip">{{ entry.orderNo }}</span>
          </div>
          <p class="muted">{{ entry.meta }}</p>
          <p class="history-record-content">{{ entry.summary }}</p>
          <div v-if="isHistoryExpanded(entry.id)" class="history-record-detail-list">
            <p>{{ entry.orderLine }}</p>
            <p>{{ entry.reviewLine }}</p>
            <p v-if="entry.disputeLine">{{ entry.disputeLine }}</p>
          </div>
          <div class="history-record-actions">
            <button class="mini-outline" @click="toggleHistory(entry.id)">
              {{ isHistoryExpanded(entry.id) ? '收起详情' : '查看详情' }}
            </button>
            <button v-if="entry.itemId" class="mini-outline" @click="openItemDetail(entry.itemId)">查看商品</button>
            <RouterLink class="mini-outline link-btn" :to="buildMessageLink(entry)">继续沟通</RouterLink>
            <button class="mini-outline" @click="handleDeleteOrder(entry.orderId)">删除记录</button>
          </div>
        </article>
      </div>
      <div v-else class="empty-inline">还没有历史记录。</div>
    </section>

    <div v-if="activeAction && selectedOrder" class="profile-modal-backdrop" @click.self="closeActionModal">
      <section class="profile-modal-card">
        <div class="floor-header profile-modal-header">
          <div>
            <h3>{{ modalTitle }}</h3>
            <p>当前商品：{{ selectedOrder.itemTitle }} · 订单号 {{ selectedOrder.orderNo || selectedOrder.id }}</p>
          </div>
          <button class="outline-btn" @click="closeActionModal">关闭</button>
        </div>

        <div v-if="activeAction === 'review'" class="form-card compact-form-card profile-modal-body">
          <label>
            <span>评分</span>
            <input v-model="state.reviewForm.score" type="number" min="1" max="5" />
          </label>
          <label>
            <span>评价内容</span>
            <textarea v-model="state.reviewForm.content" rows="4"></textarea>
          </label>
          <button class="search-btn" :disabled="state.loading.review" @click="handleSubmitReview">提交评价</button>
        </div>

        <div v-else-if="activeAction === 'dispute'" class="form-card compact-form-card profile-modal-body">
          <label>
            <span>纠纷原因</span>
            <textarea v-model="state.disputeForm.reason" rows="5"></textarea>
          </label>
          <button class="search-btn" :disabled="state.loading.dispute" @click="handleSubmitDispute">提交纠纷</button>
        </div>

        <div v-else-if="activeAction === 'pay'" class="form-card compact-form-card profile-modal-body">
          <p class="muted">支付金额：￥{{ selectedOrder.amount }}</p>
          <p v-if="paymentCountdownText(selectedOrder)" class="muted">{{ paymentCountdownText(selectedOrder) }}</p>
          <p class="muted">
            收货信息：{{ selectedOrder.receiverName }} / {{ selectedOrder.receiverPhone }} / {{ selectedOrder.receiverCampus }}
          </p>
          <p class="muted">地址：{{ selectedOrder.receiverDetail }}</p>
          <label>
            <span>支付方式</span>
            <select v-model="payForm.paymentMethod">
              <option value="WECHAT">微信支付</option>
              <option value="ALIPAY">支付宝</option>
              <option value="CAMPUS_CARD">校园卡支付</option>
            </select>
          </label>
          <button class="search-btn" :disabled="state.loading.orderAction" @click="handlePayOrder">确认支付</button>
        </div>

        <div v-else class="form-card compact-form-card profile-modal-body">
          <label>
            <span>取消原因</span>
            <textarea v-model="cancelForm.reason" rows="4" placeholder="例如：暂时不需要、信息填写错误"></textarea>
          </label>
          <button class="search-btn" :disabled="state.loading.orderAction" @click="handleCancelOrder">确认取消</button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useMarketplace } from '../composables/useMarketplace'

const router = useRouter()
const {
  state,
  loadOrders,
  loadTradeRecords,
  fillReview,
  fillDispute,
  submitReview,
  createDispute,
  payOrder,
  shipOrder,
  completeOrder,
  cancelOrder,
  deleteOrderRecord,
  fetchItemDetail,
  setMessage
} = useMarketplace()
const activeAction = ref('')
const selectedOrder = ref(null)
const expandedHistoryIds = ref([])
const nowTick = ref(Date.now())
const expiredRefreshPending = ref(false)
let countdownTimer = null
const payForm = reactive({
  paymentMethod: 'WECHAT'
})
const cancelForm = reactive({
  reason: '暂时不需要该商品'
})

const currentUserId = computed(() => Number(state.currentUser.userId || state.profile.userId || 0))
const modalTitle = computed(() => {
  if (activeAction.value === 'review') return '提交评价'
  if (activeAction.value === 'dispute') return '发起纠纷'
  if (activeAction.value === 'pay') return '订单支付'
  return '取消订单'
})

const latestReviewByOrder = computed(() =>
  Object.fromEntries(
    state.reviews.map(review => [Number(review.orderId), review])
  )
)

const latestDisputeByOrder = computed(() =>
  Object.fromEntries(
    state.disputes.map(dispute => [Number(dispute.orderId), dispute])
  )
)

const pendingOrders = computed(() =>
  state.orders.filter(order => ['PENDING_PAYMENT', 'PAID', 'SHIPPED', 'DISPUTING'].includes(String(order.status || '').toUpperCase()))
)

const finishedOrders = computed(() =>
  state.orders.filter(order => ['COMPLETED', 'REFUNDED', 'CANCELLED', 'PAYMENT_FAILED'].includes(String(order.status || '').toUpperCase()))
)

const historyRecords = computed(() =>
  state.orders.map(order => {
    const review = latestReviewByOrder.value[order.id]
    const dispute = latestDisputeByOrder.value[order.id]
    return {
      id: `order-${order.id}`,
      createdAt: order.createdAt,
      itemId: order.itemId,
      orderId: order.id,
      orderNo: order.orderNo || order.id,
      itemTitle: order.itemTitle,
      title: `订单记录 · ${order.itemTitle}`,
      meta: `买家 ${order.buyerName} · 卖家 ${order.sellerName}`,
      summary: buildHistorySummary(order, review, dispute),
      orderLine: `订单状态：${statusText(order.status)} · 成交金额：￥${order.amount}`,
      reviewLine: review ? `评价记录：${review.score} 星，${review.fromUser} 评价了 ${review.toUser}，内容为“${review.content}”` : '评价记录：暂未提交',
      disputeLine: dispute ? buildDisputeLine(dispute) : '',
      detailCreatedAt: dispute?.handledAt || review?.createdAt || dispute?.createdAt || order.completedAt || order.createdAt
    }
  }).sort((a, b) => new Date(b.detailCreatedAt) - new Date(a.detailCreatedAt))
)

onMounted(async () => {
  startCountdownTimer()
  if (state.token) {
    await Promise.all([loadOrders(), loadTradeRecords()])
  }
})

onUnmounted(() => {
  stopCountdownTimer()
})

function openReviewModal(order) {
  selectedOrder.value = order
  fillReview(order)
  activeAction.value = 'review'
}

function openDisputeModal(order) {
  selectedOrder.value = order
  fillDispute(order)
  activeAction.value = 'dispute'
}

function openPayModal(order) {
  selectedOrder.value = order
  payForm.paymentMethod = order.paymentMethod || 'WECHAT'
  activeAction.value = 'pay'
}

function openCancelModal(order) {
  selectedOrder.value = order
  cancelForm.reason = '暂时不需要该商品'
  activeAction.value = 'cancel'
}

function closeActionModal() {
  activeAction.value = ''
  selectedOrder.value = null
}

async function handleSubmitReview() {
  const ok = await submitReview()
  if (!ok) return
  await loadOrders()
  closeActionModal()
}

async function handleSubmitDispute() {
  const ok = await createDispute()
  if (!ok) return
  await loadOrders()
  closeActionModal()
}

async function handlePayOrder() {
  if (!selectedOrder.value) return
  const ok = await payOrder(selectedOrder.value.id, payForm.paymentMethod)
  if (!ok) return
  closeActionModal()
}

async function handleCancelOrder() {
  if (!selectedOrder.value) return
  const ok = await cancelOrder(selectedOrder.value.id, cancelForm.reason)
  if (!ok) return
  closeActionModal()
}

async function handleShip(orderId) {
  await shipOrder(orderId)
}

async function handleComplete(orderId) {
  await completeOrder(orderId)
}

async function handleDeleteOrder(orderId) {
  await deleteOrderRecord(orderId)
}

function isBuyer(order) {
  return Number(order.buyerId) === currentUserId.value
}

function isSeller(order) {
  return Number(order.sellerId) === currentUserId.value
}

function canPay(order) {
  return isBuyer(order) && String(order.status || '').toUpperCase() === 'PENDING_PAYMENT' && getPaymentRemainingSeconds(order) > 0
}

function canCancel(order) {
  return isBuyer(order) && String(order.status || '').toUpperCase() === 'PENDING_PAYMENT' && getPaymentRemainingSeconds(order) > 0
}

function canShip(order) {
  return isSeller(order) && order.status === 'PAID'
}

function canComplete(order) {
  return !isSeller(order) && String(order.status || '').toUpperCase() === 'SHIPPED'
}

function canReview(order) {
  return order.status === 'COMPLETED'
}

function canDispute(order) {
  return ['PAID', 'SHIPPED', 'COMPLETED'].includes(order.status)
}

function buildMessageLink(order) {
  const orderId = order.orderId ?? order.id
  return {
    path: '/messages',
    query: {
      orderId,
      itemId: order.itemId,
      itemTitle: order.itemTitle,
      orderNo: order.orderNo || orderId
    }
  }
}

function buildHistorySummary(order, review, dispute) {
  if (dispute?.handleResult) {
    return `订单 ${order.orderNo || order.id} 的纠纷已被管理员处理，展开可查看处理结论。`
  }
  if (dispute) {
    return `订单 ${order.orderNo || order.id} 当前存在纠纷，管理员处理中。`
  }
  if (review) {
    return `订单 ${order.orderNo || order.id} 已完成评价，展开可查看评价详情。`
  }
  if (order.status === 'PENDING_PAYMENT') {
    return `订单 ${order.orderNo || order.id} 已创建，等待买家在 ${paymentCountdownText(order) || '30 分钟内'}完成支付。`
  }
  if (order.status === 'PAID') {
    return `订单 ${order.orderNo || order.id} 已支付，等待卖家发货。`
  }
  if (order.status === 'PAYMENT_FAILED') {
    return `订单 ${order.orderNo || order.id} 已因超时未支付而失效，商品已重新上架。`
  }
  if (order.status === 'SHIPPED') {
    return `订单 ${order.orderNo || order.id} 已发货，等待买家确认收货。`
  }
  return `订单 ${order.orderNo || order.id} 当前状态为 ${statusText(order.status)}。`
}

function buildDisputeLine(dispute) {
  const handledBy = dispute.handlerName ? `，处理人：${dispute.handlerName}` : ''
  const handledResult = dispute.handleResult ? `，处理结果：“${dispute.handleResult}”` : ''
  return `纠纷记录：${disputeStatusText(dispute.status)}，原因是“${dispute.reason}”${handledBy}${handledResult}`
}

function toggleHistory(entryId) {
  const idSet = new Set(expandedHistoryIds.value)
  if (idSet.has(entryId)) {
    idSet.delete(entryId)
  } else {
    idSet.add(entryId)
  }
  expandedHistoryIds.value = Array.from(idSet)
}

function isHistoryExpanded(entryId) {
  return expandedHistoryIds.value.includes(entryId)
}

async function openItemDetail(itemId) {
  try {
    await fetchItemDetail(itemId)
    router.push(`/item/${itemId}`)
  } catch {
    setMessage('该商品已失效，已为你返回商品页', 'info')
    router.push('/items')
  }
}

function statusText(value) {
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

function getPaymentRemainingSeconds(order) {
  nowTick.value
  if (String(order?.status || '').toUpperCase() !== 'PENDING_PAYMENT') {
    return 0
  }
  const expireAtMs = Date.parse(order?.paymentExpireAt || '')
  if (Number.isNaN(expireAtMs)) {
    return Math.max(0, Number(order?.paymentRemainingSeconds || 0))
  }
  return Math.max(0, Math.floor((expireAtMs - nowTick.value) / 1000))
}

function paymentCountdownText(order) {
  if (String(order?.status || '').toUpperCase() !== 'PENDING_PAYMENT') {
    return ''
  }
  const remainingSeconds = getPaymentRemainingSeconds(order)
  return remainingSeconds > 0
    ? `剩余支付时间 ${formatCountdown(remainingSeconds)}`
    : '支付超时，正在刷新状态...'
}

function formatCountdown(seconds) {
  const normalized = Math.max(0, Number(seconds || 0))
  const minutes = Math.floor(normalized / 60)
  const remainder = normalized % 60
  return `${String(minutes).padStart(2, '0')}:${String(remainder).padStart(2, '0')}`
}

function startCountdownTimer() {
  stopCountdownTimer()
  countdownTimer = window.setInterval(async () => {
    nowTick.value = Date.now()
    const hasExpiredPending = state.orders.some(order =>
      String(order.status || '').toUpperCase() === 'PENDING_PAYMENT' && getPaymentRemainingSeconds(order) <= 0
    )
    if (hasExpiredPending && state.token && !expiredRefreshPending.value) {
      expiredRefreshPending.value = true
      try {
        await loadOrders()
      } finally {
        expiredRefreshPending.value = false
      }
    }
  }, 1000)
}

function stopCountdownTimer() {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }
}

function disputeStatusText(value) {
  return ({
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED_REFUND: '已退款',
    RESOLVED_COMPLETE: '确认完结',
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

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}
</script>
