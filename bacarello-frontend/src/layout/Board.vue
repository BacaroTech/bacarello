<script lang="ts" setup>

import { onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import BoardNavbar from '@/components/boardComponent/BoardNavbar.vue';
import BoardCard from '@/components/boardComponent/BoardCard.vue';
import { useTrelloList } from '@/stores/mainCardList.ts';
defineOptions({ name: 'BoardLayout' })

const route = useRoute()
const lists = useTrelloList()

function currentBoardId(): string {
    return String(route.params.id)
}

function addColumn(payload: { id: string; name: string }) {
    void lists.addColumn(currentBoardId(), payload)
}

onMounted(() => {
    void lists.getLists(currentBoardId())
})

watch(
    () => route.params.id,
    () => {
        void lists.getLists(currentBoardId())
    },
)
</script>
<template>
    <div class="h-full flex flex-col overflow-hidden">
        <BoardNavbar />
        <p v-if="lists.listError" class="px-4 py-2 text-sm text-destructive">
            {{ lists.listError }}
        </p>
        <p v-if="lists.isLoadingLists" class="px-4 py-2 text-sm text-muted-foreground">
            Loading lists...
        </p>
        <BoardCard :tasks="lists.trelloMock" @add-task="lists.handleAddTask" @add-column="addColumn" />
    </div>

</template>
