<template>
  <section class="page-stack">
    <section class="floor-card">
      <div class="floor-header">
        <div>
          <h3>购物车</h3>
        </div>
        <div class="floor-actions">
          <button class="mini-outline" :disabled="!state.cart.length" @click="handleClearCart">清空</button>
          <button class="outline-btn" @click="loadItems">刷新</button>
        </div>
      </div>

      <div v-if="state.token && state.cart.length" class="cart-layout">
        <section class="list-stack">
          <article
            v-for="item in state.cart"
            :key="item.id"
            :class="['cart-item-card', { disabled: isUnavailable(item) }]"
          >
            <label class="cart-check">
              <input v-model="selectedIds" type="checkbox" :value="Number(item.id)" :disabled="isUnavailable(item)" />
            </label>
            <div class="cart-thumb" @click="openItem(item.id)">
              <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title" />
              <span v-else>校园二手</span>
            </div>
            <div class="cart-item-main">
              <button class="goods-title-button" @click="openItem(item.id)">
                <strong>{{ item.title }}</strong>
              </button>
              <div class="cart-item-meta">
                <span>{{ item.category || '未分类' }}</span>
                <span>{{ item.sellerName || '校园用户' }}</span>
                <span>{{ stockText(item) }}</span>
              </div>
              <div class="cart-item-actions">
                <strong>￥{{ item.price }}</strong>
                <button class="mini-outline" @click="removeFromCart(item.id)">移除</button>
              </div>
            </div>
          </article>
        </section>

        <aside class="cart-summary-card">
          <div class="summary-card">
            <span>已选商品</span>
            <strong>{{ selectedItems.length }}</strong>
          </div>
          <div class="summary-card">
            <span>合计金额</span>
            <strong>￥{{ totalPrice }}</strong>
          </div>
          <button class="search-btn" :disabled="!selectedItems.length || checkingOut" @click="handleCheckout">
            {{ checkingOut ? '结算中...' : '去结算' }}
          </button>
        </aside>
      </div>

      <div v-else-if="state.token" class="empty-state">
        购物车为空
        <RouterLink class="outline-btn link-btn inline-link-btn" to="/items">去逛商品</RouterLink>
      </div>

      <div v-else class="empty-state">
        请先登录
        <RouterLink class="outline-btn link-btn inline-link-btn" to="/profile">去登录</RouterLink>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useMarketplace } from '../composables/useMarketplace'

const router = useRouter()
const { state, loadItems, fetchItemDetail, removeFromCart, clearCart, checkoutCart, cleanupInvalidCartItems, setMessage } = useMarketplace()
const selectedIds = ref([])
const checkingOut = ref(false)

const selectedItems = computed(() => {
  const idSet = new Set(selectedIds.value.map(id => Number(id)))
  return state.cart.filter(item => idSet.has(Number(item.id)) && !isUnavailable(item))
})

const totalPrice = computed(() =>
  selectedItems.value.reduce((sum, item) => sum + Number(item.price || 0), 0).toFixed(2)
)

onMounted(async () => {
  await loadItems()
  await cleanupInvalidCartItems({ notify: true })
  syncSelection()
})

watch(
  () => state.cart,
  () => {
    syncSelection()
  },
  { deep: true }
)

function syncSelection() {
  selectedIds.value = state.cart.filter(item => !isUnavailable(item)).map(item => Number(item.id))
}

function isUnavailable(item) {
  return Number(item?.stock ?? 0) < 1 || String(item?.status) === 'SOLD'
}

function stockText(item) {
  return isUnavailable(item) ? '已售罄' : `库存 ${Number(item?.stock ?? 0)}`
}

async function openItem(itemId) {
  try {
    await fetchItemDetail(itemId)
    router.push(`/item/${itemId}`)
  } catch (error) {
    removeFromCart(itemId)
    setMessage('该商品已失效，已从购物车移除', 'info')
  }
}

function handleClearCart() {
  clearCart()
  setMessage('购物车已清空', 'success')
}

async function handleCheckout() {
  if (!selectedItems.value.length) return
  checkingOut.value = true
  try {
    await checkoutCart(selectedIds.value)
  } finally {
    checkingOut.value = false
  }
}
</script>
