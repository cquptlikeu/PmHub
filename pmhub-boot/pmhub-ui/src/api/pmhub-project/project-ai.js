import request from '@/utils/request'

export function analyzeProjectApi(data) {
  return request({
    url: '/project/ai/analyze',
    method: 'post',
    data
  })
}

export function getProjectAiTaskApi(analysisTaskId) {
  return request({
    url: `/project/ai/tasks/${analysisTaskId}`,
    method: 'get'
  })
}

export function retryProjectAiTaskApi(analysisTaskId) {
  return request({
    url: `/project/ai/tasks/${analysisTaskId}/retry`,
    method: 'post'
  })
}

export function getProjectAiSummaryApi(projectId) {
  return request({
    url: `/project/ai/summary/${projectId}`,
    method: 'get'
  })
}

export function getProjectAiRiskListApi(projectId) {
  return request({
    url: `/project/ai/risks/${projectId}`,
    method: 'get'
  })
}

export function generateProjectWeeklyReportApi(data) {
  return request({
    url: '/project/ai/weekly-report/generate',
    method: 'post',
    data
  })
}

export function getProjectWeeklyReportsApi(projectId) {
  return request({
    url: `/project/ai/weekly-report/${projectId}`,
    method: 'get'
  })
}
