const storage = require('./storage')
const helpers = require('./helpers')
const { API_BASE } = require('./constants')

function getHeaders(auth = true) {
  const headers = {
    'content-type': 'application/json'
  }
  const session = storage.getSession()
  if (auth && session.token) {
    headers.Authorization = `Bearer ${session.token}`
  }
  return headers
}

function handleUnauthorized() {
  const app = getApp()
  if (app && typeof app.clearSession === 'function') {
    app.clearSession()
    return
  }
  storage.clearSession()
}

function request({ path, method = 'GET', data = {}, auth = true, header = {} }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${API_BASE}${path}`,
      method,
      data,
      header: {
        ...getHeaders(auth),
        ...header
      },
      success: (res) => {
        const payload = res.data || {}
        if (res.statusCode >= 200 && res.statusCode < 300 && payload.success !== false) {
          resolve(payload.data)
          return
        }
        if (res.statusCode === 401) {
          handleUnauthorized()
        }
        reject(new Error(payload.message || `请求失败: ${res.statusCode}`))
      },
      fail: () => reject(new Error('请求失败，请确认后端已启动且开发者工具已关闭域名校验'))
    })
  })
}

function uploadFile({ path, filePath, name = 'file', formData = {}, auth = true }) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${API_BASE}${path}`,
      filePath,
      name,
      formData,
      header: auth ? { Authorization: `Bearer ${storage.getSession().token}` } : {},
      success: (res) => {
        let payload = {}
        try {
          payload = JSON.parse(res.data || '{}')
        } catch (error) {
          payload = {}
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && payload.success !== false) {
          resolve(payload.data)
          return
        }
        if (res.statusCode === 401) {
          handleUnauthorized()
        }
        reject(new Error(payload.message || `上传失败: ${res.statusCode}`))
      },
      fail: () => reject(new Error('上传失败，请稍后重试'))
    })
  })
}

function applyAuthPayload(data) {
  const auth = {
    token: data.token,
    user: {
      userId: data.userId,
      nickname: data.nickname,
      avatarUrl: helpers.normalizeImageUrl(data.avatarUrl),
      isAdmin: Boolean(data.isAdmin)
    }
  }
  const app = getApp()
  if (app && typeof app.setSession === 'function') {
    app.setSession(auth)
  } else {
    storage.setSession(auth)
  }
  return auth.user
}

function loginWithWechat() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: async (loginRes) => {
        if (!loginRes.code) {
          reject(new Error('未获取到微信登录 code'))
          return
        }
        try {
          const data = await request({
            path: '/auth/wechat/login',
            method: 'POST',
            data: { code: loginRes.code },
            auth: false
          })
          resolve(applyAuthPayload(data))
        } catch (error) {
          reject(error)
        }
      },
      fail: () => reject(new Error('微信登录失败'))
    })
  })
}

module.exports = {
  request,
  uploadFile,
  applyAuthPayload,
  loginWithWechat
}
