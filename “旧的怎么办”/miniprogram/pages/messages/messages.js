const app = getApp()
const service = require('../../utils/service')
const helpers = require('../../utils/helpers')

Page({
  data: {
    loggedIn: false,
    messages: [],
    context: null,
    draft: ''
  },

  onShow() {
    app.restoreSession()
    const loggedIn = !!app.globalData.token
    const context = wx.getStorageSync('message_context') || null
    this.setData({ loggedIn, context })
    if (loggedIn) {
      this.loadMessages()
    }
  },

  async loadMessages() {
    try {
      const messages = await service.request({ path: '/messages' })
      this.setData({
        messages: (messages || []).map(helpers.normalizeMessage)
      })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/profile' })
  },

  onDraftInput(event) {
    this.setData({ draft: event.detail.value })
  },

  async sendMessage() {
    if (!this.data.context?.sellerId) {
      wx.showToast({ title: '请先从商品详情进入咨询', icon: 'none' })
      return
    }
    if (!this.data.draft.trim()) {
      wx.showToast({ title: '请输入消息内容', icon: 'none' })
      return
    }
    try {
      await service.request({
        path: '/messages',
        method: 'POST',
        data: {
          itemId: Number(this.data.context.itemId || 0),
          toUserId: Number(this.data.context.sellerId || 0),
          content: this.data.draft.trim()
        }
      })
      this.setData({ draft: '' })
      await this.loadMessages()
      wx.showToast({ title: '发送成功', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  }
})
