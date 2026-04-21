import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    redirect: '/downloads',
    children: [
      {
        path: '/downloads',
        name: 'Downloads',
        component: () => import('@/views/Downloads.vue'),
        meta: { title: '下载管理' }
      },
      {
        path: '/saved-messages',
        name: 'SavedMessages',
        component: () => import('@/views/SavedMessages.vue'),
        meta: { title: '收藏夹' }
      },
      {
        path: '/forward-listener',
        name: 'ForwardListener',
        component: () => import('@/views/ForwardListener.vue'),
        meta: { title: '监听转发' }
      },
      {
        path: '/forward-tasks',
        name: 'ForwardTasks',
        component: () => import('@/views/ForwardTasks.vue'),
        meta: { title: '转发任务' }
      },
      {
        path: '/config',
        name: 'Config',
        component: () => import('@/views/Config.vue'),
        meta: { title: '基础配置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  next()
})

export default router
