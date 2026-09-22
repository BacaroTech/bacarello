import { createRouter, createWebHistory } from 'vue-router'
import UserView from '@/views/UserView.vue'
import AppHome from '@/views/AppHome.vue'
import Search from '@/views/Search.vue'
import BoardView from '@/views/BoardView.vue'
import CardDetail from '@/components/CardDetail.vue'
import { useUserAuth } from '@/stores/userAuth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: AppHome,
    },
    {
      path: '/board/:id',
      name: 'board',
      component: BoardView,
      meta: { requiresAuth: true },
      children: [
        {
          path: 'card/:cardId',
          name: 'card',
          component: CardDetail,
        },
      ],
    },
    {
      path: '/search',
      name: 'search',
      component: Search,
      meta: { requiresAuth: true },
    },
    {
      path: '/user/:id',
      name: 'user',
      component: UserView,
      meta: { requiresAuth: true },
      /* children: [
        {
          path: 'profile',
          component: UserProfile,
        },
        {
          path: 'posts',
          component: UserPosts,
        },
      ], */
    },
  ],
})

router.beforeEach((to) => {
  const auth = useUserAuth()

  if (!to.matched.some((route) => route.meta.requiresAuth)) return true
  if (auth.checkAuthentication()) return true

  return {
    name: 'home',
    query: { auth: 'required', redirect: to.fullPath },
  }
})

export default router
