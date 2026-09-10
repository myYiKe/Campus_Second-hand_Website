const API_BASE = 'http://127.0.0.1:8080/api'
const STATIC_BASE = 'http://127.0.0.1:8080'

const STORAGE_KEYS = {
  token: 'campus_trade_token',
  user: 'campus_trade_user',
  address: 'campus_trade_address',
  settings: 'campus_trade_settings',
  cart: 'campus_trade_cart'
}

const CATEGORY_OPTIONS = [
  '数码设备',
  '学习资料',
  '宿舍电器',
  '出行代步',
  '家居用品',
  '服饰鞋包',
  '文体乐器',
  '美妆个护',
  '办公配件',
  '票券周边'
]

const CAMPUS_OPTIONS = ['全部校区', '望江校区', '江安校区']

const DEFAULT_ADDRESS = {
  receiver: '张同学',
  phone: '13800000000',
  campus: '望江校区',
  detail: '四川大学望江校区东园宿舍 3 舍'
}

const DEFAULT_SETTINGS = {
  orderNotice: true,
  messageNotice: true,
  darkMode: false
}

module.exports = {
  API_BASE,
  STATIC_BASE,
  STORAGE_KEYS,
  CATEGORY_OPTIONS,
  CAMPUS_OPTIONS,
  DEFAULT_ADDRESS,
  DEFAULT_SETTINGS
}
