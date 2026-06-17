<script lang="ts" setup>

import { ref } from 'vue';
import BoardNavbar from '../components/BoardNavbar.vue';
import BoardCard from '../components/BoardCard.vue';
import { useTrelloList } from '@/stores/mainCardList.ts';
const { trelloMock, handleAddTask, addColumn } = useTrelloList()
</script>
<template>
    <div class="h-full flex flex-col overflow-hidden">
        <BoardNavbar />
        <BoardCard :tasks="trelloMock" @add-task="handleAddTask" @add-column="addColumn" />
    </div>

</template>