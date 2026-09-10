<template>
  <div>
    <GuestHomeView
      v-if="!state.token"
      :campus-location="state.campusLocation"
      :guest-hero-metrics="guestHeroMetrics"
      :popular-hero-item="popularHeroItem"
      :recommendation-items="recommendationItems"
      :loading-items="state.loading.items"
      :favorite-ids="favoriteIds"
      :format-date="formatDate"
      :status-text="statusText"
      :stock-text="stockText"
      :is-out-of-stock="isOutOfStock"
      @select-campus="selectCampus"
      @go-items="goItems"
      @refresh-items="refreshItems"
      @open-item-detail="openItemDetail"
      @favorite="handleFavorite"
      @report="handleReport"
      @add-cart="handleAddToCart"
      @order="handleOrder"
    />

    <AuthenticatedHomeView
      v-else
      :is-admin="isAdmin"
      :campus-location="state.campusLocation"
      :category-options="categoryOptions"
      :current-category-label="currentCategoryLabel"
      :hero-metrics="heroMetrics"
      :featured-deals="featuredDeals"
      :active-featured-deal="activeFeaturedDeal"
      :featured-index="featuredIndex"
      :panel-entries="panelEntries"
      :profile="state.profile"
      :current-user="state.currentUser"
      :published-item-count="publishedItemCount"
      :recommendation-items="recommendationItems"
      :preview-wanted-posts="previewWantedPosts"
      :favorite-ids="favoriteIds"
      :loading-items="state.loading.items"
      :loading-wanted="state.loading.wanted"
      :format-date="formatDate"
      :status-text="statusText"
      :stock-text="stockText"
      :is-own-item="isOwnItem"
      :is-out-of-stock="isOutOfStock"
      @select-category="selectCategory"
      @select-campus="selectCampus"
      @shift-featured="shiftFeatured"
      @jump-featured="setFeaturedIndex"
      @refresh-items="refreshItems"
      @go-items="goItems"
      @go-audit="goAudit"
      @go-publish="goPublish"
      @go-wanted="goWanted"
      @open-item-detail="openItemDetail"
      @favorite="handleFavorite"
      @report="handleReport"
      @add-cart="handleAddToCart"
      @order="handleOrder"
    />

    <ReportDialog
      :visible="reportDialogVisible"
      :item-title="reportTarget?.title || ''"
      :submitting="reportSubmitting"
      @cancel="closeReportDialog"
      @submit="submitReport"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AuthenticatedHomeView from '../components/home/AuthenticatedHomeView.vue'
import GuestHomeView from '../components/home/GuestHomeView.vue'
import ReportDialog from '../components/ReportDialog.vue'
import { useMarketplace } from '../composables/useMarketplace'

const router = useRouter()
const { state, isAdmin, categoryOptions, favoriteIds, loadItems, loadWanted, loadProfile, loadMyItems, toggleFavorite, addToCart, createOrder, createItemReport, setMessage } =
  useMarketplace()

const featuredIndex = ref(0)
const reportDialogVisible = ref(false)
const reportTarget = ref(null)
const reportSubmitting = ref(false)
const publishedItemCount = computed(() => state.myItems.filter(item => item.status !== 'DELETED').length)
let featuredTimer = 0

const panelEntries = computed(() => {
  if (isAdmin.value) {
    return [
      { to: '/messages', label: '消息' },
      { to: '/items', label: '商品' },
      { to: '/admin', label: '审核' },
      { to: '/profile', label: '我的' }
    ]
  }
  const entries = [
    { to: '/cart', label: '购物车' },
    { to: '/orders', label: '订单' },
    { to: '/messages', label: '消息' },
    { to: '/profile', label: '我的' }
  ]
  return entries
})
const popularItems = computed(() =>
  [...state.items].sort((first, second) => {
    const favoriteGap = Number(second.favoriteCount ?? 0) - Number(first.favoriteCount ?? 0)
    if (favoriteGap !== 0) return favoriteGap
    return new Date(second.publishTime || second.createdAt || 0).getTime() - new Date(first.publishTime || first.createdAt || 0).getTime()
  })
)
const featuredDeals = computed(() => popularItems.value.slice(0, 5))
const activeFeaturedDeal = computed(() => featuredDeals.value[featuredIndex.value] || null)
const popularHeroItem = computed(() => featuredDeals.value[0] || null)
const recommendationItems = computed(() => popularItems.value.slice(0, 8))
const previewWantedPosts = computed(() => state.wantedPosts.slice(0, 3))
const currentCategoryLabel = computed(() => state.filters.category || '全部商品')
const heroMetrics = computed(() => [
  { label: '在售商品', value: state.items.length },
  { label: '公开求购', value: state.wantedPosts.length },
  { label: state.token ? '我的收藏' : '商品分类', value: state.token ? state.favorites.length : Math.max(categoryOptions.value.length - 1, 0) }
])
const guestHeroMetrics = computed(() => [
  { label: '在售商品', value: state.items.length },
  { label: '最高收藏', value: popularHeroItem.value?.favoriteCount ?? 0 },
  { label: '当前校区', value: state.campusLocation }
])

