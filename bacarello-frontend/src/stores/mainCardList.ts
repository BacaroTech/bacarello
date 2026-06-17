import { ref, computed, type Component } from 'vue'
import { defineStore } from 'pinia'
export interface Task {
  id: string
  title: string
  description?: string
  labels?: string[]
  endTask?: string
  completed?: boolean
}

export interface Column {
  id: string
  name: string
  tasks: Task[]
}

export const useTrelloList = defineStore('trelloList', () => {
  const trelloMock = ref<Column[]>([
    {
      id: 'todo',
      name: 'To Do',
      tasks: [
        {
          id: 'task-1',
          title: 'Design login page',
          description: 'Create responsive UI for login',
          labels: ['design', 'ui'],
          endTask: '2026-02-20',
        },
        {
          id: 'task-2',
          title: 'Setup Pinia store',
          labels: ['backend'],
        },
      ],
    },
    {
      id: 'in-progress',
      name: 'In Progress',
      tasks: [
        {
          id: 'task-3',
          title: 'Implement drag & drop',
          labels: ['feature'],
          endTask: '2026-02-18',
        },
      ],
    },
    {
      id: 'done',
      name: 'Done',
      tasks: [
        {
          id: 'task-4',
          title: 'Project scaffolding',
          labels: ['setup'],
        },
      ],
    },
  ])
  function handleAddTask({ columnId, task }: { columnId: string; task: Task }) {
    const column = trelloMock.value.find((c) => c.id === columnId)
    if (!column) return
    column.tasks.push(task)
  }
  function addColumn({ id, name }: { id: string; name: string }) {
    trelloMock.value.push({ id: id, name: name, tasks: [] })
  }
  return {trelloMock, handleAddTask, addColumn}
})
