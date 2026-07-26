import http from './http'

export interface ReviewRecord {
  id: number
  experimentId: number
  teacherId: number
  score: number
  comment: string
  createdAt: string
  updatedAt: string
}

export interface ReviewPage {
  records: ReviewRecord[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CreateReviewParams {
  experimentId: number
  score: number
  comment: string
}

export function getReviewList(params?: { page?: number; size?: number; experimentId?: number }) {
  return http.get<any, { code: number; data: ReviewPage }>(
    '/api/reviews',
    { params }
  )
}

export function createReview(data: CreateReviewParams) {
  return http.post<any, { code: number; data: ReviewRecord }>(
    '/api/reviews',
    data
  )
}

export function updateReview(id: number, data: Partial<CreateReviewParams>) {
  return http.put<any, { code: number; data: ReviewRecord }>(
    `/api/reviews/${id}`,
    data
  )
}

export function deleteReview(id: number) {
  return http.delete<any, { code: number }>(
    `/api/reviews/${id}`
  )
}
