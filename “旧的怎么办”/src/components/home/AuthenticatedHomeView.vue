<template>
  <div class="page-stack">
    <section class="hero-layout">
      <section class="hero-center">
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

        <div class="hero-banner">
          <div>
            <p class="hero-subtitle">四川大学二手</p>
            <h2>{{ campusLocation === '望江校区' ? '望江校内随手淘' : '江安同校更方便' }}<br />毕业季低价转</h2>
            <div class="hero-tags">
              <span>望江</span>
              <span>江安</span>
              <span>现货</span>
            </div>
          </div>
          <div class="hero-stats">
            <div v-for="metric in heroMetrics" :key="metric.label">
              <strong>{{ metric.value }}</strong>
              <span>{{ metric.label }}</span>
            </div>
          </div>
        </div>

        <div class="deal-carousel" :class="{ empty: !featuredDeals.length }">
          <template v-if="activeFeaturedDeal">
            <button class="carousel-nav prev" @click="$emit('shift-featured', -1)">‹</button>
            <RouterLink :to="`/item/${activeFeaturedDeal.id}`" class="deal-card channel-link carousel-card">
              <div class="carousel-copy">
                <span class="deal-label">{{ activeFeaturedDeal.campus || campusLocation }}</span>
                <strong>{{ activeFeaturedDeal.title }}</strong>
                <p>{{ activeFeaturedDeal.description || '成色良好，校内交易。' }}</p>
                <div class="carousel-meta">
                  <span>{{ activeFeaturedDeal.category || '未分类' }}</span>
                  <span>{{ statusText(activeFeaturedDeal.status) }}</span>
                </div>
                <div class="deal-price">￥{{ activeFeaturedDeal.price }}</div>
              </div>
              <img v-if="activeFeaturedDeal.imageUrl" :src="activeFeaturedDeal.imageUrl" :alt="activeFeaturedDeal.title" class="deal-image carousel-image" />
            </RouterLink>
            <button class="carousel-nav next" @click="$emit('shift-featured', 1)">›</button>
            <div class="carousel-dots">
              <button
                v-for="(item, index) in featuredDeals"
                :key="item.id"
                :class="['carousel-dot', { active: featuredIndex === index }]"
                @click="$emit('jump-featured', index)"
              />
            </div>
          </template>
          <div v-else class="empty-inline">暂无推荐商品。</div>
        </div>
      </section>

      <aside class="user-panel">
        <h3>我的</h3>
        <div class="profile-card">
          <strong>{{ profile.nickname || currentUser.nickname || '校园用户' }}</strong>
          <span>{{ profile.campusVerified ? '已认证' : '待认证' }}</span>
        </div>
        <div class="mini-stat-grid">
          <div>
            <strong>{{ profile.favoriteCount ?? 0 }}</strong>
            <span>收藏</span>
          </div>
          <div>
            <strong>{{ publishedItemCount }}</strong>
            <span>发布</span>
          </div>
        </div>
        <div class="user-quick-grid">
          <RouterLink v-for="entry in panelEntries" :key="entry.to" :to="entry.to" class="panel-link link-btn">
            {{ entry.label }}
          </RouterLink>
        </div>
      </aside>
    </section>

    <section class="recommend-floor">
      <div class="floor-header">
        <div>
          <h3>好物推荐</h3>
        </div>
        <div class="floor-actions">
          <span class="floor-campus-tag">{{ campusLocation }}</span>
          <button class="outline-btn" :disabled="loadingItems" @click="$emit('refresh-items')">
            {{ loadingItems ? '刷新中...' : '刷新商品' }}
          </button>
        </div>
      </div>

      <div class="floor-category-list">
        <button
          v-for="category in categoryOptions"
          :key="`floor-${category}`"
          :class="['floor-category-chip', { active: currentCategoryLabel === category }]"
          @click="$emit('select-category', category)"
        >
          {{ category }}
        </button>
      </div>

      <div class="featured-row">
        <button class="featured-banner large featured-link" @click="$emit('go-items')">
          <p class="hero-subtitle">今日主推</p>
          <h4>{{ currentCategoryLabel === '全部商品' ? '热门推荐' : currentCategoryLabel }}</h4>
        </button>
        <button v-if="isAdmin" class="featured-banner featured-link" @click="$emit('go-audit')">
          <p class="hero-subtitle">审核</p>
          <h4>去审核</h4>
        </button>
        <template v-else>
          <button class="featured-banner featured-link" @click="$emit('go-publish')">
            <p class="hero-subtitle">发布</p>
            <h4>去发布</h4>
          </button>
          <button class="featured-banner featured-link" @click="$emit('go-wanted')">
            <p class="hero-subtitle">求购</p>
            <h4>去求购</h4>
          </button>
        </template>
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
                <button v-if="isOwnItem(item)" class="mini-outline" type="button" disabled>
                  我的商品
                </button>
                <template v-else>
                  <button class="mini-outline" @click.stop="$emit('report', item)">举报</button>
                  <button class="mini-outline" :disabled="isOutOfStock(item)" @click.stop="$emit('add-cart', item)">
                    {{ isOutOfStock(item) ? '已售罄' : '加购' }}
                  </button>
                  <button class="mini-solid" :disabled="isOutOfStock(item)" @click.stop="$emit('order', item)">
                    {{ isOutOfStock(item) ? '已售罄' : '立即下单' }}
                  </button>
                </template>
              </div>
            </div>
          </div>
        </article>
        <div v-if="!recommendationItems.length" class="empty-state">
          {{ loadingItems ? '商品加载中...' : '暂时没有查到符合条件的商品。' }}
        </div>
      </div>
    </section>

    <section class="market-showcase market-showcase-single">
      <section class="floor-card">
        <div class="floor-header">
          <div>
            <h3>求购</h3>
          </div>
        </div>
        <div class="wanted-grid compact-list">
          <article v-for="post in previewWantedPosts" :key="post.id" class="wanted-card compact-wanted-card">
            <strong>{{ post.title }}</strong>
            <p class="wanted-meta">{{ post.description || '校内沟通' }}</p>
            <div class="wanted-footer">
              <div class="wanted-topline">
                <span>￥{{ post.budget }}</span>
                <small>{{ post.publisherName || post.publisher || '校园用户' }}</small>
              </div>
              <RouterLink
                class="mini-outline link-btn wanted-contact-btn"
                :to="{ path: '/messages', query: { wantedId: post.id, wantedTitle: post.title } }"
              >
                去联系
              </RouterLink>
            </div>
          </article>
          <div v-if="!previewWantedPosts.length" class="empty-inline">
            {{ loadingWanted ? '加载中...' : '暂无求购' }}
          </div>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'

