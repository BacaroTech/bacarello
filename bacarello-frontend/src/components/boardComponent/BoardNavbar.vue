<script lang="ts" setup>
import { ref } from 'vue'
import {DropdownMenu, DropdownMenuTrigger} from '@/components/ui/dropdown-menu'
interface User {
    id: number
    icon?: string
    name: string
}
interface BoardBar {
    title: string
    users?: User[]
}
const boardBar = ref<BoardBar>({
    title: 'Bacaro-sviluppo',
    users: [
        {
            id: 1,
            name: 'Prova'
        },
        {
            id: 2,
            name: 'Test'
        }
    ]
})
</script>
<template>
    <div class="flex justify-between py-2 w-full items-center board-navbar z-50">
        <div class="flex space-x-3">
            <DropdownMenu v-for="drop in boardBar">
                <DropdownMenuTrigger class="ml-4">
                    
                </DropdownMenuTrigger>
            </DropdownMenu>
            <img src="/images/logo_nero.png" alt="bacaro logo" class="h-12">
        </div>
    </div>
</template>