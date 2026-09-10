<template>
  <section class="page-stack">
    <section class="floor-card">
      <div class="floor-header">
        <div>
          <h3>审核中心</h3>
          <p class="muted">管理员仅保留商品审核、消息和资料入口，不再展示购物车、发布、订单板块。</p>
        </div>
        <button class="outline-btn" :disabled="state.loading.admin || !state.token" @click="loadAdminData">刷新审核数据</button>
      </div>

      <div v-if="state.token && isAdmin" class="summary-grid">
        <div v-for="metric in adminMetricList" :key="metric.label" class="summary-card">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </div>
      <div v-else-if="state.token" class="empty-state">
        仅管理员可查看
      </div>
      <div v-else class="empty-state">
        请先登录
        <RouterLink class="outline-btn link-btn inline-link-btn" to="/profile">去登录</RouterLink>
      </div>
    </section>

    <section v-if="state.token && isAdmin" class="floor-card">
      <div class="floor-header">
        <div>
          <h3>商品审核板块</h3>
          <p class="muted">按商品卡片查看待审举报，点击卡片可展开商品详情和举报理由后再处理。</p>
        </div>
      </div>
      <div class="goods-grid audit-goods-grid">
        <article
          v-for="report in pendingReports"
          :key="report.id"
          class="goods-card clickable-card audit-goods-card"
          @click="openReportDetail(report)"
        >
          <div class="goods-cover audit-goods-cover">
            <img v-if="report.itemImageUrl" :src="report.itemImageUrl" :alt="report.itemTitle" class="goods-image" />
            <span v-else class="goods-placeholder">待审核商品</span>
            <span class="goods-badge">{{ report.itemCategory || '未分类' }}</span>
            <span class="goods-status">待审举报</span>
          </div>
          <div class="goods-body">
            <strong>{{ report.itemTitle || `商品 #${report.targetId}` }}</strong>
            <p class="goods-desc">{{ buildPreviewText(report.itemDescription) }}</p>
            <div class="goods-meta">发布者：{{ report.sellerName || '未知用户' }}</div>
            <div class="goods-meta">举报人：{{ report.reporterName || '匿名用户' }}</div>
            <div class="goods-meta">举报时间：{{ formatDate(report.createdAt) }}</div>
            <div class="goods-footer">
              <strong>{{ formatPrice(report.itemPrice) }}</strong>
              <button class="mini-outline" type="button" @click.stop="openReportDetail(report)">
                查看审核
              </button>
            </div>
          </div>
        </article>
        <div v-if="!pendingReports.length" class="empty-state">
          当前没有待处理举报。
        </div>
      </div>
    </section>

    <section v-if="state.token && isAdmin" class="floor-card">
      <div class="floor-header">
        <div>
          <h3>纠纷处理板块</h3>
          <p class="muted">订单纠纷会在这里集中处理，管理员可先标记处理中，或直接给出退款、完结、驳回结论。</p>
        </div>
      </div>
      <div class="list-stack">
        <article
          v-for="dispute in pendingDisputes"
          :key="`dispute-${dispute.id}`"
          class="list-card clickable-card"
          @click="openDisputeDetail(dispute)"
        >
          <div>
            <strong>{{ dispute.itemTitle || `订单 #${dispute.orderId}` }}</strong>
            <p class="muted">订单号：{{ dispute.orderId }} · 投诉人：{{ dispute.complainant || '未知用户' }}</p>
            <p class="muted">当前状态：{{ disputeStatusText(dispute.status) }} · 原订单状态：{{ orderStatusText(dispute.orderStatusSnapshot) }}</p>
            <p class="muted">纠纷原因：{{ buildPreviewText(dispute.reason) }}</p>
          </div>
          <div class="inline-actions" @click.stop>
            <button class="mini-outline" @click="openDisputeDetail(dispute)">处理纠纷</button>
          </div>
        </article>
        <div v-if="!pendingDisputes.length" class="empty-inline">当前没有待处理纠纷。</div>
      </div>
    </section>

    <section v-if="state.token && isAdmin" class="floor-card user-list-card">
      <div class="floor-header">
        <div>
          <h3>用户管理</h3>
          <p class="muted">点击用户卡片可查看昵称、学号等资料。</p>
        </div>
      </div>
      <div class="list-stack">
        <div v-for="user in activeAdminUsers" :key="user.userId" class="list-card admin-user-card" @click="openUserCard(user)">
          <div>
            <strong>{{ user.nickname }}</strong>
            <p class="muted">状态：{{ user.status }} · 认证：{{ user.campusVerified ? '是' : '否' }} · 学号：{{ maskStudentNo(user.studentNo) }}</p>
          </div>
          <div class="inline-actions" @click.stop>
            <button class="mini-outline" @click="updateUserStatus(user.userId, 'ACTIVE')">恢复</button>
            <button class="mini-outline" @click="updateUserStatus(user.userId, 'WARNED')">警告</button>
            <button class="mini-danger" @click="updateUserStatus(user.userId, 'BANNED')">封禁</button>
            <button class="mini-danger" @click="handleDeleteUser(user)">删除用户</button>
          </div>
        </div>
        <div v-if="!activeAdminUsers.length" class="empty-inline">当前没有可管理的普通用户。</div>
      </div>
    </section>

    <section v-if="state.token && isAdmin" class="floor-card user-list-card">
      <div class="floor-header">
        <div>
          <h3>已删除用户</h3>
          <p class="muted">删除后的用户会移入这里归档展示，不再出现在上方管理列表。</p>
        </div>
      </div>
      <div class="list-stack">
        <div v-for="user in deletedAdminUsers" :key="`deleted-${user.userId}`" class="list-card admin-user-card deleted-user-card" @click="openUserCard(user)">
          <div>
            <strong>{{ user.nickname }}</strong>
            <p class="muted">状态：{{ user.status }} · 学号：{{ maskStudentNo(user.studentNo) }}</p>
          </div>
        </div>
        <div v-if="!deletedAdminUsers.length" class="empty-inline">当前没有已删除用户。</div>
      </div>
    </section>

    <div v-if="selectedUser" class="profile-modal-backdrop" @click.self="closeUserCard">
      <section class="profile-modal-card admin-user-modal">
        <div class="profile-modal-header">
          <div class="floor-header">
            <div>
              <h3>用户资料卡片</h3>
            </div>
            <button class="outline-btn" @click="closeUserCard">关闭</button>
          </div>
        </div>
        <div class="profile-modal-body admin-user-modal-body">
          <div class="admin-user-modal-hero">
            <div class="admin-user-avatar">{{ userInitial(selectedUser.nickname) }}</div>
            <div>
              <strong>{{ selectedUser.nickname || '未命名用户' }}</strong>
              <p class="muted">状态：{{ selectedUser.status }} · 认证：{{ selectedUser.campusVerified ? '已认证' : '未认证' }}</p>
            </div>
          </div>
          <div class="summary-grid admin-user-summary-grid">
            <div class="summary-card">
              <span>昵称</span>
              <strong>{{ selectedUser.nickname || '-' }}</strong>
            </div>
            <div class="summary-card">
              <span>学号</span>
              <strong>{{ selectedUser.studentNo || '未绑定' }}</strong>
            </div>
            <div class="summary-card">
              <span>校区</span>
              <strong>{{ selectedUser.campus || '未填写' }}</strong>
            </div>
            <div class="summary-card">
              <span>账号状态</span>
              <strong>{{ selectedUser.status }}</strong>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div v-if="selectedReport" class="profile-modal-backdrop" @click.self="closeReportDetail">
      <section class="profile-modal-card report-review-modal">
        <div class="profile-modal-header">
          <div class="floor-header">
            <div>
              <h3>审核商品</h3>
              <p class="muted">查看商品详情与举报理由后，再决定恢复上架或打回处理。</p>
            </div>
            <button class="outline-btn" @click="closeReportDetail">关闭</button>
          </div>
        </div>
        <div class="profile-modal-body report-review-body">
          <div class="report-review-layout">
            <div class="report-review-cover">
              <img
                v-if="selectedReport.itemImageUrl"
                :src="selectedReport.itemImageUrl"
                :alt="selectedReport.itemTitle || '待审核商品'"
                class="report-review-image"
              />
              <div v-else class="detail-placeholder report-review-placeholder">暂无图片</div>
            </div>
            <div class="report-review-main">
              <div class="summary-grid report-review-summary-grid">
                <div class="summary-card">
                  <span>商品名称</span>
                  <strong>{{ selectedReport.itemTitle || `商品 #${selectedReport.targetId}` }}</strong>
                </div>
                <div class="summary-card">
                  <span>价格</span>
                  <strong>{{ formatPrice(selectedReport.itemPrice) }}</strong>
                </div>
                <div class="summary-card">
                  <span>分类</span>
                  <strong>{{ selectedReport.itemCategory || '未分类' }}</strong>
                </div>
                <div class="summary-card">
                  <span>库存</span>
                  <strong>{{ stockText(selectedReport.itemStock) }}</strong>
                </div>
                <div class="summary-card">
                  <span>发布者</span>
                  <strong>{{ selectedReport.sellerName || '未知用户' }}</strong>
                </div>
                <div class="summary-card">
                  <span>校区</span>
                  <strong>{{ selectedReport.itemCampus || '未填写' }}</strong>
                </div>
              </div>

              <section class="floor-card compact-audit-panel">
                <div class="floor-header">
                  <div>
                    <h3>商品详情</h3>
                  </div>
                </div>
                <p class="audit-detail-text">{{ selectedReport.itemDescription || '卖家暂未填写更多商品描述。' }}</p>
                <p class="muted">发布时间：{{ formatDate(selectedReport.itemPublishTime) }}</p>
              </section>

              <section class="floor-card compact-audit-panel">
                <div class="floor-header">
                  <div>
                    <h3>举报信息</h3>
                  </div>
                </div>
                <p class="audit-report-reason">{{ selectedReport.reason || '未填写举报原因' }}</p>
                <p class="muted">举报人：{{ selectedReport.reporterName || '匿名用户' }}</p>
                <p class="muted">提交时间：{{ formatDate(selectedReport.createdAt) }}</p>
              </section>

              <section class="form-card compact-form-card">
                <label>
                  <span>审核说明</span>
                  <textarea v-model="reportAuditForm.reason" rows="3" placeholder="可补充管理员处理说明"></textarea>
                </label>
                <label>
                  <span>用户处理</span>
                  <select v-model="reportAuditForm.userAction">
                    <option value="NONE">不处理</option>
                    <option value="WARNED">警告发布者</option>
                    <option value="BANNED">封禁发布者</option>
                  </select>
                </label>
                <div class="modal-actions audit-decision-actions">
                  <button class="outline-btn" type="button" :disabled="state.loading.audit" @click="closeReportDetail">取消</button>
                  <button class="mini-outline" type="button" :disabled="state.loading.audit" @click="submitReportAudit('APPROVED')">
                    {{ state.loading.audit ? '处理中...' : '恢复上架' }}
                  </button>
                  <button class="mini-danger" type="button" :disabled="state.loading.audit" @click="submitReportAudit('REJECTED')">
                    {{ state.loading.audit ? '处理中...' : '打回处理' }}
                  </button>
                </div>
              </section>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div v-if="selectedDispute" class="profile-modal-backdrop" @click.self="closeDisputeDetail">
      <section class="profile-modal-card">
        <div class="profile-modal-header">
          <div class="floor-header">
            <div>
              <h3>处理订单纠纷</h3>
              <p class="muted">订单 #{{ selectedDispute.orderId }} · {{ selectedDispute.itemTitle || '未关联商品' }}</p>
            </div>
            <button class="outline-btn" @click="closeDisputeDetail">关闭</button>
          </div>
        </div>
        <div class="profile-modal-body">
          <div class="summary-grid admin-user-summary-grid">
            <div class="summary-card">
              <span>投诉人</span>
              <strong>{{ selectedDispute.complainant || '-' }}</strong>
            </div>
            <div class="summary-card">
              <span>买家</span>
              <strong>{{ selectedDispute.buyerName || selectedDispute.buyerId || '-' }}</strong>
            </div>
            <div class="summary-card">
              <span>卖家</span>
              <strong>{{ selectedDispute.sellerName || selectedDispute.sellerId || '-' }}</strong>
            </div>
            <div class="summary-card">
              <span>原订单状态</span>
              <strong>{{ orderStatusText(selectedDispute.orderStatusSnapshot) }}</strong>
            </div>
          </div>

          <section class="floor-card compact-audit-panel">
            <div class="floor-header">
              <div>
                <h3>纠纷详情</h3>
              </div>
            </div>
            <p class="audit-report-reason">{{ selectedDispute.reason || '未填写纠纷原因' }}</p>
            <p class="muted">提交时间：{{ formatDate(selectedDispute.createdAt) }}</p>
            <p v-if="selectedDispute.handleResult" class="muted">已有处理结果：{{ selectedDispute.handleResult }}</p>
          </section>

          <section class="form-card compact-form-card">
            <label>
              <span>处理动作</span>
              <select v-model="disputeHandleForm.status">
                <option value="PROCESSING">标记处理中</option>
                <option value="RESOLVED_REFUND">判定退款</option>
                <option value="RESOLVED_COMPLETE">判定交易完成</option>
                <option value="REJECTED">驳回纠纷</option>
              </select>
            </label>
            <label>
              <span>处理说明</span>
              <textarea v-model="disputeHandleForm.handleResult" rows="4" placeholder="例如：核实聊天记录后，判定卖家退款并回收订单"></textarea>
            </label>
            <div class="modal-actions audit-decision-actions">
              <button class="outline-btn" type="button" :disabled="state.loading.disputeHandle" @click="closeDisputeDetail">取消</button>
              <button class="search-btn" type="button" :disabled="state.loading.disputeHandle" @click="submitDisputeHandle">
                {{ state.loading.disputeHandle ? '处理中...' : '提交处理结果' }}
              </button>
            </div>
          </section>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useMarketplace } from '../composables/useMarketplace'

