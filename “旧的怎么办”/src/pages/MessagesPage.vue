<template>
  <section class="page-stack">
    <section class="floor-card">
      <div class="floor-header">
        <div>
          <h3>消息中心</h3>
        </div>
      </div>

      <div class="message-layout">
        <aside class="message-session-list">
          <div class="search-suggest-title">全部聊天历史</div>
          <button
            v-for="entry in conversationEntries"
            :key="entry.id"
            :class="['message-session-item', { active: activeSessionId === entry.id }]"
            @click="openConversation(entry)"
          >
            <strong>{{ entry.title }}</strong>
            <span>{{ entry.desc }}</span>
            <span>{{ entry.time }}</span>
          </button>
          <div v-if="!conversationEntries.length" class="empty-inline">暂无聊天记录。</div>
        </aside>

        <section class="message-panel">
          <div class="message-panel-header">
            <div>
              <strong>{{ activeConversation?.title || '聊天详情' }}</strong>
              <span>{{ activeConversation?.desc || '选择左侧聊天记录后查看详情' }}</span>
            </div>
            <div v-if="activeConversation" class="inline-actions">
              <button class="mini-outline" @click="handleDeleteCurrentThread">删除记录</button>
              <RouterLink v-if="activeConversation.link" class="outline-btn link-btn" :to="activeConversation.link">
                {{ activeConversation.linkText }}
              </RouterLink>
            </div>
          </div>

          <div v-if="activeConversation?.kind === 'item' && itemConsultCard" class="message-item-card">
            <div class="message-item-card-main">
              <strong>{{ itemConsultCard.title }}</strong>
            </div>
            <div class="message-item-meta-grid">
              <div>
                <span>商品名称</span>
                <strong>{{ itemConsultCard.title }}</strong>
              </div>
              <div>
                <span>价格</span>
                <strong>￥{{ itemConsultCard.price }}</strong>
              </div>
              <div>
                <span>校区</span>
                <strong>{{ itemConsultCard.campus }}</strong>
              </div>
              <div>
                <span>交易ID</span>
                <strong>{{ itemConsultCard.tradeId }}</strong>
              </div>
              <div>
                <span>库存</span>
                <strong>{{ itemConsultCard.stock }}</strong>
              </div>
            </div>
          </div>

          <div v-if="activeConversation" class="chat-log">
            <div v-for="entry in chatMessages" :key="entry.id" :class="['chat-bubble', entry.from]">
              <strong>{{ entry.from === 'self' ? '我' : entry.from === 'other' ? '对方' : '系统' }}</strong>
              <p>{{ entry.text }}</p>
              <small>{{ entry.time }}</small>
            </div>
            <div v-if="chatMessages.length === 1 && chatMessages[0].from === 'system'" class="empty-inline">
              还没有聊天内容，现在可以继续发送消息。
            </div>
          </div>
          <div v-else class="empty-state">请选择左侧聊天记录查看详情。</div>

          <div v-if="activeConversation" class="chat-input-row">
            <input
              v-model="draft"
              class="chat-input"
              :disabled="isSelfConversation || isUnavailableConversation || isMissingChatTarget"
              :placeholder="chatInputPlaceholder"
              @keyup.enter="sendMessage"
            />
            <button class="search-btn" :disabled="!draft.trim() || isSelfConversation || isUnavailableConversation || isMissingChatTarget" @click="sendMessage">发送</button>
          </div>
        </section>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useMarketplace } from '../composables/useMarketplace'

const router = useRouter()
const route = useRoute()
const { state, fetchItemDetail, loadOrders, loadMessages, sendChatMessage, deleteMessageThread, setMessage } = useMarketplace()
const activeSessionId = ref('')
const draft = ref('')
const relatedItem = ref(null)

