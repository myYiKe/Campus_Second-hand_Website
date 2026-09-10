<template>
  <section class="page-stack">
    <section class="floor-card">
      <div class="floor-header">
        <div>
          <h3>发布中心</h3>
        </div>
      </div>

      <div v-if="state.token" class="profile-shortcut-grid">
        <button class="profile-shortcut-card" @click="openPublishModal('item')">
          <strong>发布闲置</strong>
        </button>
        <button class="profile-shortcut-card" @click="openPublishModal('wanted')">
          <strong>发布求购</strong>
        </button>
      </div>
      <div v-else class="empty-state">
        请先登录
        <RouterLink class="outline-btn link-btn inline-link-btn" to="/profile">去登录</RouterLink>
      </div>

      <div v-if="state.token && activeMode" class="profile-modal-backdrop" @click.self="closePublishModal">
        <section class="profile-modal-card publish-modal-card">
          <div class="floor-header profile-modal-header">
            <div>
              <h3>{{ activeMode === 'wanted' ? '发布求购' : '发布闲置' }}</h3>
            </div>
            <button class="outline-btn" @click="closePublishModal">关闭</button>
          </div>

          <div v-if="!state.profile.campusVerified" class="empty-state compact-empty-state">
            请先完成校园认证
            <RouterLink class="outline-btn link-btn inline-link-btn" to="/profile" @click="closePublishModal">去认证</RouterLink>
          </div>

          <div v-else-if="activeMode === 'item'" class="form-card compact-form-card profile-modal-body">
            <div class="form-grid">
              <label>
                <span>商品标题</span>
                <input v-model="state.publishForm.title" placeholder="例如 2024款 iPad Air 5" />
              </label>
              <label>
                <span>分类</span>
                <select v-model="state.publishForm.category">
                  <option v-for="category in publishCategoryOptions" :key="category" :value="category">
                    {{ category }}
                  </option>
                </select>
              </label>
              <label>
                <span>价格</span>
                <input v-model="state.publishForm.price" type="number" min="0" step="0.01" placeholder="0.00" />
              </label>
              <label>
                <span>库存</span>
                <input v-model="state.publishForm.stock" type="number" min="1" step="1" placeholder="1" />
              </label>
              <label class="full">
                <span>商品描述</span>
                <textarea v-model="state.publishForm.description" rows="4" placeholder="写清购入年份、成色、配件与使用情况"></textarea>
              </label>
              <label class="full">
                <span>商品图片</span>
                <input type="file" accept="image/png,image/jpeg,image/webp" multiple @change="handleFileChange" />
              </label>
            </div>
            <div v-if="state.publishForm.imageUrls.length" class="upload-preview-grid">
              <div v-for="(imageUrl, index) in state.publishForm.imageUrls" :key="`${imageUrl}-${index}`" class="upload-preview-card">
                <img :src="imageUrl" :alt="`商品图片 ${index + 1}`" />
                <button class="mini-danger" @click="removeImage(index)">删除</button>
              </div>
            </div>
            <button class="search-btn" :disabled="state.loading.publish" @click="handlePublishItem">
              {{ state.loading.publish ? '发布中...' : '提交商品' }}
            </button>
          </div>

          <div v-else class="form-card compact-form-card profile-modal-body">
            <div class="form-grid">
              <label>
                <span>求购标题</span>
                <input v-model="state.wantedForm.title" placeholder="例如 求购 9 成新小米平板 / 高数教材" />
              </label>
              <label>
                <span>预算</span>
                <input v-model="state.wantedForm.budget" type="number" min="0" step="0.01" placeholder="0.00" />
              </label>
              <label class="full">
                <span>求购描述</span>
                <textarea
                  v-model="state.wantedForm.description"
                  rows="4"
                  placeholder="写清期望成色、型号、面交校区和可接受时间"
                ></textarea>
              </label>
            </div>
            <button class="search-btn" :disabled="state.loading.wantedSubmit" @click="handleSubmitWanted">
              {{ state.loading.wantedSubmit ? '发布中...' : '提交求购' }}
            </button>
          </div>
        </section>
      </div>
    </section>

    <section class="floor-card">
      <div class="floor-header">
        <div>
          <h3>我的发布</h3>
        </div>
        <button class="outline-btn" :disabled="state.loading.myItems || !state.token" @click="loadMyItems">刷新</button>
      </div>

      <div v-if="state.token" class="goods-grid my-items-grid">
        <article v-for="item in displayMyItems" :key="item.id" class="goods-card">
          <div class="goods-cover">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title" class="goods-image" />
            <span v-else class="goods-placeholder">暂无图片</span>
            <span class="goods-badge">{{ item.category || '未分类' }}</span>
            <span class="goods-status">{{ statusText(item.status) }}</span>
          </div>
          <div class="goods-body">
            <RouterLink v-if="item.status === 'ON_SALE'" class="goods-title-link" :to="`/item/${item.id}`">
              <h4>{{ item.title }}</h4>
            </RouterLink>
            <h4 v-else>{{ item.title }}</h4>
            <p class="goods-desc">{{ item.description || '暂无说明' }}</p>
            <div class="goods-meta">价格 ￥{{ item.price }}</div>
            <div class="goods-meta">库存 {{ item.stock }} 件</div>
            <div class="goods-meta">状态 {{ statusText(item.status) }}</div>
            <div class="goods-footer">
              <strong>￥{{ item.price }}</strong>
              <div class="inline-actions">
                <button v-if="item.status !== 'ON_SALE'" class="mini-outline" @click="updateMyItemStatus(item.id, 'ON_SALE')">上架</button>
                <button class="mini-outline" @click="removeMyItem(item.id)">{{ item.status === 'ON_SALE' ? '下架' : '删除' }}</button>
              </div>
            </div>
          </div>
        </article>
        <div v-if="!displayMyItems.length" class="empty-inline">暂无发布记录。</div>
      </div>
      <div v-else class="empty-inline">请先登录。</div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useMarketplace } from '../composables/useMarketplace'

