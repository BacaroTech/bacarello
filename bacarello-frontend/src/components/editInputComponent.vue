<script setup lang="ts">
  import { ref, watch } from 'vue'
  
  const props = defineProps({
    modelValue: {
      type: String,
      required: true,
    },
  })
  
  const emit = defineEmits([
    'update:modelValue',
    'save',
  ])
  
  const isEditing = ref(false)
  const localValue = ref(props.modelValue)
  
  watch(
    () => props.modelValue,
    (value) => {
      localValue.value = value
    }
  )
  
  function startEditing() {
    isEditing.value = true
  }
  
  function save() {
    isEditing.value = false
    console.log(localValue.value);
    
    emit('update:modelValue', localValue.value)
    emit('save', localValue.value)
  }
  </script>
<!-- EditableColumnName.vue -->
<template>
    <div class="mb-3">
      <h3
        v-if="!isEditing"
        class="font-semibold px-2 py-1 rounded cursor-pointer hover:bg-accent"
        @click="startEditing"
      >
        {{ modelValue }}
      </h3>
  
      <textarea
        v-else
        v-model="localValue"
        class="w-full resize-none rounded p-2 outline-none border bg-background"
        rows="1"
        autofocus
        @blur="save"
        @keydown.enter.prevent="save"
      />
    </div>
  </template>
  
  