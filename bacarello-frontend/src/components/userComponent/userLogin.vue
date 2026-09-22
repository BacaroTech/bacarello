<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useUserAuth } from '@/stores/userAuth'
import { useRoute, useRouter } from 'vue-router'

const open = defineModel<boolean>('open', { default: false })

const auth = useUserAuth()
const route = useRoute()
const router = useRouter()
const isSubmitting = ref(false)
const errorMessage = ref('')
const mode = ref<'register' | 'login'>('register')
const isDesktop = useMediaQuery('(min-width: 640px)')
const form = reactive({
  email: '',
  displayName: '',
  password: '',
})

const title = computed(() => (mode.value === 'register' ? 'Create account' : 'Sign in'))
const description = computed(() =>
  mode.value === 'register'
    ? 'Register a Bacarello account to save boards and cards.'
    : 'Sign in with your email and password.',
)
const submitLabel = computed(() => {
  if (isSubmitting.value) return mode.value === 'register' ? 'Creating...' : 'Signing in...'

  return mode.value === 'register' ? 'Create account' : 'Sign in'
})
const Modal = computed(() => ({
  Root: isDesktop.value ? Dialog : Drawer,
  Content: isDesktop.value ? DialogContent : DrawerContent,
  Header: isDesktop.value ? DialogHeader : DrawerHeader,
  Title: isDesktop.value ? DialogTitle : DrawerTitle,
  Description: isDesktop.value ? DialogDescription : DrawerDescription,
  Footer: isDesktop.value ? DialogFooter : DrawerFooter,
}))

function resetForm() {
  form.email = ''
  form.displayName = ''
  form.password = ''
  errorMessage.value = ''
}

function toggleMode() {
  mode.value = mode.value === 'register' ? 'login' : 'register'
  errorMessage.value = ''
}

async function submitAuth() {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    const submittedMode = mode.value

    if (submittedMode === 'register') {
      await auth.registerAndLoginUser({
        email: form.email,
        displayName: form.displayName,
        password: form.password,
      })
    } else {
      await auth.loginUser({
        email: form.email,
        password: form.password,
      })
    }

    open.value = false
    if (submittedMode === 'login' && typeof route.query.redirect === 'string') {
      await router.replace(route.query.redirect)
    }
    resetForm()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Authentication failed'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <component :is="Modal.Root" v-model:open="open">
    <component
      :is="Modal.Content"
      class="max-h-[calc(100svh-2rem)] overflow-y-auto sm:max-w-[425px]"
      :class="{ 'px-4 pb-8': !isDesktop }"
    >
      <form class="grid gap-5" @submit.prevent="submitAuth">
        <component :is="Modal.Header">
          <component :is="Modal.Title">{{ title }}</component>
          <component :is="Modal.Description">
            {{ description }}
          </component>
        </component>
        <div class="grid gap-4">
          <div class="grid gap-3">
            <Label for="email">Username</Label>
            <Input
              id="email"
              v-model="form.email"
              autocomplete="email"
              name="email"
              placeholder="you@example.com"
              required
              type="email"
            />
          </div>
          <div v-if="mode === 'register'" class="grid gap-3">
            <Label for="displayName">Name</Label>
            <Input
              id="displayName"
              v-model="form.displayName"
              autocomplete="name"
              name="displayName"
              placeholder="Andrea"
              required
            />
          </div>
          <div class="grid gap-3">
            <Label for="password">Password</Label>
            <Input
              id="password"
              v-model="form.password"
              autocomplete="current-password"
              name="password"
              required
              type="password"
            />
          </div>
          <p v-if="errorMessage" class="text-sm text-destructive">
            {{ errorMessage }}
          </p>
        </div>
        <component :is="Modal.Footer" class="gap-2 sm:gap-0">
          <Button type="button" variant="outline" @click="toggleMode">
            {{ mode === 'register' ? 'Sign in instead' : 'Create account' }}
          </Button>
          <Button type="submit" :disabled="isSubmitting">
            {{ submitLabel }}
          </Button>
        </component>
      </form>
    </component>
  </component>
</template>