const { state, isAdmin, adminMetricList, loadAdminData, auditItem, handleDispute, updateUserStatus, deleteUser } = useMarketplace()
const pendingReports = computed(() => state.adminReports.filter(report => report.status === 'PENDING'))
const pendingDisputes = computed(() => state.adminDisputes.filter(dispute => ['PENDING', 'PROCESSING'].includes(dispute.status)))
const activeAdminUsers = computed(() => state.adminUsers.filter(user => user.status !== 'DELETED'))
const deletedAdminUsers = computed(() => state.adminUsers.filter(user => user.status === 'DELETED'))
const selectedUser = ref(null)
const selectedReport = ref(null)
const selectedDispute = ref(null)
const reportAuditForm = reactive({
  reason: '',
  userAction: 'NONE'
})
const disputeHandleForm = reactive({
  status: 'PROCESSING',
  handleResult: ''
})

async function handleDeleteUser(user) {
  const confirmed = window.confirm(`确认删除用户“${user.nickname}”吗？该操作会同步下架其商品。`)
  if (!confirmed) {
    return
  }
  await deleteUser(user.userId)
}

function openReportDetail(report) {
  selectedReport.value = report
  reportAuditForm.reason = report.reason || ''
  reportAuditForm.userAction = 'NONE'
}

function closeReportDetail() {
  if (state.loading.audit) {
    return
  }
  selectedReport.value = null
}

