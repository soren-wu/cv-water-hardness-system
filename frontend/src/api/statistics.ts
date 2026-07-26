import http from './http'

export interface OverviewStats {
  totalStudents: number
  totalTasks: number
  totalExperiments: number
  submittedCount: number
  reviewedCount: number
  pendingReviewCount: number
  in_progressCount: number
  near_endpointCount: number
  endpointCount: number
  abnormalCount: number
  averageScore: number
}

export function getOverview() {
  return http.get<any, { code: number; data: OverviewStats }>(
    '/api/statistics/overview'
  )
}
