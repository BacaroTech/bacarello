<script setup lang="ts">
import { computed, ref } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { useMediaQuery } from '@vueuse/core'
import { Button } from '@/components/ui/button'
import type { BoardView } from '@/stores/board'
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog'
import {
    Drawer,
    DrawerClose,
    DrawerContent,
    DrawerDescription,
    DrawerFooter,
    DrawerHeader,
    DrawerTitle,
    DrawerTrigger,
} from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import {
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage
} from '@/components/ui/form'
import { useBoardDataStore } from '@/stores/board'
const boardStore = useBoardDataStore()
const formSchema = toTypedSchema(
    z.object({
        title: z
            .string()
            .min(5, 'Board title must be at least 5 characters.')
            .max(20, 'Board title must be at most 32 characters.'),
        label: z
            .string()
            .min(2, 'Label must be at least 2 characters.')
            .max(20, 'Label must be at most 20 characters.'),
    }),
)
const { handleSubmit, resetForm } = useForm<BoardView>({
    validationSchema: formSchema,
    initialValues: {
        title: '',
        label: '',
    },
})
const isSubmitting = ref(false)
const onSubmit = handleSubmit(async (data: BoardView) => {
    isSubmitting.value = true

    try {
        await boardStore.createBoard(data)
        resetForm()
        open.value = false
    } finally {
        isSubmitting.value = false
    }
})
const isDesktop = useMediaQuery('(min-width: 640px)')

const Modal = computed(() => ({
    Root: isDesktop.value ? Dialog : Drawer,
    Trigger: isDesktop.value ? DialogTrigger : DrawerTrigger,
    Content: isDesktop.value ? DialogContent : DrawerContent,
    Header: isDesktop.value ? DialogHeader : DrawerHeader,
    Title: isDesktop.value ? DialogTitle : DrawerTitle,
    Description: isDesktop.value ? DialogDescription : DrawerDescription,
    Footer: isDesktop.value ? DialogFooter : DrawerFooter,
    Close: isDesktop.value ? DialogClose : DrawerClose,
}))

const open = ref(false)

</script>

<template>
    <component :is="Modal.Root" v-model:open="open">
        <component :is="Modal.Trigger" as-child>
            <Button variant="outline" class="cursor-pointer">
                Create Board
            </Button>
        </component>
        <component :is="Modal.Content" class="sm:max-w-md" :class="[
            { 'px-2 pb-8 *:px-4': !isDesktop },
        ]">
            <component :is="Modal.Header">
                <component :is="Modal.Title">
                    Create Board
                </component>
                <component :is="Modal.Description">
                    Create a new Board
                </component>
            </component>

            <div class="flex items-center gap-2">
                <div class="grid flex-1 gap-2">
                    <form id="form-vee-demo" @submit="onSubmit" class="space-y-6">
                        <p v-if="boardStore.boardError" class="text-sm text-destructive">
                            {{ boardStore.boardError }}
                        </p>
                        <FormField v-slot="{ componentField }" name="title">
                            <FormItem>
                                <FormLabel>Board Title</FormLabel>
                                <FormControl>
                                    <Input v-bind="componentField" placeholder="Title" autocomplete="off" />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        </FormField>

                        <FormField v-slot="{ componentField }" name="label">
                            <FormItem>
                                <FormLabel>Board Label</FormLabel>
                                <FormControl>
                                    <Input v-bind="componentField" placeholder="Label"
                                        autocomplete="off" />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        </FormField>
                        <Button type="submit" form="form-vee-demo" :disabled="isSubmitting">
                            {{ isSubmitting ? 'Creating...' : 'Submit' }}
                        </Button>
                    </form>
                </div>
            </div>
        </component>
    </component>
</template>