defineProps({
  isAdmin: {
    type: Boolean,
    default: false
  },
  campusLocation: {
    type: String,
    required: true
  },
  categoryOptions: {
    type: Array,
    required: true
  },
  currentCategoryLabel: {
    type: String,
    required: true
  },
  heroMetrics: {
    type: Array,
    required: true
  },
  featuredDeals: {
    type: Array,
    required: true
  },
  activeFeaturedDeal: {
    type: Object,
    default: null
  },
  featuredIndex: {
    type: Number,
    required: true
  },
  panelEntries: {
    type: Array,
    required: true
  },
  profile: {
    type: Object,
    required: true
  },
  currentUser: {
    type: Object,
    required: true
  },
  publishedItemCount: {
    type: Number,
    required: true
  },
  recommendationItems: {
    type: Array,
    required: true
  },
  previewWantedPosts: {
    type: Array,
    required: true
  },
  favoriteIds: {
    type: Object,
    required: true
  },
  loadingItems: {
    type: Boolean,
    default: false
  },
  loadingWanted: {
    type: Boolean,
    default: false
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
  isOwnItem: {
    type: Function,
    required: true
  },
  isOutOfStock: {
    type: Function,
    required: true
  }
})

defineEmits([
  'select-category',
  'select-campus',
  'shift-featured',
  'jump-featured',
  'refresh-items',
  'go-items',
  'go-audit',
  'go-publish',
  'go-wanted',
  'open-item-detail',
  'favorite',
  'report',
  'add-cart',
  'order'
])

const campusOptions = ['望江校区', '江安校区']
</script>
