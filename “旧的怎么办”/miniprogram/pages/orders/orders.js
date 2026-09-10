const app = getApp()
const service = require('../../utils/service')
const helpers = require('../../utils/helpers')

const STATUS_TABS = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待发货', value: 'PAID' },
  { label: '待收货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '支付失败', value: 'PAYMENT_FAILED' }
]

Page({
  countdownTimer: null,
  expiredRefreshing: false,
  data: {
    loggedIn: false,
    profile: null,
    statusTabs: STATUS_TABS,
    currentStatus: '',
    orders: []
  },

  onShow() {
    app.restoreSession()
    const loggedIn = !!app.globalData.token
    this.setData({ loggedIn })
    if (loggedIn) {
      this.loadOrders()
    } else {
      this.stopCountdownTimer()
      this.setData({ orders: [] })
    }
  },

  onHide() {
    this.stopCountdownTimer()
  },

  onUnload() {
    this.stopCountdownTimer()
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/profile' })
  },

  async loadOrders() {
    try {
      wx.showLoading({ title: '加载中' })
      const query = this.data.currentStatus ? `?status=${encodeURIComponent(this.data.currentStatus)}` : ''
      const [orders, profile] = await Promise.all([
        service.request({ path: `/orders${query}` }),
        service.request({ path: '/profile' })
      ])
      const currentUserId = Number(profile?.userId || app.globalData.user?.userId || 0)
      const normalizedOrders = this.decorateOrders(orders || [], currentUserId)
      this.setData({
        orders: normalizedOrders,
        profile
      })
      this.startCountdownTimer()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  decorateOrders(orders, currentUserId, now = Date.now()) {
    return (orders || []).map(order => {
      const normalized = helpers.normalizeOrder(order)
      const isBuyer = Number(normalized.buyerId || 0) === currentUserId
      const isSeller = Number(normalized.sellerId || 0) === currentUserId
      const status = String(normalized.status || '').toUpperCase()
      const paymentRemainingSeconds = helpers.getPaymentRemainingSeconds(normalized, now)
      return {
        ...normalized,
        paymentRemainingSeconds,
        paymentCountdownText: helpers.paymentCountdownText(normalized, now),
        statusText: helpers.orderStatusText(normalized.status),
        paymentMethodText: helpers.paymentMethodText(normalized.paymentMethod),
        createdAtText: helpers.formatDateTime(normalized.createdAt),
        roleText: isSeller ? '卖出订单' : '买入订单',
        canPay: isBuyer && status === 'PENDING_PAYMENT' && paymentRemainingSeconds > 0,
        canCancel: isBuyer && status === 'PENDING_PAYMENT' && paymentRemainingSeconds > 0,
        canShip: isSeller && status === 'PAID',
        canComplete: isBuyer && status === 'SHIPPED',
        canHide: ['COMPLETED', 'CANCELLED', 'PAYMENT_FAILED', 'REFUNDED'].includes(status)
      }
    })
  },

  startCountdownTimer() {
    this.stopCountdownTimer()
    if (!this.data.orders.some(order => String(order.status || '').toUpperCase() === 'PENDING_PAYMENT')) {
      return
    }
    this.countdownTimer = setInterval(() => {
      const now = Date.now()
      const currentUserId = Number(this.data.profile?.userId || app.globalData.user?.userId || 0)
      const updatedOrders = this.decorateOrders(this.data.orders, currentUserId, now)
      this.setData({ orders: updatedOrders })
      const hasExpiredPending = updatedOrders.some(order =>
        String(order.status || '').toUpperCase() === 'PENDING_PAYMENT' && order.paymentRemainingSeconds <= 0
      )
      if (hasExpiredPending && !this.expiredRefreshing) {
        this.expiredRefreshing = true
        this.loadOrders().finally(() => {
          this.expiredRefreshing = false
        })
      }
    }, 1000)
  },

  stopCountdownTimer() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },

  onStatusTap(event) {
    this.setData({ currentStatus: event.currentTarget.dataset.status || '' })
    this.loadOrders()
  },

  async payOrder(event) {
    const orderId = Number(event.currentTarget.dataset.id)
    try {
      const selected = await new Promise((resolve, reject) => {
        wx.showActionSheet({
          itemList: ['微信支付', '支付宝', '校园卡支付'],
          success: resolve,
          fail: reject
        })
      })
      const methodMap = ['WECHAT', 'ALIPAY', 'CAMPUS_CARD']
      await service.request({
        path: '/orders/pay',
        method: 'PATCH',
        data: {
          orderId,
          paymentMethod: methodMap[selected.tapIndex] || 'WECHAT'
        }
      })
      wx.showToast({ title: '支付成功', icon: 'success' })
      this.loadOrders()
    } catch (error) {
      if (error && error.errMsg && error.errMsg.includes('cancel')) return
      wx.showToast({ title: error.message || '支付失败', icon: 'none' })
    }
  },

  async cancelOrder(event) {
    const orderId = Number(event.currentTarget.dataset.id)
    try {
      await service.request({
        path: '/orders/cancel',
        method: 'PATCH',
        data: {
          orderId,
          cancelReason: '买家主动取消'
        }
      })
      wx.showToast({ title: '订单已取消', icon: 'success' })
      this.loadOrders()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async shipOrder(event) {
    const orderId = Number(event.currentTarget.dataset.id)
    try {
      await service.request({
        path: '/orders/ship',
        method: 'PATCH',
        data: { orderId }
      })
      wx.showToast({ title: '已标记发货', icon: 'success' })
      this.loadOrders()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async completeOrder(event) {
    const orderId = Number(event.currentTarget.dataset.id)
    try {
      await service.request({
        path: '/orders/complete',
        method: 'PATCH',
        data: { orderId }
      })
      wx.showToast({ title: '已确认收货', icon: 'success' })
      this.loadOrders()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async hideOrder(event) {
    const orderId = Number(event.currentTarget.dataset.id)
    try {
      await service.request({
        path: `/orders/${orderId}`,
        method: 'DELETE'
      })
      wx.showToast({ title: '订单记录已隐藏', icon: 'success' })
      this.loadOrders()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  }
})
