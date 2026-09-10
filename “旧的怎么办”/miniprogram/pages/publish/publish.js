const app = getApp()
const { CATEGORY_OPTIONS } = require('../../utils/constants')

Page({
  data: {
    loggedIn: false,
    form: {
      title: '',
      category: '数码设备',
      price: '',
      description: '',
      stock: '1'
    },
    categoryOptions: CATEGORY_OPTIONS,
    imageFiles: [],
    uploadedImageUrls: []
  },

  onShow() {
    app.restoreSession()
    this.setData({
      loggedIn: !!app.globalData.token
    })
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/profile' })
  },

  onFieldInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({
      [`form.${field}`]: event.detail.value
    })
  },

  onCategoryChange(event) {
    const category = this.data.categoryOptions[event.detail.value] || this.data.form.category
    this.setData({
      'form.category': category
    })
  },

  async chooseImages() {
    try {
      const currentCount = this.data.imageFiles.length
      if (currentCount >= 6) {
        wx.showToast({ title: '最多上传 6 张图片', icon: 'none' })
        return
      }
      const result = await new Promise((resolve, reject) => {
        wx.chooseMedia({
          count: 6 - currentCount,
          mediaType: ['image'],
          sourceType: ['album', 'camera'],
          success: resolve,
          fail: reject
        })
      })
      const chosenFiles = (result.tempFiles || []).map(file => ({
        path: file.tempFilePath,
        size: file.size || 0
      }))
      this.setData({
        imageFiles: [...this.data.imageFiles, ...chosenFiles]
      })
    } catch (error) {
      if (error && error.errMsg && error.errMsg.includes('cancel')) return
      wx.showToast({ title: '选择图片失败', icon: 'none' })
    }
  },

  previewImage(event) {
    const current = event.currentTarget.dataset.url
    wx.previewImage({
      current,
      urls: this.data.imageFiles.map(file => file.path)
    })
  },

  removeImage(event) {
    const index = Number(event.currentTarget.dataset.index)
    const imageFiles = [...this.data.imageFiles]
    imageFiles.splice(index, 1)
    this.setData({ imageFiles })
  },

  async uploadImages() {
    const imageUrls = []
    for (const file of this.data.imageFiles) {
      const result = await app.uploadFile({
        path: '/items/upload',
        filePath: file.path,
        name: 'file'
      })
      imageUrls.push(result.imageUrl)
    }
    this.setData({ uploadedImageUrls: imageUrls })
    return imageUrls
  },

  async submitPublish() {
    const { title, category, price, description, stock } = this.data.form
    if (!title || !category || !price || !description || !stock) {
      wx.showToast({ title: '请完整填写商品信息', icon: 'none' })
      return
    }
    if (!this.data.imageFiles.length) {
      wx.showToast({ title: '请至少上传 1 张商品图片', icon: 'none' })
      return
    }
    try {
      wx.showLoading({ title: '提交中' })
      const imageUrls = await this.uploadImages()
      await app.request({
        path: '/items',
        method: 'POST',
        data: {
          title,
          category,
          price: Number(price),
          stock: Number(stock),
          description,
          imageUrls
        }
      })
      this.setData({
        form: {
          title: '',
          category: CATEGORY_OPTIONS[0],
          price: '',
          description: '',
          stock: '1'
        },
        imageFiles: [],
        uploadedImageUrls: []
      })
      wx.showToast({ title: '发布成功', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  }
})