const routeItemId = computed(() => readQueryValue(route.query.itemId))
const routeOrderId = computed(() => readQueryValue(route.query.orderId))
const routeWantedId = computed(() => readQueryValue(route.query.wantedId))
const routeItemTitle = computed(() => readQueryValue(route.query.itemTitle))
const routeItemPrice = computed(() => readQueryValue(route.query.itemPrice))
const routeCampus = computed(() => readQueryValue(route.query.campus))
const routeStock = computed(() => readQueryValue(route.query.stock))
const routeOrderNo = computed(() => readQueryValue(route.query.orderNo))
const routeWantedTitle = computed(() => readQueryValue(route.query.wantedTitle) || '求购信息')
const currentUserId = computed(() => Number(state.currentUser.userId || state.profile.userId || 0))

const routeConversation = computed(() => {
  if (routeWantedId.value) {
    return buildConversationEntry({
      kind: 'wanted',
      wantedId: routeWantedId.value,
      title: routeWantedTitle.value,
      text: '当前求购沟通暂未接入持久化消息，可先查看历史聊天。',
      createdAt: new Date().toISOString()
    })
  }
  if (routeOrderId.value || routeItemId.value) {
    return buildConversationEntry({
      kind: 'item',
      orderId: routeOrderId.value,
      itemId: routeItemId.value,
      itemTitle: routeItemTitle.value,
      orderNo: routeOrderNo.value,
      text: routeOrderId.value ? `订单 ${routeOrderNo.value || routeOrderId.value}` : '商品咨询',
      createdAt: new Date().toISOString()
    })
  }
  return null
})

