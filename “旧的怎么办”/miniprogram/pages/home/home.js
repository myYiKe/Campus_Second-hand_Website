const app = getApp()
const service = require('../../utils/service')
const helpers = require('../../utils/helpers')
const { CAMPUS_OPTIONS } = require('../../utils/constants')

Page({
  data: {
    loggedIn: false,
    user: null,
    profile: null,
    keyword: '',
    currentCategory: '',
    categories: ['全部商品'],
    currentCampus: CAMPUS_OPTIONS[0],
    campusOptions: CAMPUS_OPTIONS,
    quickEntries: [
      { key: 'items', title: '逛商品', desc: '像京东一样逛闲置' },
      { key: 'publish', title: '发闲置', desc: '快速发布商品' },
      { key: 'messages', title: '聊一聊', desc: '和买家卖家沟通' },
      { key: 'cart', title: '购物车', desc: '统一结算待买商品' }
    ],
    items: [],
    wantedPosts: [],
    featuredItems: [],
    loading: false
  },

  onShow() {
    app.restoreSession()
    this.setData({
      loggedIn: !!app.globalData.token,
      user: app.globalData.user
    })
    this.loadPageData()
  },

  async loadPageData() {
    this.setData({ loading: true })
    try {
      const query = []
      if (this.data.keyword.trim()) query.push(`keyword=${encodeURIComponent(this.data.keyword.trim())}`)
      if (this.data.currentCategory) query.push(`category=${encodeURIComponent(this.data.currentCategory)}`)
      if (this.data.currentCampus && this.data.currentCampus !== '全部校区') {
        query.push(`campus=${encodeURIComponent(this.data.currentCampus)}`)
      }
      const suffix = query.length ? `?${query.join('&')}` : ''
      const tasks = [
        service.request({ path: `/items${suffix}` }),
        service.request({ path: '/wanted' })
      ]
      if (this.data.loggedIn) {
        tasks.push(service.request({ path: '/profile' }))
      }
      const [items, wantedPosts, profile = null] = await Promise.all(tasks)
      const normalizedItems = (items || []).map(helpers.normalizeItem)
      this.setData({
        items: normalizedItems,
        featuredItems: normalizedItems.slice(0, 6),
        wantedPosts: (wantedPosts || []).map(helpers.normalizeWanted).slice(0, 4),
        categories: helpers.pickCategoryOptions(normalizedItems),
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

  onCampusChange(event) {
    this.setData({ currentCampus: this.data.campusOptions[event.detail.value] })
    this.loadPageData()
  },

  onSearch() {
    this.loadPageData()
  },

  onCategoryTap(event) {
    const category = event.currentTarget.dataset.category
    this.setData({ currentCategory: category === '全部商品' ? '' : category })
    this.loadPageData()
  },

  onQuickTap(event) {
    const key = event.currentTarget.dataset.key
    const tabPages = ['items', 'publish', 'cart']
    if (tabPages.includes(key)) {
      wx.switchTab({ url: `/pages/${key}/${key}` })
      return
    }
    if (key === 'messages') {
      wx.navigateTo({ url: '/pages/messages/messages' })
    }
  },

  openProfile() {
    wx.switchTab({ url: '/pages/profile/profile' })
  },

  goItems() {
    wx.switchTab({ url: '/pages/items/items' })
  },

  viewItem(event) {
    const itemId = Number(event.currentTarget.dataset.id)
    wx.navigateTo({ url: `/pages/item-detail/item-detail?id=${itemId}` })
  },
})
