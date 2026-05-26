<script lang="ts" setup>
import draggable from 'vuedraggable'
import { Plus, X } from 'lucide-vue-next';
import { ref, watch } from 'vue';
import type { Task, Column } from '../layout/Board.vue';
import Button from './ui/button/Button.vue';
import { useRouter } from 'vue-router';
const addNewToggle = ref<string | undefined>()
const newTaskTitle = ref<string>('')
const tasksProp = defineProps<{ tasks: Column[] }>()
import { useCardCheckList, type LabelItem } from '@/stores/card';
const { selectedLabels } = useCardCheckList()
const router = useRouter()
const emit = defineEmits<{
    (e: 'add-task', payload: { columnId: string; task: Task }): void,
    (e: 'add-column', payload: { id: string, name: string }): void
}>()
function addTask(columnId: string) {
    if (!newTaskTitle.value.trim()) return

    emit('add-task', {
        columnId,
        task: {
            id: crypto.randomUUID(),
            title: newTaskTitle.value
        }
    })

    newTaskTitle.value = ''
    addNewToggle.value = undefined
}
const addColumnToggle = ref(false)
const newColumnName = ref('')
function addColumn(column: string): void {
    const value = column.trim()

    if (!value) return

    emit('add-column', {
        id: value,
        name: value
    })

    newColumnName.value = ''
    addColumnToggle.value = false
}
function openCardDetail(card: Task) {
    router.push({ name: 'card', params: { cardId: card.id } })
}
function setColor(id: string, label: LabelItem): string {
    if (id === label.id) {
        return label.color
    } else {
        return ""
    }
}
const columnInputToggle = ref<Record<string, boolean>>({})
const editableColumnNames = ref<Record<string, string>>({})

function openColumnEdit(column: Column) {
    editableColumnNames.value[column.id] = column.name
    columnInputToggle.value[column.id] = true
}

function saveColumnName(column: Column) {
    const value = editableColumnNames.value[column.id]?.trim()

    if (value) {
        column.name = value
    }

    columnInputToggle.value[column.id] = false
}

</script>
<template>
    <ol class="flex gap-6 mt-3 overflow-x-auto overflow-y-hidden">
        <li v-for="column in tasksProp.tasks" :key="column.id" class="w-64 bg-muted p-4 rounded-xl shrink-0 h-auto">
            <div class="mb-3">
                <h3 v-if="!columnInputToggle[column.id]"
                    class="font-semibold px-2 py-1 rounded cursor-pointer hover:bg-accent"
                    @click="openColumnEdit(column)">
                    {{ column.name }}
                </h3>
                <textarea v-else v-model="editableColumnNames[column.id]"
                    class="w-full resize-none rounded p-2 outline-none border bg-background" rows="1" autofocus
                    @blur="saveColumnName(column)" @keydown.enter.prevent="saveColumnName(column)" />
            </div>
            <draggable v-model="column.tasks" group="tasks" item-key="id" class="space-y-2 cursor-pointer">
                <template #item="{ element }">
                    <div class="bg-background p-3 rounded-lg shadow-sm" @click="openCardDetail(element)">
                        <div class="flex">
                            <div :class="setColor(element.id, label)" class="w-4 h-4 rounded-xl"
                                v-for="label in selectedLabels" v-show="label.id == element.id"></div>
                        </div>
                        <p class="font-medium">{{ element.title }}</p>
                        <p v-if="element.description" class="text-sm text-muted-foreground">
                            {{ element.description }}
                        </p>
                    </div>
                </template>
            </draggable>
            <div v-show="addNewToggle != column.id"
                class="mt-2 flex space-x-2 hoover-section p-2 rounded-lg cursor-pointer"
                @click="addNewToggle = column.id">
                <Plus />
                <label for="addNewCard">Add a card</label>
            </div>
            <div class="mt-2" v-show="addNewToggle === column.id">
                <div class="bg-background p-3 rounded-lg shadow-sm">
                    <input v-model="newTaskTitle" type="text" name="add-task" :id="column.id"
                        placeholder="Enter a title or paste a link">
                </div>
                <div class="flex items-center">
                    <Button title="Add New" @click="addTask(column.id)"> Add New</Button>
                    <X @click="addNewToggle = ''" />
                </div>
            </div>
        </li>
        <li class="w-64 shrink-0">
            <div v-if="!addColumnToggle"
                class="flex items-center gap-2 bg-muted p-3 rounded-xl cursor-pointer hover:bg-accent"
                @click="addColumnToggle = true">
                <Plus class="w-4 h-4" />
                <span>Add another list</span>
            </div>

            <div v-else class="w-64 bg-muted p-4 rounded-xl">
                <input v-model="newColumnName" type="text" placeholder="Enter list title..."
                    class="w-full rounded-md border bg-background p-2 outline-none" autofocus
                    @keydown.enter.prevent="addColumn(newColumnName)" />

                <div class="flex items-center gap-2 mt-3">
                    <Button @click="addColumn(newColumnName)">
                        Add list
                    </Button>

                    <X class="cursor-pointer" @click="addColumnToggle = false" />
                </div>
            </div>
        </li>
    </ol>
</template>