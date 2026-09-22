<script lang="ts" setup>
import Navbar from '@/layout/Navbar.vue';
import { useRouter } from 'vue-router';
import { onMounted, watch } from 'vue';
import Separator from '@/components/ui/separator/Separator.vue';
import CreateBoardCard from '@/components/boardComponent/CreateBoardCard.vue';
import Button from '@/components/ui/button/Button.vue';
import { useBoardDataStore } from '@/stores/board';
import { useUserAuth } from '@/stores/userAuth';
import { useWorkspaceStore } from '@/stores/workspace';
const homeUserBoards = useBoardDataStore()
const workspaceStore = useWorkspaceStore()
const auth = useUserAuth()
const router = useRouter()
function getBoardId(id: string | undefined): void {
    if (!id) return

    router.push({
        name: 'board',
        params: { id: id }
    })
}

async function createWorkspace(): Promise<void> {
    try {
        await workspaceStore.createWorkspace()
    } catch {
        // Error already stored for UI display.
    }
}

onMounted(() => {
    if (auth.isAuthenticated) {
        homeUserBoards.getBoards()
    }
})

watch(
    () => auth.isAuthenticated,
    (isAuthenticated) => {
        if (isAuthenticated) {
            homeUserBoards.getBoards()
        }
    },
)

</script>
<template>
    <div class="flex flex-col min-h-screen">
        <header>
            <Navbar />
        </header>

        <main class="flex-1 items-center p-10 bg-muted/40">
            <div >
                <h1 class="scroll-m-20 text-center text-4xl font-extrabold tracking-tight text-balance"> Your Boards</h1>
                <Separator class="my-4" />
                <p v-if="homeUserBoards.boardError" class="mb-4 text-center text-sm text-destructive">
                    {{ homeUserBoards.boardError }}
                </p>
                <p v-if="workspaceStore.workspaceError" class="mb-4 text-center text-sm text-destructive">
                    {{ workspaceStore.workspaceError }}
                </p>
                <p v-if="homeUserBoards.isLoadingBoards" class="mb-4 text-center text-sm text-muted-foreground">
                    Loading boards...
                </p>
                <div class="grid gap-6 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
                    <div v-for="board in homeUserBoards.homeUserBoards" :key="board.id" @click="getBoardId(board.id)" class="cursor-pointer rounded-xl p-6 h-32
                       bg-linear-to-br from-indigo-500 to-purple-600
                       text-white shadow-md
                       transition-transform duration-200
                       hover:scale-105 hover:shadow-xl">
                       <h1 class="text-lg font-semibold font-mono">{{ board.title }}</h1>
                        <h3 class="font-mono">
                            {{ board.label }}
                        </h3>
                    </div>
                </div>
                <div class="flex flex-wrap justify-center gap-3 mt-16">
                    <CreateBoardCard />
                    <Button
                        variant="outline"
                        :disabled="workspaceStore.isCreatingWorkspace"
                        @click="createWorkspace"
                    >
                        {{ workspaceStore.isCreatingWorkspace ? 'Creating workspace...' : 'Create Workspace' }}
                    </Button>
                </div>
            </div>
        </main>
    </div>
</template>
