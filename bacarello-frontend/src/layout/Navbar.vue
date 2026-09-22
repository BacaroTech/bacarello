<script lang="ts" setup>
import { ref, watch, useTemplateRef, nextTick } from 'vue';
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

import Input from '@/components/ui/input/Input.vue';
import Button from '@/components/ui/button/Button.vue';
import UserDropDown from '@/components/userComponent/UserDropDown.vue';
import { LayoutGrid, Search } from 'lucide-vue-next';


interface DropDownItem {
  id: string
  label: string
  action?: string
  disabled?: boolean
  route?: boolean
  submenu?: DropDownItem[]
}
type DropDownPosition = "left" | "right"
export interface DropDownConfig {
  id: string
  position: DropDownPosition
  label: string
  items: DropDownItem[]
}


const inputVal = ref('')
const dropDowns: DropDownConfig[] = [{
  id: "menu",
  position: "left",
  label: "Menu",
  items: [
    { id: "home", label: "Home", route: true },
    { id: "boards", label: "Boards" },
    { id: "templates", label: "Templates" }
  ]
},
{
  id: "user",
  position: "right",
  label: "My Account",
  items: [
    { id: "user", label: "Profile", route: true },
    { id: "theme", label: "Theme", submenu: [{ id: 'light', label: 'Light' }, { id: 'dark', label: 'Dark' }] },
    { id: "settings", label: "Settings" },
    { id: "logout", label: "Logout" }
  ]
}]
</script>
<template>
  <div class="flex justify-between py-1 w-full items-center navbar-bg">
    <div class="flex space-x-3">
      <DropdownMenu v-for="drop in dropDowns.filter(d => d.position === 'left')">
        <DropdownMenuTrigger class="ml-4">
          <LayoutGrid :size="20" />
        </DropdownMenuTrigger>
        <DropdownMenuContent class="mt-2 z-999">
          <DropdownMenuItem v-for="item in drop.items" :key="item.id">
            <router-link class="flex-1" :to="{ name: item.id, }" v-if="item?.route">
              {{ item.label }}
            </router-link>
            <span v-else>{{ item.label }}</span>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <img src="/images/logo_nero.png" alt="bacaro logo" class="h-12">
    </div>
    <div class="flex space-x-2 items-center">
      <router-link class="flex-1" :to="{ name: 'search', }">
        <Search class="md:hidden flex" />
      </router-link>

      <Input v-model="inputVal" placeholder="Search" class="w-96 hidden md:flex input-bg" />
      <Button>Create</Button>
    </div>
    <UserDropDown :drop-downs="dropDowns.filter(d => d.position === 'right')" />
  </div>
</template>