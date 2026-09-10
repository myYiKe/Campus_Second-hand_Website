<template>
  <section class="page-stack">
    <section class="floor-card">
      <div class="campus-strip top-campus-strip">
        <span>校区</span>
        <div class="campus-switcher">
          <button
            v-for="campus in campusOptions"
            :key="campus"
            :class="['campus-chip', { active: currentCampusLabel === campus }]"
            @click="selectCampus(campus)"
          >
            {{ campus }}
          </button>
        </div>
      </div>

      <div class="floor-header">
        <div>
          <h3>{{ pageTitle }}</h3>
        </div>
        <div class="floor-actions">
          <span class="floor-campus-tag">{{ currentCampusLabel }}</span>
          <button class="outline-btn" :disabled="state.loading.items" @click="loadItems">
            {{ state.loading.items ? '刷新中...' : '刷新商品' }}
          </button>
        </div>
      </div>

      <div class="filter-summary-row">
        <span class="summary-chip">搜索词：{{ state.filters.keyword || '全部商品' }}</span>
        <span class="summary-chip">当前分类：{{ currentCategoryLabel }}</span>
        <span class="summary-chip">校区：{{ currentCampusLabel }}</span>
        <button class="mini-outline" @click="resetFilters">重置筛选</button>
      </div>

      <div class="floor-category-list">
        <button
          v-for="category in categoryOptions"
          :key="category"
          :class="['floor-category-chip', { active: currentCategoryLabel === category }]"
          @click="selectCategory(category)"
        >
          {{ category }}
        </button>
      </div>

      <div class="goods-grid">
        <article v-for="item in state.items" :key="item.id" class="goods-card clickable-card" @click="openItemDetail(item.id)">
          <RouterLink :to="`/item/${item.id}`" class="goods-cover goods-link">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title" class="goods-image" />
            <span v-else class="goods-placeholder">暂无图片</span>
            <span class="goods-badge">{{ item.category || '未分类' }}</span>
            <span class="goods-status">{{ statusText(item.status) }}</span>
          </RouterLink>
          <div class="goods-body">
            <RouterLink :to="`/item/${item.id}`" class="goods-title-link">
              <h4>{{ item.title }}</h4>
            </RouterLink>
            <p class="goods-desc">{{ item.description || '成色良好，校内交易。' }}</p>
            <div class="goods-meta">{{ item.sellerName || '校园用户' }}</div>
            <div class="goods-meta">{{ formatDate(item.publishTime || item.createdAt) }}</div>
            <div class="goods-meta">库存 {{ stockText(item) }}</div>
            <div class="goods-footer">
              <strong>￥{{ item.price }}</strong>
              <div class="inline-actions">
                <button class="mini-outline" @click.stop="handleFavorite(item)">
                  {{ favoriteIds.has(Number(item.id)) ? '已收藏' : '收藏' }}
                </button>
                <button v-if="isOwnItem(item)" class="mini-outline" type="button" disabled>
                  我的商品
                </button>
                <template v-else>
                  <button class="mini-outline" @click.stop="handleReport(item)">
                    举报
                  </button>
                  <button class="mini-outline" :disabled="isOutOfStock(item)" @click.stop="handleAddToCart(item)">
                    {{ isOutOfStock(item) ? '已售罄' : '加购' }}
                  </button>
                  <button class="mini-solid" :disabled="isOutOfStock(item)" @click.stop="handleOrder(item)">
                    {{ isOutOfStock(item) ? '已售罄' : '立即下单' }}
                  </button>
                </template>
              </div>
            </div>
          </div>
        </article>
        <div v-if="!state.items.length" class="empty-state">
          {{ state.loading.items ? '商品加载中...' : '暂时没有查到符合条件的商品。' }}
        </div>
      </div>
    </section>

    <ReportDialog
      :visible="reportDialogVisible"
      :item-title="reportTarget?.title || ''"
      :submitting="reportSubmitting"
      @cancel="closeReportDialog"
      @submit="submitReport"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import ReportDialog from '../components/ReportDialog.vue'
import { useMarketplace } from '../composables/useMarketplace'

const campusOptions = ['全部校区', '望江校区', '江安校区']

const route = useRoute()
const router = useRouter()
const { state, categoryOptions, favoriteIds, loadItems, toggleFavorite, addToCart, createOrder, createItemReport, setMessage } = useMarketplace()
const reportDialogVisible = ref(false)
const reportTarget = ref(null)
const reportSubmitting = ref(false)

const currentCategoryLabel = computed(() => state.filters.category || '全部商品')
const currentCampusLabel = computed(() => state.campusLocation || '全部校区')
const currentCampusValue = computed(() => (state.campusLocation === '全部校区' ? '' : state.campusLocation))
const pageTitle = computed(() => (state.filters.keyword ? `“${state.filters.keyword}” 的搜索结果` : '商品页'))
onMounted(async () => {
  await syncRouteToFilters()
})

watch(
  () => route.query,
  async () => {
    await syncRouteToFilters()
  }
)

async function syncRouteToFilters() {
  state.filters.keyword = readQueryValue(route.query.keyword)
  state.filters.category = readQueryValue(route.query.category)
  const campus = readQueryValue(route.query.campus)
  state.campusLocation = campusOptions.includes(campus) && campus !== '全部校区' ? campus : ''
  await loadItems({ campus: state.campusLocation })
}

function readQueryValue(value) {
  return Array.isArray(value) ? value[0] || '' : value || ''
}

function buildListQuery() {
  const query = {}
  if (state.filters.keyword.trim()) query.keyword = state.filters.keyword.trim()
  if (state.filters.category.trim()) query.category = state.filters.category.trim()
  if (currentCampusValue.value.trim()) query.campus = currentCampusValue.value.trim()
  return query
}

async function selectCategory(category) {
  state.filters.category = category === '全部商品' ? '' : category
  await router.push({ path: '/items', query: buildListQuery() })
  await loadItems()
}

async function selectCampus(campus) {
  const nextCampus = campus === '全部校区' ? '' : campus
  if (state.campusLocation === nextCampus) return
  state.campusLocation = nextCampus
  await router.push({ path: '/items', query: buildListQuery() })
  await loadItems({ campus: nextCampus })
}

async function resetFilters() {
  state.filters.keyword = ''
  state.filters.category = ''
  await router.push({ path: '/items', query: buildListQuery() })
  await loadItems({ campus: currentCampusValue.value })
}

async function handleFavorite(item) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    window.alert('请先登录后再收藏商品，正在为你跳转到登录页面。')
    router.push('/profile')
    return
  }
  await toggleFavorite(item)
}

async function handleOrder(item) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    window.alert('请先登录后再下单，正在为你跳转到登录页面。')
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

function isOutOfStock(item) {
  return Number(item?.stock ?? 0) < 1 || String(item?.status) === 'SOLD'
}

function isOwnItem(item) {
  const currentUserId = Number(state.currentUser.userId || state.profile.userId || 0)
  return currentUserId > 0 && Number(item?.sellerId || 0) === currentUserId
}

function stockText(item) {
  return isOutOfStock(item) ? '已售罄' : `${Number(item?.stock ?? 0)} 件`
}

function openItemDetail(itemId) {
  router.push(`/item/${itemId}`)
}
</script>
