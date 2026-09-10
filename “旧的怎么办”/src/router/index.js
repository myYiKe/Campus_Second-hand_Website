import { createRouter, createWebHistory } from 'vue-router'

import AdminPage from '../pages/AdminPage.vue'
import CartPage from '../pages/CartPage.vue'
import HomePage from '../pages/HomePage.vue'
import ItemDetailPage from '../pages/ItemDetailPage.vue'
import ItemListPage from '../pages/ItemListPage.vue'
import MessagesPage from '../pages/MessagesPage.vue'
import OrdersPage from '../pages/OrdersPage.vue'
import ProfilePage from '../pages/ProfilePage.vue'
import PublishPage from '../pages/PublishPage.vue'
import WechatAuthPage from '../pages/WechatAuthPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomePage },
    { path: '/items', name: 'items', component: ItemListPage },
    { path: '/item/:id', name: 'item-detail', component: ItemDetailPage, props: true },
    { path: '/cart', name: 'cart', component: CartPage },
    { path: '/publish', name: 'publish', component: PublishPage },
    { path: '/orders', name: 'orders', component: OrdersPage },
    { path: '/messages', name: 'messages', component: MessagesPage },
    { path: '/profile', name: 'profile', component: ProfilePage },
    { path: '/wechat-auth', name: 'wechat-auth', component: WechatAuthPage },
    { path: '/admin', name: 'admin', component: AdminPage }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

export default router
