const app = getApp()
const service = require('../../utils/service')

Page({
  data: {
    loggedIn: false,
    user: null,
    profile: null,
    authMode: 'login',
    loginForm: {
      nickname: '',
      password: ''
    },
    registerForm: {
      nickname: '',
      password: '',
      confirmPassword: ''
    },
    verifyForm: {
      studentNo: '20230001',
      campus: '望江校区'
    }
  },

  onShow() {
    app.restoreSession()
    const loggedIn = !!app.globalData.token
    this.setData({
      loggedIn,
      user: app.globalData.user
    })
    if (loggedIn) {
      this.loadProfile()
    }
  },

  switchAuthMode(event) {
    this.setData({ authMode: event.currentTarget.dataset.mode })
  },

  onLoginInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ [`loginForm.${field}`]: event.detail.value })
  },

  onRegisterInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ [`registerForm.${field}`]: event.detail.value })
  },

  async submitLogin() {
    const { nickname, password } = this.data.loginForm
    if (!nickname.trim() || !password.trim()) {
      wx.showToast({ title: '请输入昵称和密码', icon: 'none' })
      return
    }
    try {
      wx.showLoading({ title: '登录中' })
      const auth = await service.request({
        path: '/auth/login',
        method: 'POST',
        data: {
          account: nickname.trim(),
          password: password.trim(),
          loginType: 'NICKNAME'
        },
        auth: false
      })
      const user = service.applyAuthPayload(auth)
      this.setData({
        loggedIn: true,
        user,
        loginForm: { nickname: '', password: '' }
      })
      await this.loadProfile()
      wx.showToast({ title: '登录成功', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  async submitRegister() {
    const { nickname, password, confirmPassword } = this.data.registerForm
    if (!nickname.trim() || !password.trim() || !confirmPassword.trim()) {
      wx.showToast({ title: '请完整填写注册信息', icon: 'none' })
      return
    }
    if (password !== confirmPassword) {
      wx.showToast({ title: '两次输入密码不一致', icon: 'none' })
      return
    }
    try {
      wx.showLoading({ title: '注册中' })
      const auth = await service.request({
        path: '/auth/register',
        method: 'POST',
        data: {
          nickname: nickname.trim(),
          password: password.trim()
        },
        auth: false
      })
      const user = service.applyAuthPayload(auth)
      this.setData({
        loggedIn: true,
        user,
        authMode: 'login',
        registerForm: { nickname: '', password: '', confirmPassword: '' }
      })
      await this.loadProfile()
      wx.showToast({ title: '注册成功', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  async loadProfile() {
    try {
      const profile = await service.request({ path: '/profile' })
      this.setData({ profile })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  onStudentNoInput(event) {
    this.setData({ 'verifyForm.studentNo': event.detail.value })
  },

  onCampusInput(event) {
    this.setData({ 'verifyForm.campus': event.detail.value })
  },

  async submitVerify() {
    try {
      await service.request({
        path: '/auth/student/verify',
        method: 'POST',
        data: this.data.verifyForm
      })
      wx.showToast({ title: '认证成功', icon: 'success' })
      await this.loadProfile()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  openOrders() {
    wx.navigateTo({ url: '/pages/orders/orders' })
  },

  logout() {
    app.clearSession()
    this.setData({
      loggedIn: false,
      user: null,
      profile: null
    })
    wx.showToast({ title: '已退出', icon: 'success' })
  }
})
