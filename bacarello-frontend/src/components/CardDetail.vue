<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card'
import { X, SquarePlus } from 'lucide-vue-next'
import { Separator } from './ui/separator'
import type { DateValue } from '@internationalized/date'
import { ChevronDownIcon, TextAlignStart, Users, ListPlus, Timer, Bookmark, Search, ChevronLeft } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Input } from '@/components/ui/input'
import CheckList from './CheckList.vue'
import { InputGroup, InputGroupAddon, InputGroupInput, } from '@/components/ui/input-group'
import { useCardCheckList, type CheckList as CheckModel } from '@/stores/card';
import LabelComponent from './LabelComponent.vue'
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/components/ui/popover'
import { computed, ref, watch } from 'vue'
import type { Ref } from 'vue'
import Label from './ui/label/Label.vue'
const { addItem, colorVal, addLabel, selectedLabels } = useCardCheckList()
const router = useRouter()
const route = useRoute()
import { useTrelloList } from '@/stores/mainCardList.ts'
const { trelloMock } = useTrelloList()
import editInputComponent from './editInputComponent.vue'
function close() {
    router.push({ name: 'board', params: { id: route.params.id } })
}
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip'

const date = ref() as Ref<DateValue>
const open = ref<Record<string, boolean>>(
    {
        date: false,
        checkList: false,
        labels: false,
        editLabel: false,
        members: false,
    }
)
function addItemCheckList(item: CheckModel): void {
    addItem(item)
    open.value.checkList = false
}
const inputVal = ref<CheckModel>({ label: '' })
const cardDetail = computed(() => { trelloMock.find(e => e.tasks.find(d => d.id === route.params.cardId)) })
</script>

