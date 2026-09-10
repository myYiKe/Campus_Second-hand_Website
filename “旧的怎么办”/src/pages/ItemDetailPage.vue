<template>
  <section class="detail-page">
    <div class="detail-breadcrumb">
      <RouterLink to="/">首页</RouterLink>
      <span>/</span>
      <span>{{ item?.category || '商品详情' }}</span>
      <span>/</span>
      <strong>{{ item?.title || '加载中' }}</strong>
    </div>

    <div v-if="item" class="detail-grid">
      <div class="detail-gallery">
        <div class="detail-cover">
          <img v-if="selectedImage" :src="selectedImage" :alt="item.title" class="detail-cover-image" />
          <span v-else class="detail-placeholder">暂无图片</span>
          <span class="goods-badge">{{ item.category || '未分类' }}</span>
          <span class="goods-status">{{ statusText(item.status) }}</span>
        </div>
        <div class="detail-thumb-row">
          <button
            v-for="(image, index) in galleryImages"
            :key="`${image}-${index}`"
            :class="['detail-thumb', { active: selectedImage === image }]"
            @click="selectedImage = image"
          >
            <img :src="image" :alt="`${item.title}-${index + 1}`" class="detail-thumb-image" />
          </button>
        </div>
      </div>

      <div class="detail-info">
        <h2>{{ item.title }}</h2>
        <div class="detail-price">￥{{ item.price }}</div>
        <p class="detail-desc">{{ item.description || '暂无补充说明' }}</p>

        <div class="purchase-card">
          <div class="purchase-row">
            <span>交易地点</span>
            <strong>{{ item.campus || '校内面交' }}</strong>
          </div>
          <div class="purchase-row">
            <span>状态</span>
            <strong>{{ statusText(item.status) }}</strong>
          </div>
          <div class="purchase-row">
            <span>剩余库存</span>
            <strong>{{ stockText(item) }}</strong>
          </div>
          <div class="purchase-row">
            <span>购物车状态</span>
            <strong>{{ cartIds.has(Number(item.id)) ? '已加入购物车' : '可加入购物车' }}</strong>
          </div>
          <div class="purchase-actions">
            <button v-if="isOwnItem(item)" class="mini-outline large-btn" type="button" disabled>
              我的商品
            </button>
            <template v-else>
              <button class="mini-outline large-btn" :disabled="isOutOfStock(item)" @click="handleAddToCart(item)">
                {{ isOutOfStock(item) ? '已售罄' : cartIds.has(Number(item.id)) ? '已在购物车' : '加入购物车' }}
              </button>
              <button class="mini-solid large-btn" :disabled="isOutOfStock(item)" @click="handleOrder(item)">
                {{ isOutOfStock(item) ? '已售罄' : '直接购买' }}
              </button>
            </template>
          </div>
        </div>

        <div class="detail-meta-list">
          <div><span>卖家</span><strong>{{ item.sellerName || '校园用户' }}</strong></div>
          <div><span>分类</span><strong>{{ item.category || '未分类' }}</strong></div>
          <div><span>发布时间</span><strong>{{ formatDate(item.publishTime || item.createdAt) }}</strong></div>
          <div><span>库存</span><strong>{{ stockText(item) }}</strong></div>
        </div>

        <div class="hero-actions">
          <button class="mini-outline large-btn" @click="handleFavorite(item)">
            {{ favoriteIds.has(Number(item.id)) ? '取消收藏' : '加入收藏' }}
          </button>
          <RouterLink
            v-if="!isOwnItem(item)"
            class="outline-btn link-btn large-btn"
            :to="{
              path: '/messages',
              query: {
                itemId: item.id,
                itemTitle: item.title,
                itemPrice: item.price,
                campus: item.campus || '校内面交',
                stock: item.stock
              }
            }"
          >
            咨询卖家
          </RouterLink>
          <button
            v-if="!isOwnItem(item)"
            class="mini-outline large-btn"
            type="button"
            @click="handleReport(item)"
          >
            举报商品
          </button>
          <RouterLink class="outline-btn link-btn large-btn" to="/orders">查看订单</RouterLink>
        </div>

        <div class="seller-card">
          <div>
            <p class="seller-label">卖家信息</p>
            <strong>{{ item.sellerName || '校园用户' }}</strong>
          </div>
          <div class="seller-metrics">
            <div>
              <span>校内认证</span>
              <strong>已完成</strong>
            </div>
            <div>
              <span>交易方式</span>
              <strong>校内面交</strong>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="loadingDetail" class="empty-state">
      商品详情加载中...
    </div>

    <div v-else class="empty-state">
      未找到该商品。
    </div>

    <section class="recommend-floor">
      <div class="floor-header">
        <div>
          <h3>猜你喜欢</h3>
        </div>
      </div>
      <div class="goods-grid">
        <article v-for="goods in relatedItems" :key="goods.id" class="goods-card clickable-card" @click="openItemDetail(goods.id)">
          <RouterLink :to="`/item/${goods.id}`" class="goods-cover goods-link">
            <img v-if="goods.imageUrl" :src="goods.imageUrl" :alt="goods.title" class="goods-image" />
            <span v-else class="goods-placeholder">暂无图片</span>
            <span class="goods-badge">{{ goods.category || '未分类' }}</span>
            <span class="goods-status">{{ statusText(goods.status) }}</span>
          </RouterLink>
          <div class="goods-body">
            <RouterLink :to="`/item/${goods.id}`" class="goods-title-link">
              <h4>{{ goods.title }}</h4>
            </RouterLink>
            <div class="goods-meta">库存 {{ stockText(goods) }}</div>
            <div class="goods-footer">
              <strong>￥{{ goods.price }}</strong>
              <button class="mini-solid" :disabled="isOutOfStock(goods)" @click.stop="handleOrder(goods)">
                {{ isOutOfStock(goods) ? '已售罄' : '下单' }}
              </button>
            </div>
          </div>
        </article>
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
import { RouterLink, useRouter } from 'vue-router'
import ReportDialog from '../components/ReportDialog.vue'
import { useMarketplace } from '../composables/useMarketplace'