async function submitReportAudit(result) {
  if (!selectedReport.value) {
    return
  }
  const success = await auditItem({
    itemId: String(selectedReport.value.targetId || ''),
    result,
    reason: reportAuditForm.reason,
    userAction: reportAuditForm.userAction
  })
  if (success) {
    closeReportDetail()
  }
}

function openDisputeDetail(dispute) {
  selectedDispute.value = dispute
  disputeHandleForm.status = dispute.status === 'PROCESSING' ? 'PROCESSING' : 'RESOLVED_COMPLETE'
  disputeHandleForm.handleResult = dispute.handleResult || ''
}

function closeDisputeDetail() {
  if (state.loading.disputeHandle) {
    return
  }
  selectedDispute.value = null
}

async function submitDisputeHandle() {
  if (!selectedDispute.value) {
    return
  }
  const success = await handleDispute(
    selectedDispute.value.id,
    disputeHandleForm.status,
    disputeHandleForm.handleResult
  )
  if (success) {
    closeDisputeDetail()
  }
}

function openUserCard(user) {
  selectedUser.value = user
}

function closeUserCard() {
  selectedUser.value = null
}

function userInitial(nickname) {
  const text = String(nickname || '').trim()
  return text ? text.slice(0, 1).toUpperCase() : '用'
}

function maskStudentNo(studentNo) {
  const value = String(studentNo || '').trim()
  if (!value) {
    return '未绑定'
  }
  if (value.length <= 4) {
    return value
  }
  return `${value.slice(0, 2)}****${value.slice(-2)}`
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function formatPrice(value) {
  const price = Number(value)
  return Number.isFinite(price) ? `￥${price}` : '待定'
}

function stockText(value) {
  const stock = Number(value)
  return Number.isFinite(stock) ? `${stock} 件` : '-'
}

function buildPreviewText(value) {
  const text = String(value || '').trim()
  if (!text) {
    return '卖家暂未填写更多商品描述。'
  }
  return text.length > 34 ? `${text.slice(0, 34)}...` : text
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

function orderStatusText(value) {
  return ({
    PENDING_PAYMENT: '待支付',
    PAID: '待发货',
    SHIPPED: '待收货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    DISPUTING: '纠纷处理中',
    REFUNDED: '已退款'
  })[String(value || '').toUpperCase()] || value || '-'
}

onMounted(async () => {
  if (state.token && isAdmin.value) {
    await loadAdminData()
  }
})
</script>
