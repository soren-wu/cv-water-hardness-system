import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { StudentProfile } from '../types/dashboard'

export const useStudentStore = defineStore('student', () => {
  const profile = ref<StudentProfile>({
    name: '张同学',
    studentNumber: '2024010156',
    role: 'student',
  })

  const token = ref<string | null>(null)

  function setToken(nextToken: string) {
    token.value = nextToken
  }

  function clearSession() {
    token.value = null
  }

  return {
    profile,
    token,
    setToken,
    clearSession,
  }
})
