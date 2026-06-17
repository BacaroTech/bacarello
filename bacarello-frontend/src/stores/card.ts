import type { Task } from './mainCardList'
import { defineStore } from 'pinia'
import { ref } from 'vue'
export interface CheckList {
  label: string
  lists?: ListItem[]
}
export interface ListItem {
  id?: number
  label: string
  checked?: boolean
}
export interface LabelItem {
  id?: string
  name: string
  color: string
  checked?: boolean
}
export const useCardCheckList = defineStore('checkList', () => {
  const items = ref<CheckList[]>([
    {
      label: 'prova',
      lists: [
        { id: 1, label: 'First task', checked: false },
        { id: 2, label: 'Second task', checked: true },
        { id: 3, label: 'Third task', checked: false },
      ],
    },
  ])
  const colorVal = ref<LabelItem[]>([
    { name: '', color: 'bg-green-500' },
    { name: '', color: 'bg-blue-500' },
    { name: '', color: 'bg-yellow-500' },
    { name: '', color: 'bg-red-500' },
    { name: '', color: 'bg-purple-500' },
    { name: '', color: 'bg-teal-500' },
  ])
  const selectedLabels = ref<LabelItem[]>([])
  function addItem(itemLabel: CheckList): void {
    if (itemLabel && itemLabel.label != '') {
      items.value.push({ label: itemLabel.label, lists: [] })
    }
  }
  function addNewListItem(listItem: ListItem, labelId: CheckList): void {
    if (listItem && labelId) {
      const checklist = items.value.find((list) => list.label === labelId.label)
      if (checklist && checklist != undefined) {
        checklist.lists?.push({
          id: Date.now(),
          label: listItem.label,
          checked: false,
        })
      }
    }
  }
  function editListItem(listItem: ListItem, labelId: CheckList): void {
    if (listItem && labelId) {
      const checklist = items.value.find((list) => list.label === labelId.label)
      if (checklist) {
        const item = checklist.lists?.find((itemList) => itemList.id === listItem.id)
        if (item) {
          item.label = listItem.label
        }
      }
    }
  }
  function createLabel(inputVal: any, labels: LabelItem[]): void {
    if (!inputVal.value.label.trim()) return

    labels.push({
      id: String(Date.now()),
      name: inputVal.value.label,
      color: inputVal.value.color,
    })

    inputVal.value.label = ''
  }

  function addLabel(label: LabelItem, listId: string, indexList?: number) {
    label.checked = !label.checked
    const index = selectedLabels.value.findIndex((l) => l.color === label.color)
    if (index === -1) {
      selectedLabels.value.push({ name: label.name, color: label.color, id: listId })
    } else {
      selectedLabels.value.splice(index, 1)
    }
  }
  function updateLabel(originalColor: string, updated: LabelItem, cardId: string) {
    const selected = selectedLabels.value.find((l) => l.id === cardId && l.color === originalColor)

    if (selected) {
      selected.name = updated.name
      selected.color = updated.color
    }
  }
  return {
    items,
    addItem,
    addNewListItem,
    createLabel,
    colorVal,
    selectedLabels,
    addLabel,
    editListItem,
    updateLabel,
  }
})
