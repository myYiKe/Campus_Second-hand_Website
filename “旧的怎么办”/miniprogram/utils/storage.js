const { STORAGE_KEYS, DEFAULT_ADDRESS, DEFAULT_SETTINGS } = require('./constants')

function readJson(key, fallback) {
  try {
    const value = wx.getStorageSync(key)
    return value || value === 0 || value === false ? value : fallback
  } catch (error) {
    return fallback
  }
}

function writeJson(key, value) {
  wx.setStorageSync(key, value)
  return value
}

function getSession() {
  return {
    token: readJson(STORAGE_KEYS.token, ''),
    user: readJson(STORAGE_KEYS.user, null)
  }
}

function setSession(auth) {
  writeJson(STORAGE_KEYS.token, auth.token || '')
  writeJson(STORAGE_KEYS.user, auth.user || null)
}

function clearSession() {
  wx.removeStorageSync(STORAGE_KEYS.token)
  wx.removeStorageSync(STORAGE_KEYS.user)
}

function getAddress() {
  return { ...DEFAULT_ADDRESS, ...readJson(STORAGE_KEYS.address, {}) }
}

function setAddress(address) {
  return writeJson(STORAGE_KEYS.address, { ...getAddress(), ...address })
}

function getSettings() {
  return { ...DEFAULT_SETTINGS, ...readJson(STORAGE_KEYS.settings, {}) }
}

function setSettings(settings) {
  return writeJson(STORAGE_KEYS.settings, { ...getSettings(), ...settings })
}

function getCart() {
  return readJson(STORAGE_KEYS.cart, []) || []
}

function setCart(cart) {
  return writeJson(STORAGE_KEYS.cart, cart || [])
}

module.exports = {
  readJson,
  writeJson,
  getSession,
  setSession,
  clearSession,
  getAddress,
  setAddress,
  getSettings,
  setSettings,
  getCart,
  setCart
}