const route = useRoute()
const router = useRouter()
const activeMode = ref('item')
const { state, categoryOptions, publishItem, submitWanted, loadMyItems, updateMyItemStatus, deleteMyItem, uploadItemImages, setMessage, loadProfile } =
  useMarketplace()
const publishCategoryOptions = computed(() => categoryOptions.value.filter(category => category !== '全部商品'))
const displayMyItems = computed(() => state.myItems.filter(item => item.status !== 'DELETED'))

onMounted(async () => {
  syncMode()
  if (state.token) {
    await Promise.all([loadProfile(), loadMyItems()])
  }
})

watch(
  () => route.query.mode,
  () => {
    syncMode()
  }
)

function syncMode() {
  activeMode.value = ['item', 'wanted'].includes(route.query.mode) ? route.query.mode : ''
}

function switchMode(mode) {
  activeMode.value = mode
  router.push({ path: '/publish', query: { mode } })
}

function openPublishModal(mode) {
  switchMode(mode)
}

function closePublishModal() {
  activeMode.value = ''
  router.push({ path: '/publish' })
}

async function handleFileChange(event) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return
  try {
    const uploadedUrls = await uploadItemImages(files)
    state.publishForm.imageUrls.push(...uploadedUrls)
    setMessage(`已上传 ${uploadedUrls.length} 张图片`, 'success')
  } catch (error) {
    setMessage(error.message, 'error')
  } finally {
    event.target.value = ''
  }
}

function removeImage(index) {
  state.publishForm.imageUrls.splice(index, 1)
}

async function removeMyItem(itemId) {
  await deleteMyItem(itemId)
}

async function handlePublishItem() {
  const ok = await publishItem()
  if (ok) {
    closePublishModal()
  }
}

async function handleSubmitWanted() {
  const ok = await submitWanted()
  if (ok) {
    closePublishModal()
  }
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
</script>