const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

const router = useRouter()
const { state, favoriteIds, cartIds, loadItems, fetchItemDetail, toggleFavorite, addToCart, createOrder, createItemReport, setMessage } = useMarketplace()
const selectedImage = ref('')
const detailItem = ref(null)
const loadingDetail = ref(false)
const reportDialogVisible = ref(false)
const reportTarget = ref(null)
const reportSubmitting = ref(false)

onMounted(async () => {
  await loadDetail()
})

const item = computed(() => detailItem.value || state.items.find(goods => String(goods.id) === props.id) || null)
const galleryImages = computed(() => {
  if (!item.value) return []
  return item.value.imageUrls?.length ? item.value.imageUrls : item.value.imageUrl ? [item.value.imageUrl] : []
})
const relatedItems = computed(() => state.items.filter(goods => String(goods.id) !== props.id).slice(0, 4))

watch(
  galleryImages,
  value => {
    selectedImage.value = value[0] || ''
  },
  { immediate: true }
)

watch(
  () => props.id,
  async () => {
    await loadDetail()
  }
)

async function loadDetail() {
  const cachedItem = state.items.find(goods => String(goods.id) === props.id)
  if (cachedItem) {
    detailItem.value = cachedItem
  }

  loadingDetail.value = true
  try {
    detailItem.value = await fetchItemDetail(props.id)
    if (!state.items.length) {
      await loadItems()
    }
  } catch (error) {
    if (/商品不存在|商品已下架或不存在/i.test(String(error?.message || ''))) {
      detailItem.value = null
      setMessage('该商品已失效，已为你返回商品页', 'info')
      await router.replace('/items')
    } else if (!detailItem.value) {
      setMessage(error.message, 'error')
    }
  } finally {
    loadingDetail.value = false
  }
}

async function handleFavorite(goods) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    router.push('/profile')
    return
  }
  await toggleFavorite(goods)
}

function handleAddToCart(goods) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    router.push('/profile')
    return
  }
  addToCart(goods)
}

async function handleOrder(goods) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    router.push('/profile')
    return
  }
  await createOrder(goods)
}

async function handleReport(goods) {
  if (!state.token) {
    setMessage('请先登录', 'info')
    router.push('/profile')
    return
  }
  reportTarget.value = goods
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
    await router.replace('/items')
  }
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

function isOutOfStock(goods) {
  return Number(goods?.stock ?? 0) < 1 || String(goods?.status) === 'SOLD'
}

function stockText(goods) {
  return isOutOfStock(goods) ? '已售罄' : `${Number(goods?.stock ?? 0)} 件`
}

function isOwnItem(goods) {
  const currentUserId = Number(state.currentUser.userId || state.profile.userId || 0)
  return currentUserId > 0 && Number(goods?.sellerId || 0) === currentUserId
}

function openItemDetail(itemId) {
  router.push(`/item/${itemId}`)
}
</script>