const messageHistoryEntries = computed(() => {
  const latestByScope = new Map()
  state.chatHistory
    .filter(entry => {
      const relatedToUser = [entry.fromUserId, entry.toUserId].some(id => Number(id) === currentUserId.value)
      if (!state.token) {
        return false
      }
      if (!relatedToUser) {
        return false
      }
      return Boolean(entry.orderId || entry.itemId)
    })
    .forEach(entry => {
      const conversation = buildConversationEntry({
        kind: 'item',
        orderId: entry.orderId,
        itemId: entry.itemId,
        itemTitle: entry.itemTitle,
        orderNo: findOrderNo(entry.orderId),
        text: entry.content || entry.text || '',
        createdAt: entry.createdAt,
        messageType: entry.entryType
      })
      const previous = latestByScope.get(conversation.id)
      if (!previous || new Date(conversation.createdAt) > new Date(previous.createdAt)) {
        latestByScope.set(conversation.id, conversation)
      }
    })
  return Array.from(latestByScope.values()).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

const conversationEntries = computed(() => {
  const deduped = new Map()
  for (const entry of [...messageHistoryEntries.value, routeConversation.value]) {
    if (!entry) continue
    if (!deduped.has(entry.id)) {
      deduped.set(entry.id, entry)
    }
  }
  return Array.from(deduped.values())
})

const activeConversation = computed(() => {
  return conversationEntries.value.find(entry => entry.id === activeSessionId.value) || null
})

const orderContext = computed(() => {
  if (!activeConversation.value?.orderId) return null
  return state.orders.find(order => String(order.id) === String(activeConversation.value.orderId)) || null
})

const threadHistory = computed(() => {
  if (!activeConversation.value) return []
  return state.chatHistory.filter(entry => matchesConversation(entry, activeConversation.value))
})

const resolvedTargetUserId = computed(() => {
  if (!activeConversation.value) return null
  if (orderContext.value) {
    return currentUserId.value === Number(orderContext.value.buyerId)
      ? Number(orderContext.value.sellerId)
      : Number(orderContext.value.buyerId)
  }
  const counterpartIds = new Set()
  threadHistory.value.forEach(entry => {
    const fromUserId = Number(entry.fromUserId || 0)
    const toUserId = Number(entry.toUserId || 0)
    if (fromUserId && fromUserId !== currentUserId.value) counterpartIds.add(fromUserId)
    if (toUserId && toUserId !== currentUserId.value) counterpartIds.add(toUserId)
  })
  const historyCounterpart = Array.from(counterpartIds)[0]
  if (historyCounterpart) {
    return historyCounterpart
  }
  if (relatedItem.value?.sellerId && Number(relatedItem.value.sellerId) !== currentUserId.value) {
    return Number(relatedItem.value.sellerId)
  }
  return null
})

const isSelfConversation = computed(() => {
  if (!activeConversation.value) return false
  return Boolean(resolvedTargetUserId.value) && Number(resolvedTargetUserId.value) === currentUserId.value
})

const isUnavailableConversation = computed(() => {
  if (!activeConversation.value) return true
  if (activeConversation.value.kind === 'wanted') return true
  return Boolean(activeConversation.value.itemId) && !orderContext.value && !relatedItem.value
})

const isMissingChatTarget = computed(() => {
  if (!activeConversation.value || activeConversation.value.kind === 'wanted') return false
  return !resolvedTargetUserId.value
})

const chatInputPlaceholder = computed(() => {
  if (isSelfConversation.value) return '不能和自己聊天'
  if (isMissingChatTarget.value) return '当前会话缺少聊天对象'
  if (activeConversation.value?.kind === 'wanted') return '当前求购沟通暂未接入'
  if (isUnavailableConversation.value) return '商品已失效'
  return '输入消息'
})

const itemConsultCard = computed(() => {
  if (!activeConversation.value || activeConversation.value.kind !== 'item') return null
  const priceValue = relatedItem.value?.price ?? routeItemPrice.value
  const stockValue = relatedItem.value?.stock ?? routeStock.value
  return {
    title: relatedItem.value?.title || activeConversation.value.itemTitle || `商品 #${activeConversation.value.itemId || '--'}`,
    price: priceValue || '--',
    campus: relatedItem.value?.campus || routeCampus.value || '校内面交',
    tradeId: orderContext.value?.orderNo || activeConversation.value.orderNo || activeConversation.value.orderId || `ITEM-${activeConversation.value.itemId || '--'}`,
    stock: Number(stockValue ?? 0) > 0 ? `${Number(stockValue)} 件` : '已售罄'
  }
})

const chatMessages = computed(() => {
  if (!activeConversation.value) return []
  const introText = buildIntroText()
  const messages = introText
    ? [{ id: `intro-${activeConversation.value.id}`, from: 'system', text: introText, time: formatTime(new Date().toISOString()) }]
    : []
  const threadMessages = state.chatHistory
    .filter(entry => matchesConversation(entry, activeConversation.value))
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    .map(entry => ({
      id: entry.id,
      from: Number(entry.fromUserId) === currentUserId.value ? 'self' : 'other',
      text: entry.content || entry.text || '',
      time: formatTime(entry.createdAt)
    }))
  return [...messages, ...threadMessages]
})

onMounted(async () => {
  if (state.token) {
    await Promise.all([loadOrders(), loadMessages()])
  }
  syncActiveConversation()
  await loadRelatedItem()
})

watch(
  () => [routeConversation.value?.id, conversationEntries.value.length],
  async () => {
    syncActiveConversation()
    await loadRelatedItem()
  },
  { immediate: true }
)

watch(
  () => activeConversation.value?.id,
  async () => {
    await loadRelatedItem()
  }
)

async function sendMessage() {
  const text = draft.value.trim()
  if (!text || !activeConversation.value || isSelfConversation.value || isUnavailableConversation.value || isMissingChatTarget.value) return
  const targetUserId = resolvedTargetUserId.value
  if (targetUserId && targetUserId === currentUserId.value) {
    return
  }
  try {
    await sendChatMessage({
      orderId: activeConversation.value.orderId || null,
      itemId: activeConversation.value.itemId || null,
      toUserId: targetUserId,
      content: text
    })
    draft.value = ''
  } catch (error) {
    setMessage(error?.message || '消息发送失败', 'error')
  }
}

function openConversation(entry) {
  activeSessionId.value = entry.id
  router.replace({
    path: '/messages',
    query: {
      orderId: entry.orderId || '',
      itemId: entry.itemId || '',
      wantedId: entry.wantedId || '',
      itemTitle: entry.itemTitle || entry.title,
      orderNo: entry.orderNo || ''
    }
  })
}

async function handleDeleteCurrentThread() {
  if (!activeConversation.value) return
  const ok = await deleteMessageThread({
    orderId: activeConversation.value.orderId || '',
    itemId: activeConversation.value.itemId || ''
  })
  if (ok) {
    router.replace('/messages')
  }
}

function syncActiveConversation() {
  if (routeConversation.value) {
    activeSessionId.value = routeConversation.value.id
    return
  }
  if (conversationEntries.value.some(entry => entry.id === activeSessionId.value)) {
    return
  }
  activeSessionId.value = conversationEntries.value[0]?.id || ''
}

function buildIntroText() {
  if (!activeConversation.value) return ''
  if (activeConversation.value.kind === 'wanted') {
    return `当前求购：${activeConversation.value.title}`
  }
  if (itemConsultCard.value) {
    return `商品：${itemConsultCard.value.title}，价格 ￥${itemConsultCard.value.price}，校区 ${itemConsultCard.value.campus}，交易ID ${itemConsultCard.value.tradeId}，库存 ${itemConsultCard.value.stock}`
  }
  return '商品咨询'
}

async function loadRelatedItem() {
  if (!activeConversation.value?.itemId || activeConversation.value.kind === 'wanted') {
    relatedItem.value = null
    return
  }
  try {
    relatedItem.value = await fetchItemDetail(activeConversation.value.itemId)
  } catch (error) {
    relatedItem.value = null
    if (!activeConversation.value?.orderId && /商品不存在|商品已下架或不存在/i.test(String(error?.message || ''))) {
      setMessage('该商品已失效，已为你返回商品页', 'info')
      router.replace('/items')
    }
  }
}

function matchesConversation(entry, conversation) {
  if (!conversation) return false
  if (conversation.kind === 'wanted') {
    return false
  }
  if (conversation.orderId && String(entry.orderId || '') === String(conversation.orderId)) {
    return true
  }
  if (conversation.itemId) {
    return String(entry.itemId || '') === String(conversation.itemId)
  }
  return false
}

function buildConversationEntry({ kind, orderId = '', itemId = '', wantedId = '', itemTitle = '', orderNo = '', title = '', text = '', createdAt = '', messageType = '' }) {
  const conversationId = wantedId
    ? `wanted-${wantedId}`
    : orderId
      ? `order-${orderId}`
      : `item-${itemId}`
  const resolvedTitle = title || itemTitle || (orderId ? `订单 ${orderNo || orderId}` : `商品 #${itemId || '--'}`)
  const prefix = messageType === 'review-feedback'
    ? '评价反馈'
    : messageType === 'dispute-feedback'
      ? '纠纷反馈'
      : orderId
        ? `订单 ${orderNo || orderId}`
        : '商品咨询'
  return {
    id: conversationId,
    kind,
    orderId: orderId || '',
    itemId: itemId || '',
    wantedId: wantedId || '',
    itemTitle: itemTitle || title || '',
    orderNo: orderNo || '',
    title: resolvedTitle,
    desc: text || prefix,
    text: text || prefix,
    time: formatDateTime(createdAt),
    createdAt: createdAt || new Date().toISOString(),
    link: kind === 'item'
      ? (itemId ? `/item/${itemId}` : '/orders')
      : '/',
    linkText: kind === 'item'
      ? (itemId ? '查看关联商品' : '查看订单')
      : '返回首页'
  }
}

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleTimeString('zh-CN', { hour12: false })
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function readQueryValue(value) {
  return Array.isArray(value) ? value[0] || '' : value || ''
}

function findOrderNo(rawOrderId) {
  if (!rawOrderId) return ''
  const matchedOrder = state.orders.find(order => String(order.id) === String(rawOrderId))
  return matchedOrder?.orderNo || ''
}
</script>
