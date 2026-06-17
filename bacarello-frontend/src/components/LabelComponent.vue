<script lang="ts" setup>
import { Checkbox } from './ui/checkbox'
import { Button } from './ui/button';
import { SquarePen, Check } from 'lucide-vue-next';
const props = defineProps(['open', 'colorVal', 'list'])
import { useCardCheckList, type LabelItem } from '@/stores/card';
const { addLabel, updateLabel } = useCardCheckList()
import { Label } from './ui/label';
import { computed, ref } from 'vue';
import Input from './ui/input/Input.vue';
function addNewLabel(color: LabelItem, taskId: string) {
    if (taskId) {
        addLabel(color, taskId)
    }
}

const colors = ref<LabelItem[]>([
    { name: '', color: 'bg-green-500', checked: false },
    { name: '', color: 'bg-blue-500', checked: false },
    { name: '', color: 'bg-red-500', checked: false },
    { name: '', color: 'bg-stone-500', checked: false },
    { name: '', color: 'bg-violet-500', checked: false },
    { name: '', color: 'bg-green-600', checked: false },
    { name: '', color: 'bg-blue-600', checked: false },
    { name: '', color: 'bg-red-600', checked: false },
    { name: '', color: 'bg-stone-600', checked: false },
    { name: '', color: 'bg-violet-600', checked: false },
    { name: '', color: 'bg-green-700', checked: false },
    { name: '', color: 'bg-blue-700', checked: false },
    { name: '', color: 'bg-red-700', checked: false },
    { name: '', color: 'bg-stone-700', checked: false },
    { name: '', color: 'bg-violet-700', checked: false }
])
const selectedColor = ref<LabelItem>({ color: '', name: '' })
const editingLabel = ref<LabelItem | null>(null)
function toggleColor(label: LabelItem) {
    colors.value.forEach(color => {
        color.checked = false
    })

    label.checked = true

    selectedColor.value.color = label.color
}
const originalColor = ref('')
function editLabel(label: LabelItem, open: any) {
    editingLabel.value = label
    originalColor.value = label.color

    selectedColor.value = { ...label }

    colors.value.forEach(color => {
        color.checked = color.color === label.color
    })

    open.editLabel = true
}
function save(taskId: string) {
    if (!editingLabel.value || !originalColor.value) return
    editingLabel.value.name = selectedColor.value.name
    editingLabel.value.color = selectedColor.value.color

    updateLabel(
        originalColor.value,
        selectedColor.value,
        taskId
    )

    props.open.editLabel = false
}

</script>
<template>
    <div>
        <!-- Master-->
        <div v-show="!open.editLabel">
            <div class="flex flex-col mt-10 gap-4">
                <Label class="justify-center"> Labels</Label>
                <div v-for="color in colorVal" class="flex gap-2">
                    <div class="hover:opacity-100 flex w-full gap-2"
                        :class="{ 'opacity-50': !color.checked, 'opacity-100': color.checked }">
                        <Checkbox class="m-1" v-model="color.checked" @click="addNewLabel(color, list)" />
                        <span :class="color.color" @click="addLabel(color, list)"
                            class="w-full border h-auto rounded-sm cursor-pointer"></span>
                    </div>
                    <div class="hover:bg-gray-400 rounded-xs cursor-pointer">
                        <SquarePen class="h-4 mt-1 " @click="editLabel(color, open)" />
                    </div>
                </div>
            </div>
        </div>
        <!-- CHild-->
        <div v-show="open.editLabel" class="space-y-10">
            <div class="bg-black w-full p-4 rounded-md flex justify-center">
                <div class="px-3 py-1 rounded-sm text-white text-sm font-medium w-full" :class="selectedColor.color">
                    <span>{{ selectedColor.name }}</span>
                </div>
            </div>
            <div class="flex flex-col gap-1">
                <Label>Title</Label>
                <Input v-model="selectedColor.name" />
            </div>
            <div class="flex flex-col gap-1">
                <div class="grid grid-cols-5 space-y-5">
                    <div v-for="value in colors" :class="value.color"
                        class="w-12 h-8 rounded-md cursor-pointer flex justify-center items-center"
                        @click="toggleColor(value)">
                        <Check v-show="value.checked" />
                    </div>
                </div>
            </div>
            <Button variant="outline" class="font-normal items-center justify-center" @click="save(list)">
                Save
            </Button>
        </div>
    </div>
</template>