const app = getApp()
const service = require('../../utils/service')
const storage = require('../../utils/storage')
const helpers = require('../../utils/helpers')
const { CAMPUS_OPTIONS } = require('../../utils/constants')

Page({
  data: {
    loggedIn: false,
    profile: null,
    keyword: '',
    currentCategory: '',
    currentCampus: CAMPUS_OPTIONS[0],
    campusOptions: CAMPUS_OPTIONS,
    categories: ['全部商品'],
    items: [],
    favoriteItemIds: [],
    loading: false
  },

  onShow() {
    app.restoreSession()
    this.setData({ loggedIn: !!app.globalData.token })
    this.loadItems()
  },

  async loadItems() {
    this.setData({ loading: true })
    try {
      const query = []
      if (this.data.keyword.trim()) query.push(`keyword=${encodeURIComponent(this.data.keyword.trim())}`)
      if (this.data.currentCategory) query.push(`category=${encodeURIComponent(this.data.currentCategory)}`)
      if (this.data.currentCampus && this.data.currentCampus !== '全部校区') {
        query.push(`campus=${encodeURIComponent(this.data.currentCampus)}`)
      }
      const suffix = query.length ? `?${query.join('&')}` : ''
      const tasks = [service.request({ path: `/items${suffix}` })]
      if (this.data.loggedIn) {
        tasks.push(service.request({ path: '/favorites' }))
        tasks.push(service.request({ path: '/profile' }))
      }
      const [items, favorites = [], profile = null] = await Promise.all(tasks)
      const normalizedItems = (items || []).map(helpers.normalizeItem)
      this.setData({
        items: normalizedItems,
        categories: helpers.pickCategoryOptions(normalizedItems),
        favoriteItemIds: (favorites || []).map(entry => Number(entry.itemId || 0)),
        profile
      })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value })
  },

  onSearch() {
    this.loadItems()
  },

  onCampusChange(event) {
    this.setData({ currentCampus: this.data.campusOptions[event.detail.value] })
    this.loadItems()
  },

  onCategoryTap(event) {
    const category = event.currentTarget.dataset.category
    this.setData({ currentCategory: category === '全部商品' ? '' : category })
    this.loadItems()
  },

  viewItem(event) {
    wx.navigateTo({ url: `/pages/item-detail/item-detail?id=${Number(event.currentTarget.dataset.id)}` })
  },

  async toggleFavorite(event) {
    const item = this.data.items.find(entry => Number(entry.id) === Number(event.currentTarget.dataset.id))
    if (!item) return
    if (!this.ensureLogin()) return
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

  addCart(event) {
    const item = this.data.items.find(entry => Number(entry.id) === Number(event.currentTarget.dataset.id))
    if (!item) return
    if (!this.ensureLogin()) return
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

  ensureLogin() {
    if (this.data.loggedIn) return true
    wx.showToast({ title: '请先登录', icon: 'none' })
    wx.switchTab({ url: '/pages/profile/profile' })
    return false
  }
})
