<template>
  <div class="page-stack">
    <section class="floor-card guest-home-shell">
      <div class="floor-header">
        <div>
          <h3>四川大学校园二手</h3>
          <p class="muted">未登录时直接看热门商品和校区信息，不再堆叠无关入口。</p>
        </div>
        <RouterLink class="search-btn link-btn" to="/profile">登录 / 注册</RouterLink>
      </div>

      <div class="campus-strip">
        <span>校区定位</span>
        <div class="campus-switcher">
          <button
            v-for="campus in campusOptions"
            :key="campus"
            :class="['campus-chip', { active: campusLocation === campus }]"
            @click="$emit('select-campus', campus)"
          >
            {{ campus }}
          </button>
        </div>
      </div>

      <div class="guest-home-hero">
        <div class="guest-home-copy">
          <p class="hero-subtitle">校园二手更直接</p>
          <h2>{{ campusLocation === '望江校区' ? '望江校内热门闲置' : '江安校内热门闲置' }}</h2>
          <p class="helper-text guest-home-desc">
            直接浏览高收藏商品，按校区筛选，想买再登录，不再保留右侧欢迎卡片和冗余功能入口。
          </p>
          <div class="hero-tags">
            <span>热门收藏</span>
            <span>校内面交</span>
            <span>{{ campusLocation }}</span>
          </div>
          <div class="profile-auth-actions guest-home-actions">
            <button class="search-btn" @click="$emit('go-items')">浏览商品</button>
            <RouterLink class="outline-btn link-btn" to="/profile">去登录</RouterLink>
          </div>
        </div>
        <div class="guest-home-stats">
          <div v-for="metric in guestHeroMetrics" :key="metric.label" class="summary-card">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </div>
        </div>
      </div>

      <RouterLink v-if="popularHeroItem" :to="`/item/${popularHeroItem.id}`" class="deal-card channel-link guest-highlight-card">
        <div class="carousel-copy">
          <span class="deal-label">{{ popularHeroItem.campus || campusLocation }}</span>
          <strong>{{ popularHeroItem.title }}</strong>
          <p>{{ popularHeroItem.description || '成色良好，校内交易。' }}</p>
          <div class="carousel-meta">
            <span>{{ popularHeroItem.category || '未分类' }}</span>
            <span>收藏 {{ popularHeroItem.favoriteCount }}</span>
          </div>
          <div class="deal-price">￥{{ popularHeroItem.price }}</div>
        </div>
        <img v-if="popularHeroItem.imageUrl" :src="popularHeroItem.imageUrl" :alt="popularHeroItem.title" class="deal-image carousel-image" />
      </RouterLink>
    </section>

    <section class="recommend-floor">
      <div class="floor-header">
        <div>
          <h3>热门收藏</h3>
          <p class="muted">未登录时默认展示收藏数更高的商品。</p>
        </div>
        <div class="floor-actions">
          <span class="floor-campus-tag">{{ campusLocation }}</span>
          <button class="outline-btn" :disabled="loadingItems" @click="$emit('refresh-items')">
            {{ loadingItems ? '刷新中...' : '刷新商品' }}
          </button>
        </div>
      </div>

      <div class="goods-grid">
        <article v-for="item in recommendationItems" :key="item.id" class="goods-card clickable-card" @click="$emit('open-item-detail', item.id)">
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
            <div class="goods-meta">收藏 {{ item.favoriteCount ?? 0 }}</div>
            <div class="goods-meta">{{ formatDate(item.publishTime || item.createdAt) }}</div>
            <div class="goods-meta">库存 {{ stockText(item) }}</div>
            <div class="goods-footer">
              <strong>￥{{ item.price }}</strong>
              <div class="inline-actions">
                <button class="mini-outline" @click.stop="$emit('favorite', item)">
                  {{ favoriteIds.has(Number(item.id)) ? '已收藏' : '收藏' }}
                </button>
                <button class="mini-outline" @click.stop="$emit('report', item)">举报</button>
                <button class="mini-outline" :disabled="isOutOfStock(item)" @click.stop="$emit('add-cart', item)">
                  {{ isOutOfStock(item) ? '已售罄' : '加购' }}
                </button>
                <button class="mini-solid" :disabled="isOutOfStock(item)" @click.stop="$emit('order', item)">
                  {{ isOutOfStock(item) ? '已售罄' : '立即下单' }}
                </button>
              </div>
            </div>
          </div>
        </article>
        <div v-if="!recommendationItems.length" class="empty-state">
          {{ loadingItems ? '商品加载中...' : '暂时没有查到符合条件的商品。' }}
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'

defineProps({
  campusLocation: {
    type: String,
    required: true
  },
  guestHeroMetrics: {
    type: Array,
    required: true
  },
  popularHeroItem: {
    type: Object,
    default: null
  },
  recommendationItems: {
    type: Array,
    required: true
  },
  loadingItems: {
    type: Boolean,
    default: false
  },
  favoriteIds: {
    type: Object,
    required: true
  },
  formatDate: {
    type: Function,
    required: true
  },
  statusText: {
    type: Function,
    required: true
  },
  stockText: {
    type: Function,
    required: true
  },
  isOutOfStock: {
    type: Function,
    required: true
  }
})

defineEmits(['select-campus', 'go-items', 'refresh-items', 'open-item-detail', 'favorite', 'report', 'add-cart', 'order'])

const campusOptions = ['望江校区', '江安校区']
</script>
