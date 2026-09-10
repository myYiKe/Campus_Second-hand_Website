const app = getApp()
const service = require('../../utils/service')
const storage = require('../../utils/storage')
const helpers = require('../../utils/helpers')

Page({
  data: {
    loggedIn: false,
    profile: null,
    itemId: 0,
    item: null,
    gallery: [],
    selectedImage: '',
    favoriteItemIds: [],
    reportReason: ''
  },

  onLoad(options) {
    this.setData({ itemId: Number(options.id || 0) })
  },

  onShow() {
    app.restoreSession()
    this.setData({ loggedIn: !!app.globalData.token })
    this.loadPageData()
  },

  async loadPageData() {
    if (!this.data.itemId) return
    try {
      const tasks = [service.request({ path: `/items/${this.data.itemId}` })]
      if (this.data.loggedIn) {
        tasks.push(service.request({ path: '/favorites' }))
        tasks.push(service.request({ path: '/profile' }))
      }
      const [item, favorites = [], profile = null] = await Promise.all(tasks)
      const normalized = helpers.normalizeItem(item)
      this.setData({
        item: normalized,
        gallery: normalized.imageUrls,
        selectedImage: normalized.imageUrls[0] || normalized.imageUrl || '',
        favoriteItemIds: (favorites || []).map(entry => Number(entry.itemId || 0)),
        profile
      })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  selectImage(event) {
    this.setData({ selectedImage: event.currentTarget.dataset.url })
  },

  onReportInput(event) {
    this.setData({ reportReason: event.detail.value })
  },

  openMessages() {
    if (!this.ensureLogin()) return
    wx.setStorageSync('message_context', {
      itemId: this.data.item.id,
      itemTitle: this.data.item.title || '',
      sellerId: this.data.item.sellerId || 0,
      sellerName: this.data.item.sellerName || '校园用户'
    })
    wx.switchTab({ url: '/pages/messages/messages' })
  },

  async toggleFavorite() {
    if (!this.ensureLogin()) return
    const item = this.data.item
    const favoriteSet = new Set(this.data.favoriteItemIds)
    try {
      if (favoriteSet.has(item.id)) {
        await service.request({ path: `/favorites/${item.id}`, method: 'DELETE' })
        favoriteSet.delete(item.id)
        wx.showToast({ title: '已取消收藏', icon: 'success' })
      } else {
        await service.request({ path: '/favorites', method: 'POST', data: { itemId: item.id } })
        favoriteSet.add(item.id)
        wx.showToast({ title: '收藏成功', icon: 'success' })
      }
      this.setData({ favoriteItemIds: Array.from(favoriteSet) })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  addCart() {
    if (!this.ensureLogin()) return
    const item = this.data.item
    const currentUserId = Number(this.data.profile?.userId || 0)
    if (currentUserId && currentUserId === Number(item.sellerId || 0)) {
      wx.showToast({ title: '自己的商品无需加入购物车', icon: 'none' })
      return
    }
    const cart = storage.getCart()
    if (cart.some(entry => Number(entry.id) === item.id)) {
      wx.showToast({ title: '该商品已在购物车中', icon: 'none' })
      return
    }
    storage.setCart([helpers.buildCartItem(item), ...cart])
    wx.showToast({ title: '已加入购物车', icon: 'success' })
  },

  async createOrder() {
    if (!this.ensureLogin()) return
    const profile = this.data.profile || await this.loadProfileSilently()
    const item = this.data.item
    if (!profile?.campusVerified) {
      wx.showToast({ title: '请先完成校园认证', icon: 'none' })
      return
    }
    if (Number(profile.userId || 0) === Number(item.sellerId || 0)) {
      wx.showToast({ title: '不能下单自己的商品', icon: 'none' })
      return
    }
    const address = storage.getAddress()
    if (!address.receiver || !address.phone || !address.detail) {
      wx.showToast({ title: '请先完善收货地址', icon: 'none' })
      wx.switchTab({ url: '/pages/profile/profile' })
      return
    }
    try {
      await service.request({
        path: '/orders',
        method: 'POST',
        data: {
          itemId: item.id,
          receiverName: address.receiver,
          receiverPhone: address.phone,
          receiverCampus: address.campus,
          receiverDetail: address.detail
        }
      })
      wx.showToast({ title: '已创建待支付订单', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async submitReport() {
    if (!this.ensureLogin()) return
    if (!this.data.reportReason.trim()) {
      wx.showToast({ title: '请填写举报原因', icon: 'none' })
      return
    }
    try {
      await service.request({
        path: '/reports',
        method: 'POST',
        data: {
          targetType: 'ITEM',
          targetId: this.data.item.id,
          reason: this.data.reportReason.trim()
        }
      })
      this.setData({ reportReason: '' })
      wx.showToast({ title: '举报已提交', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async loadProfileSilently() {
    try {
      const profile = await service.request({ path: '/profile' })
      this.setData({ profile })
      return profile
    } catch (error) {
      return null
    }
  },

  ensureLogin() {
    if (this.data.loggedIn) return true
    wx.showToast({ title: '请先登录', icon: 'none' })
    wx.switchTab({ url: '/pages/profile/profile' })
    return false
  }
})