onMounted(async () => {
  if (state.token) {
    await Promise.all([loadProfile(), loadMyItems(), loadWanted()])
  }
  if (!state.items.length) {
    await loadItems()
  }
  startFeaturedRotation()
})

onBeforeUnmount(() => {
  window.clearInterval(featuredTimer)
})

watch(featuredDeals, deals => {
  if (!deals.length) {
    featuredIndex.value = 0
    window.clearInterval(featuredTimer)
    return
  }
  if (featuredIndex.value >= deals.length) {
    featuredIndex.value = 0
  }
  startFeaturedRotation()
})

async function selectCategory(category) {
  state.filters.category = category === '全部商品' ? '' : category
  await router.push({ path: '/items', query: buildItemQuery() })
  await loadItems()
}

async function selectCampus(campus) {
  if (state.campusLocation === campus) return
  state.campusLocation = campus
  await loadItems()
}

async function handleFavorite(item) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    router.push('/profile')
    return
  }
  await toggleFavorite(item)
}

async function handleOrder(item) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    router.push('/profile')
    return
  }
  await createOrder(item)
}

async function handleReport(item) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    router.push('/profile')
    return
  }
  if (isOwnItem(item)) {
    return
  }
  reportTarget.value = item
  reportDialogVisible.value = true
}

function closeReportDialog() {
  if (reportSubmitting.value) {
    return
  }
  reportDialogVisible.value = false
  reportTarget.value = null
}

async function submitReport(reason) {
  if (!reportTarget.value || reportSubmitting.value) {
    return
  }
  reportSubmitting.value = true
  const success = await createItemReport(reportTarget.value.id, reason)
  reportSubmitting.value = false
  if (success) {
    closeReportDialog()
  }
}

function handleAddToCart(item) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    router.push('/profile')
    return
  }
  addToCart(item)
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function statusText(status) {
  const map = {
    ON_SALE: '在售',
    PENDING_REVIEW: '待审',
    OFF_SHELF: '下架',
    SOLD: '已售'
  }
  return map[status] || status || '在售'
}

function isOwnItem(item) {
  const currentUserId = Number(state.currentUser.userId || state.profile.userId || 0)
  return currentUserId > 0 && Number(item?.sellerId || 0) === currentUserId
}

function isOutOfStock(item) {
  return Number(item?.stock ?? 0) < 1 || String(item?.status) === 'SOLD'
}

function stockText(item) {
  return isOutOfStock(item) ? '已售罄' : `${Number(item?.stock ?? 0)} 件`
}

function shiftFeatured(step) {
  if (!featuredDeals.value.length) return
  featuredIndex.value = (featuredIndex.value + step + featuredDeals.value.length) % featuredDeals.value.length
  startFeaturedRotation()
}

function setFeaturedIndex(index) {
  featuredIndex.value = index
  startFeaturedRotation()
}

function buildItemQuery() {
  const query = {}
  if (state.filters.keyword.trim()) query.keyword = state.filters.keyword.trim()
  if (state.filters.category.trim()) query.category = state.filters.category.trim()
  if (state.campusLocation.trim()) query.campus = state.campusLocation.trim()
  return query
}

async function goItems() {
  await router.push({ path: '/items', query: buildItemQuery() })
  await loadItems()
}

async function refreshItems() {
  await loadItems()
}

function goAudit() {
  router.push('/admin')
}

function goPublish() {
  if (!state.token) {
    router.push('/profile')
    return
  }
  router.push({ path: '/publish', query: { mode: 'item' } })
}

function goWanted() {
  if (!state.token) {
    router.push('/profile')
    return
  }
  router.push({ path: '/publish', query: { mode: 'wanted' } })
}

function openItemDetail(itemId) {
  router.push(`/item/${itemId}`)
}

function startFeaturedRotation() {
  window.clearInterval(featuredTimer)
  if (featuredDeals.value.length <= 1) return
  featuredTimer = window.setInterval(() => {
    featuredIndex.value = (featuredIndex.value + 1) % featuredDeals.value.length
  }, 3500)
}
</script>
