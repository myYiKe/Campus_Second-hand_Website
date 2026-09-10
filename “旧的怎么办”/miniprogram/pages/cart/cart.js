const app = getApp()
const service = require('../../utils/service')
const storage = require('../../utils/storage')

Page({
  data: {
    loggedIn: false,
    profile: null,
    cart: [],
    selectedIds: [],
    totalPrice: 0
  },

  onShow() {
    app.restoreSession()
    this.setData({ loggedIn: !!app.globalData.token })
    this.loadCart()
    if (this.data.loggedIn) {
      this.loadProfile()
    }
  },

  loadCart() {
    const cart = storage.getCart()
    const selectedIds = cart.map(item => Number(item.id))
    this.setData({ cart, selectedIds })
    this.syncTotalPrice(cart, selectedIds)
  },

  async loadProfile() {
    try {
      const profile = await service.request({ path: '/profile' })
      this.setData({ profile })
      return profile
    } catch (error) {
      this.setData({ profile: null })
      return null
    }
  },

  toggleSelect(event) {
    const itemId = Number(event.currentTarget.dataset.id)
    const selected = new Set(this.data.selectedIds)
    if (selected.has(itemId)) {
      selected.delete(itemId)
    } else {
      selected.add(itemId)
    }
    const selectedIds = Array.from(selected)
    this.setData({ selectedIds })
    this.syncTotalPrice(this.data.cart, selectedIds)
  },

  toggleSelectAll() {
    const selectedIds = this.data.selectedIds.length === this.data.cart.length
      ? []
      : this.data.cart.map(item => Number(item.id))
    this.setData({ selectedIds })
    this.syncTotalPrice(this.data.cart, selectedIds)
  },

  removeItem(event) {
    const itemId = Number(event.currentTarget.dataset.id)
    const cart = this.data.cart.filter(item => Number(item.id) !== itemId)
    storage.setCart(cart)
    this.loadCart()
    wx.showToast({ title: '已移出购物车', icon: 'success' })
  },

  async checkoutSelected() {
    if (!this.data.loggedIn) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      wx.switchTab({ url: '/pages/profile/profile' })
      return
    }
    const profile = this.data.profile || await this.loadProfile()
    if (!profile?.campusVerified) {
      wx.showToast({ title: '请先完成校园认证', icon: 'none' })
      return
    }
    const address = storage.getAddress()
    if (!address.receiver || !address.phone || !address.detail) {
      wx.showToast({ title: '请先完善收货地址', icon: 'none' })
      wx.switchTab({ url: '/pages/profile/profile' })
      return
    }
    const selectedItems = this.data.cart.filter(item => this.data.selectedIds.includes(Number(item.id)))
    if (!selectedItems.length) {
      wx.showToast({ title: '请选择要结算的商品', icon: 'none' })
      return
    }
    const successIds = []
    for (const item of selectedItems) {
      try {
        await service.request({
          path: '/orders',
          method: 'POST',
          data: {
            itemId: Number(item.id),
            receiverName: address.receiver,
            receiverPhone: address.phone,
            receiverCampus: address.campus,
            receiverDetail: address.detail
          }
        })
        successIds.push(Number(item.id))
      } catch (error) {
        wx.showToast({ title: error.message, icon: 'none' })
      }
    }
    if (successIds.length) {
      const restCart = this.data.cart.filter(item => !successIds.includes(Number(item.id)))
      storage.setCart(restCart)
      this.loadCart()
      wx.showToast({ title: `已创建 ${successIds.length} 笔订单`, icon: 'success' })
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/orders/orders' })
      }, 400)
    }
  },

  syncTotalPrice(cart, selectedIds) {
    const totalPrice = cart
      .filter(item => selectedIds.includes(Number(item.id)))
      .reduce((sum, item) => sum + Number(item.price || 0), 0)
    this.setData({ totalPrice: Number(totalPrice.toFixed(2)) })
  }
})