<template>
    <!-- Overlay -->
    <div class="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
        <!-- Modal Container -->
        <Card class="relative w-full max-w-3xl shadow-2xl h-full z-999">
            <CardHeader>
                <X @click="close" class="absolute right-4 top-4 cursor-pointer" />
                <CardTitle>To Do</CardTitle>
            </CardHeader>
            <Separator />
            <CardContent>
                <div class="flex flex-col">
                    <editInputComponent :model-value="String(route.params.cardId ?? '')" />
                    <div class="mt-5 space-x-2">
                        <Popover v-model:open="open.date">
                            <PopoverTrigger as-child>
                                <Button id="date-picker" variant="outline"
                                    class="justify-between font-normal items-center">
                                    <Timer />
                                    Dates
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent class="w-auto overflow-hidden p-0" align="start">
                                <Calendar v-model="date" locale="it-IT" @update:model-value="(value) => {
                                    if (value) {
                                        date = value
                                        open.date = false
                                    }
                                }" />
                            </PopoverContent>
                        </Popover>
                        <Popover :open="open.checkList" @update:open="(val) => open.checkList = val">
                            <PopoverTrigger as-child>
                                <Button variant="outline" class="justify-between font-normal items-center">
                                    <ListPlus />
                                    ChekList
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent class="w-auto overflow-hidden p-0" align="start">
                                <CardHeader class="mt-2 flex items-center justify-between">
                                    <CardTitle>Add Checklist</CardTitle>
                                    <div @click="open.checkList = false"
                                        class="hover:bg-accent p-2 rounded-lg cursor-pointer">
                                        <X class="size-4" />
                                    </div>
                                </CardHeader>
                                <CardContent>
                                    <div class="flex flex-col mt-10 gap-4">
                                        <Label> Title</Label>
                                        <Input v-model="inputVal.label" />
                                    </div>
                                </CardContent>
                                <CardFooter class="mt-2 mb-2 space-x-2">
                                    <Button @click="addItemCheckList(inputVal)">Add</Button>
                                </CardFooter>
                            </PopoverContent>
                        </Popover>
                        <Popover v-model:open="open.labels" v-if="selectedLabels.length === 0">
                            <PopoverTrigger as-child>
                                <Button variant="outline" class="justify-between font-normal items-center">
                                    <Bookmark />
                                    Labels
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent class="overflow-hidden p-0" align="start"
                                v-show="open.labels && !open.editLabel">
                                <CardHeader class="mt-2 flex items-center justify-between">
                                    <div v-show="open.editLabel" class="flex justify-between items-center p-2">
                                        <ChevronLeft @click="open.editLabel = false"
                                            class="cursor-pointer hover:bg-accent" />
                                        <CardTitle>Edit Label</CardTitle>
                                    </div>
                                    <CardTitle v-show="!open.editLabel">Add Label</CardTitle>
                                    <div @click="open.labels = false"
                                        class="hover:bg-accent p-2 rounded-lg cursor-pointer">
                                        <X class="size-4" />
                                    </div>
                                </CardHeader>
                                <CardContent>
                                    <LabelComponent :open="open" :color-val="colorVal" :list="$route.params.cardId" />
                                </CardContent>
                                <CardFooter class="mt-4 mb-2 space-x-2 justify-center">
                                    <Button v-show="!open.editLabel" variant="outline" class="font-normal items-center"
                                        @click="open.editLabel = true">
                                        Create Label
                                    </Button>
                                </CardFooter>
                            </PopoverContent>
                        </Popover>
                        <Popover v-model:open="open.members">
                            <PopoverTrigger as-child>
                                <Button variant="outline" class="justify-between font-normal items-center">
                                    <Users />
                                    Members
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent class="w-auto overflow-hidden p-0 space-y-4" align="start">
                                <CardHeader class="mt-2 flex items-center justify-between">
                                    <CardTitle>Add Checklist</CardTitle>
                                    <div @click="open.members = false"
                                        class="hover:bg-accent p-2 rounded-lg cursor-pointer">
                                        <X class="size-4" />
                                    </div>
                                </CardHeader>
                                <CardContent>
                                    <InputGroup>
                                        <InputGroupInput placeholder="Search..." />
                                        <InputGroupAddon>
                                            <Search />
                                        </InputGroupAddon>
                                    </InputGroup>
                                </CardContent>
                                <CardFooter class="mt-2 mb-2 space-x-2">
                                    <Label>Board Members</Label>
                                </CardFooter>
                            </PopoverContent>
                        </Popover>
                    </div>
                </div>
                <div class="space-y-4">
                    <div class="inline-flex flex-row items-center max-w-sm mt-10 gap-3">
                        <div class="flex flex-col gap-1 items-start" v-show="selectedLabels.length > 0">
                            <label>Labels</label>
                            <div class="flex flex-row items-center gap-0.5">
                                <TooltipProvider>
                                    <Tooltip v-for="value in selectedLabels" >
                                        <TooltipTrigger>
                                            <div class="border h-8 w-12 rounded-sm" :class="value.color"
                                                v-show="value.id === $route.params.cardId">
                                            </div>

                                        </TooltipTrigger>
                                        <TooltipContent>
                                            {{ `Color: ${value.color}, Name: ${value.name}`}}
                                        </TooltipContent>
                                    </Tooltip>
                                </TooltipProvider>

                                <Popover v-if="selectedLabels.length >= 1" v-model:open="open.labels">
                                    <PopoverTrigger as-child>
                                        <SquarePlus />
                                    </PopoverTrigger>
                                    <PopoverContent class="overflow-hidden p-0" align="start">
                                        <CardHeader class="mt-2 flex items-center justify-between">
                                            <div v-show="open.editLabel" class="flex justify-between items-center p-2">
                                                <ChevronLeft @click="open.editLabel = false"
                                                    class="cursor-pointer hover:bg-accent" />
                                                <CardTitle>Edit Label</CardTitle>
                                            </div>
                                            <CardTitle v-show="!open.editLabel">Add Label</CardTitle>
                                            <div @click="open.labels = false"
                                                class="hover:bg-accent p-2 rounded-lg cursor-pointer">
                                                <X class="size-4" />
                                            </div>
                                        </CardHeader>
                                        <CardContent>
                                            <LabelComponent :open="open" :color-val="colorVal"
                                                :list="$route.params.cardId" />
                                        </CardContent>
                                        <CardFooter class="mt-4 mb-2 space-x-2 justify-center">
                                            <Button v-show="!open.editLabel" variant="outline"
                                                class="font-normal items-center" @click="open.editLabel = true">
                                                Create Label
                                            </Button>
                                        </CardFooter>
                                    </PopoverContent>
                                </Popover>
                            </div>
                        </div>
                        <div class="inline-flex flex-col" v-show="date">
                            <label>Due Date</label>
                            <div class="inline-flex cursor-pointer flex-row input-bg justify-between gap-5 rounded-sm px-2 py-1"
                                @click="open.date = !open.date">
                                <div class="text-sm">{{ date }}</div>
                                <ChevronDownIcon />
                            </div>
                        </div>
                    </div>
                    <div class="flex mt-10 flex-col gap-2">
                        <div class="grid grid-cols-[auto_1fr] items-center gap-2">
                            <TextAlignStart />
                            <div class="flex justify-between">
                                <Label>Description</Label>
                                <Button> Edit</Button>
                            </div>
                        </div>
                        <textarea name="text"></textarea>
                    </div>
                    <div class="overflow-y-auto max-h-[70%]">
                        <CheckList />
                    </div>
                </div>
            </CardContent>
            <CardFooter>

            </CardFooter>
        </Card>
    </div>
</template>