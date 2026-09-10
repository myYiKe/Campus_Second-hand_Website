<template>
  <section class="page-stack">
    <section class="floor-card">
      <div class="floor-header">
        <div>
          <h3>微信登录入口预留</h3>
          <p class="muted">当前网页端仅保留授权码获取展示界面，不接入真实微信授权流程。</p>
        </div>
        <RouterLink class="outline-btn link-btn" to="/profile">返回登录页</RouterLink>
      </div>

      <div class="wechat-auth-grid">
        <article v-for="item in demoCodes" :key="item.code" class="summary-card wechat-auth-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.code }}</strong>
          <p class="muted">{{ item.description }}</p>
        </article>
      </div>

      <div class="notice-card">
        <strong>说明</strong>
        <p class="muted">后续若需要接入真实微信登录，可在此页面替换为扫码、授权跳转和回调处理逻辑，当前阶段用于网页端入口预留与界面展示。</p>
      </div>
    </section>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'

const demoCodes = [
  { code: 'demo001', label: '普通演示账号 1', description: '用于保留旧微信登录演示链路。' },
  { code: 'demo002', label: '普通演示账号 2', description: '用于多账号联调与消息演示。' },
  { code: 'admin001', label: '旧管理员演示码', description: '仅作兼容展示，网页端管理员请使用 admin / 123456。' }
]
</script>
