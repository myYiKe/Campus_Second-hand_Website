const { API_BASE } = require('./utils/constants')
const storage = require('./utils/storage')
const service = require('./utils/service')

App({
  globalData: {
    appName: '旧的怎么办',
    apiBaseUrl: API_BASE,
    token: '',
    user: null
  },

  onLaunch() {
    // 每次打开小程序都从未登录状态开始，避免复用历史测试账号
    this.clearSession()
  },

  restoreSession() {
    const session = storage.getSession()
    this.globalData.token = session.token
    this.globalData.user = session.user
    return session
  },

  setSession(auth) {
    storage.setSession(auth)
    this.globalData.token = auth.token || ''
    this.globalData.user = auth.user || null
  },

  clearSession() {
    storage.clearSession()
    this.globalData.token = ''
    this.globalData.user = null
  },

  request(options) {
    return service.request(options)
  },

  uploadFile(options) {
    return service.uploadFile(options)
  },

  loginWithWechat() {
    return service.loginWithWechat()
  }
})
