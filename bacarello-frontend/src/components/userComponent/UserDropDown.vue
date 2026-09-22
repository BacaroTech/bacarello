<script lang="ts" setup>
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'

import { Sun, Moon } from 'lucide-vue-next'
import { useColorMode } from '@vueuse/core'
const mode = useColorMode()
import type { DropDownConfig } from '@/layout/Navbar.vue'
import { Bell, Ellipsis } from 'lucide-vue-next';
import { ref } from "vue"
import Button from '../ui/button/Button.vue'
import UserLogin from './userLogin.vue'
import { useUserAuth } from '@/stores/userAuth'

const activePanel = ref("root")
const auth = useUserAuth()
const loginModalOpen = ref(false)

defineProps<{ dropDowns: DropDownConfig[] }>()
function toggleTheme(color: unknown) {
    if (typeof color !== 'string') return

    mode.value = color as "dark" | "auto" | "light"
}

function handleAuthAction() {
    if (auth.isAuthenticated) {
        auth.logoutUser()
        activePanel.value = "root"
        return
    }

    loginModalOpen.value = true
}
</script>
<template>
    <UserLogin v-model:open="loginModalOpen" />
    <!-- Desktop Right Menu -->
    <div class="hidden md:flex items-center space-x-2 mr-2">
        <Bell :size="18" />
        <DropdownMenu v-for="drop in dropDowns" :key="drop.id">
            <DropdownMenuTrigger class="hover:bg-gray-300">
                <img src="/images/profile.png" alt="" class="border rounded-lg h-8 w-8" />
            </DropdownMenuTrigger>
            <DropdownMenuContent class="w-56 mt-4 z-999">
                <DropdownMenuLabel>{{ drop.label }}</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <template v-for="item in drop.items" :key="item.id">
                    <DropdownMenuItem v-if="item.route" as-child>
                        <router-link :to="{ name: item.id, params: { id: 'andrea' } }" class="w-full">
                            {{ item.label }}
                        </router-link>
                    </DropdownMenuItem>
                    <DropdownMenuSub v-else-if="item.submenu">
                        <DropdownMenuSubTrigger>
                            <Sun
                                class="h-[1.2rem] w-[1.2rem] rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
                            <Moon
                                class="absolute h-[1.2rem] w-[1.2rem] rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
                            <span>
                                {{ item.label }}
                            </span>
                        </DropdownMenuSubTrigger>
                        <DropdownMenuSubContent class="mr-1">
                            <RadioGroup v-model="mode" @update:model-value="toggleTheme" class="p-1">
                                <div v-for="submenu in item.submenu" :key="submenu.id"
                                class="flex hover-class items-center space-x-2 cursor-pointer" @click="toggleTheme(submenu.id)">
                                    <RadioGroupItem :id="submenu.id" :value="submenu.id"
                                        @change="toggleTheme(submenu.id)" />
                                        <Sun class="h-[1.2rem] w-[1.2rem]" v-if="submenu.id == 'light'" />
                                        <Moon class="h-[1.2rem] w-[1.2rem]" v-else />
                                    <span>
                                        {{ submenu.label }}
                                    </span>
                                </div>
                            </RadioGroup>
                        </DropdownMenuSubContent>
                    </DropdownMenuSub>
                    <DropdownMenuItem v-else-if="item.id != 'logout'">
                        {{ item.label }}
                    </DropdownMenuItem>
                    <Button v-if="item.id == 'logout'" class="m-1 w-[calc(100%-0.5rem)]" @click="handleAuthAction">
                        {{ auth.isAuthenticated ? 'Logout' : 'Log in' }}
                    </Button>

                </template>
            </DropdownMenuContent>

        </DropdownMenu>
    </div>
    <!-- Mobile Menu -->
    <div class="md:hidden relative">
        <DropdownMenu>
            <DropdownMenuTrigger>
                <Ellipsis class="h-6 w-6" @click="activePanel = 'root'" />
            </DropdownMenuTrigger>
            <DropdownMenuContent class="w-48 p-2">
                <!-- ROOT PANEL -->
                <template v-if="activePanel === 'root'">
                    <DropdownMenuItem v-for="drop in dropDowns" :key="drop.id" @select.prevent="activePanel = drop.id"
                        class="flex items-center gap-2">
                        <img src="/images/profile.png" class="h-8 w-8 rounded-lg border" />
                        {{ drop.label }}
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem>
                        <Bell :size="18" />
                    </DropdownMenuItem>
                </template>
                <!-- ACCOUNT PANEL -->
                <template v-for="drop in dropDowns" :key="drop.id">
                    <template v-if="activePanel === drop.id">
                        <DropdownMenuItem @select.prevent="activePanel = 'root'">
                            ← Back
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem v-for="item in drop.items.filter((item) => item.id !== 'logout')" :key="item.id">
                            <router-link class="flex-1" :to="{ name: item.id, params: { id: 'andrea' } }"
                                v-if="item?.route">
                                {{ item.label }}
                            </router-link>
                            <span v-else>
                                {{ item.label }}
                            </span>
                        </DropdownMenuItem>
                        <DropdownMenuItem v-if="drop.items.some((item) => item.id === 'logout')" @select.prevent="handleAuthAction">
                            {{ auth.isAuthenticated ? 'Logout' : 'Log in' }}
                        </DropdownMenuItem>
                    </template>
                </template>

            </DropdownMenuContent>
        </DropdownMenu>
    </div>

</template>
