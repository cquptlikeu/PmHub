<template>
  <div v-loading="loading" class="project-ai app-container">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button
            v-if="canOperateAnalyze"
            type="primary"
            size="mini"
            :loading="analyzeSubmitting"
            @click="handleAnalyze"
          >
            开始分析
          </el-button>
          <el-button v-if="hasAnyReadPermission" size="mini" @click="reloadAll">刷新</el-button>
          <el-tag
            v-if="canQuerySummary && summary.analysisStatus"
            :type="resolveTaskStatusType(summary.analysisStatus)"
            size="mini"
          >
            {{ formatTaskStatus(summary.analysisStatus) }}
          </el-tag>
          <el-button
            v-if="canRetryAnalysis"
            type="warning"
            size="mini"
            :loading="retrySubmitting"
            @click="handleRetryAnalyze"
          >
            重试
          </el-button>
        </div>
        <div v-if="canGenerateWeeklyReport || canQueryWeeklyReports" class="toolbar-right">
          <el-date-picker
            v-if="canGenerateWeeklyReport"
            v-model="weeklyRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="周开始"
            end-placeholder="周结束"
            size="mini"
            unlink-panels
          />
          <el-checkbox v-if="canGenerateWeeklyReport" v-model="forceRegenerate">强制重生成</el-checkbox>
          <el-button
            v-if="canGenerateWeeklyReport"
            type="success"
            size="mini"
            :loading="weeklySubmitting"
            @click="handleGenerateWeeklyReport"
          >
            生成周报
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card v-if="showSummaryCard" shadow="never" header="AI 摘要" class="summary-card">
          <div class="metric-list">
            <div class="metric-item">
              <span class="metric-label">健康分</span>
              <span class="metric-value">{{ displayValue(summary.healthScore, '暂无') }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">健康等级</span>
              <el-tag :type="resolveHealthLevelType(summary.healthLevel)" size="mini">
                {{ formatHealthLevel(summary.healthLevel) }}
              </el-tag>
            </div>
            <div class="metric-item">
              <span class="metric-label">风险总数</span>
              <span class="metric-value">{{ displayValue(summary.riskCount, 0) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">高风险</span>
              <span class="metric-value danger">{{ displayValue(summary.highRiskCount, 0) }}</span>
            </div>
          </div>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="最近分析时间">
              {{ displayValue(summary.latestAnalyzeTime, '暂无') }}
            </el-descriptions-item>
            <el-descriptions-item label="分析任务ID">
              {{ displayValue(summary.analysisTaskId, '暂无') }}
            </el-descriptions-item>
            <el-descriptions-item label="流程状态" :span="2">
              {{ displayValue(summary.workflowSummary, '当前无审批卡点') }}
            </el-descriptions-item>
            <el-descriptions-item label="扣分明细" :span="2">
              {{ displayValue(summary.deductionDetail, '暂无') }}
            </el-descriptions-item>
          </el-descriptions>

          <el-alert
            v-if="summary.analysisStatus === 'FAILED' && summary.errorMessage"
            class="task-error-alert"
            type="error"
            :title="summary.errorMessage"
            show-icon
            :closable="false"
          />

          <div class="narrative-block">
            <div class="block-title">分析结论</div>
            <div class="block-content">
              {{ displayValue(summary.aiSummary, '暂无分析结果，建议先执行项目分析。') }}
            </div>
          </div>
        </el-card>

        <el-card v-if="showRiskCard" shadow="never" header="当前风险" class="risk-card">
          <el-empty v-if="!riskList.length" description="当前暂无风险记录" :image-size="96" />
          <el-table v-else :data="riskList" size="small">
            <el-table-column label="级别" width="88" align="center">
              <template slot-scope="scope">
                <el-tag :type="resolveRiskLevelType(scope.row.riskLevel)" size="mini">
                  {{ formatRiskLevel(scope.row.riskLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="类型" prop="riskType" width="140" :formatter="formatRiskTypeCell" />
            <el-table-column label="标题" prop="title" min-width="140" show-overflow-tooltip />
            <el-table-column label="原因" prop="reason" min-width="180" show-overflow-tooltip />
            <el-table-column label="建议" prop="suggestion" min-width="180" show-overflow-tooltip />
            <el-table-column label="识别时间" prop="createdTime" width="168" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card v-if="showWeeklyReportPanel" shadow="never" header="周报记录" class="report-list-card">
          <el-empty v-if="!weeklyReports.length" description="暂无周报记录" :image-size="88" />
          <div v-else class="report-list">
            <div
              v-for="report in weeklyReports"
              :key="report.id"
              class="report-item"
              :class="{ active: report.id === selectedReportId }"
              @click="selectedReportId = report.id"
            >
              <div class="report-item-header">
                <span>v{{ report.version || 1 }}</span>
                <el-tag size="mini" :type="resolveWeeklyStatusType(report.status)">
                  {{ formatWeeklyStatus(report.status) }}
                </el-tag>
              </div>
              <div class="report-item-range">{{ report.weekStart }} 至 {{ report.weekEnd }}</div>
              <div class="report-item-time">{{ report.createdTime }}</div>
            </div>
          </div>
        </el-card>

        <el-card v-if="showWeeklyReportPanel" shadow="never" header="周报内容" class="report-content-card">
          <el-empty v-if="!selectedReport" description="请选择周报记录" :image-size="88" />
          <template v-else>
            <div class="report-meta">
              <span>版本 {{ selectedReport.version || 1 }}</span>
              <span>{{ selectedReport.weekStart }} 至 {{ selectedReport.weekEnd }}</span>
            </div>
            <div class="report-content">{{ selectedReport.content }}</div>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {
  analyzeProjectApi,
  generateProjectWeeklyReportApi,
  getProjectAiRiskListApi,
  getProjectAiSummaryApi,
  getProjectAiTaskApi,
  getProjectWeeklyReportsApi,
  retryProjectAiTaskApi
} from '@/api/pmhub-project/project-ai'
import auth from '@/plugins/auth'

function createEmptySummary(projectId) {
  return {
    projectId,
    analysisTaskId: '',
    analysisStatus: '',
    errorMessage: '',
    healthScore: null,
    healthLevel: '',
    riskCount: 0,
    highRiskCount: 0,
    deductionDetail: '',
    aiSummary: '',
    workflowSummary: '',
    latestAnalyzeTime: ''
  }
}

function getDefaultWeekRange() {
  const today = new Date()
  const day = today.getDay() || 7
  const monday = new Date(today)
  monday.setHours(0, 0, 0, 0)
  monday.setDate(today.getDate() - day + 1)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return [formatDate(monday), formatDate(sunday)]
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function extractAjaxPayload(response) {
  if (!response || typeof response !== 'object') {
    return response
  }
  if (response.data !== undefined && response.data !== null && response.data !== '') {
    return response.data
  }
  return response.msg
}

export default {
  name: 'MyProjectInfoAi',
  props: {
    projectData: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      loading: false,
      analyzeSubmitting: false,
      retrySubmitting: false,
      weeklySubmitting: false,
      summary: createEmptySummary(''),
      riskList: [],
      weeklyReports: [],
      selectedReportId: '',
      weeklyRange: getDefaultWeekRange(),
      forceRegenerate: false,
      pollingTimer: null
    }
  },
  computed: {
    projectId() {
      return this.projectData && this.projectData.projectId ? this.projectData.projectId : ''
    },
    canQuerySummary() {
      return auth.hasPermi('project:ai:summary')
    },
    canQueryRisks() {
      return auth.hasPermi('project:ai:risks')
    },
    canQueryTask() {
      return auth.hasPermi('project:ai:task')
    },
    canOperateAnalyze() {
      return auth.hasPermiAnd(['project:ai:analyze', 'project:ai:task'])
    },
    canRetryAnalysis() {
      return auth.hasPermi('project:ai:analyze') && this.summary.analysisStatus === 'FAILED' && !!this.summary.analysisTaskId
    },
    canGenerateWeeklyReport() {
      return auth.hasPermi('project:ai:weeklyReportGenerate')
    },
    canQueryWeeklyReports() {
      return auth.hasPermi('project:ai:weeklyReportList')
    },
    hasAnyReadPermission() {
      return this.canQuerySummary || this.canQueryRisks || this.canQueryWeeklyReports
    },
    showSummaryCard() {
      return this.canQuerySummary
    },
    showRiskCard() {
      return this.canQueryRisks
    },
    showWeeklyReportPanel() {
      return this.canQueryWeeklyReports
    },
    selectedReport() {
      return this.weeklyReports.find((item) => item.id === this.selectedReportId) || null
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(value) {
        if (!value) {
          return
        }
        this.summary = createEmptySummary(value)
        this.reloadAll()
      }
    }
  },
  beforeDestroy() {
    this.stopPolling()
  },
  methods: {
    reloadAll() {
      if (!this.projectId || !this.hasAnyReadPermission) {
        return
      }
      this.loading = true
      Promise.all([this.loadSummary(), this.loadRisks(), this.loadWeeklyReports()]).finally(() => {
        this.loading = false
      })
    },
    loadSummary() {
      if (!this.canQuerySummary) {
        this.stopPolling()
        this.summary = createEmptySummary(this.projectId)
        return Promise.resolve()
      }
      return getProjectAiSummaryApi(this.projectId)
        .then((res) => {
          const data = res.data || createEmptySummary(this.projectId)
          this.summary = Object.assign(createEmptySummary(this.projectId), data)
          if (this.isTaskRunning(this.summary.analysisStatus) && this.summary.analysisTaskId) {
            this.startPolling(this.summary.analysisTaskId)
          } else {
            this.stopPolling()
          }
        })
        .catch(() => {
          this.stopPolling()
          this.summary = createEmptySummary(this.projectId)
        })
    },
    loadRisks() {
      if (!this.canQueryRisks) {
        this.riskList = []
        return Promise.resolve()
      }
      return getProjectAiRiskListApi(this.projectId)
        .then((res) => {
          this.riskList = Array.isArray(res.data) ? res.data : []
        })
        .catch(() => {
          this.riskList = []
        })
    },
    loadWeeklyReports(preferredReportId) {
      if (!this.canQueryWeeklyReports) {
        this.weeklyReports = []
        this.selectedReportId = ''
        return Promise.resolve()
      }
      return getProjectWeeklyReportsApi(this.projectId)
        .then((res) => {
          this.weeklyReports = Array.isArray(res.data) ? res.data : []
          const currentExists = this.weeklyReports.some((item) => item.id === this.selectedReportId)
          if (preferredReportId && this.weeklyReports.some((item) => item.id === preferredReportId)) {
            this.selectedReportId = preferredReportId
          } else if (!currentExists) {
            this.selectedReportId = this.weeklyReports.length ? this.weeklyReports[0].id : ''
          }
        })
        .catch(() => {
          this.weeklyReports = []
          this.selectedReportId = ''
        })
    },
    handleAnalyze() {
      if (!this.projectId || !this.canOperateAnalyze) {
        return
      }
      this.analyzeSubmitting = true
      analyzeProjectApi({
        projectId: this.projectId,
        forceReanalyze: false
      })
        .then((res) => {
          const analysisTaskId = extractAjaxPayload(res)
          this.summary.analysisTaskId = analysisTaskId
          this.summary.analysisStatus = 'PENDING'
          this.summary.errorMessage = ''
          this.$modal.msgSuccess('分析任务已提交')
          this.startPolling(analysisTaskId)
        })
        .finally(() => {
          this.analyzeSubmitting = false
        })
    },
    handleRetryAnalyze() {
      if (!this.canRetryAnalysis) {
        return
      }
      this.retrySubmitting = true
      retryProjectAiTaskApi(this.summary.analysisTaskId)
        .then((res) => {
          const analysisTaskId = extractAjaxPayload(res)
          this.summary.analysisTaskId = analysisTaskId
          this.summary.analysisStatus = 'PENDING'
          this.summary.errorMessage = ''
          this.$modal.msgSuccess('分析任务已重新提交')
          this.startPolling(analysisTaskId)
        })
        .finally(() => {
          this.retrySubmitting = false
        })
    },
    handleGenerateWeeklyReport() {
      if (!this.projectId || !this.canGenerateWeeklyReport) {
        return
      }
      if (!this.weeklyRange || this.weeklyRange.length !== 2) {
        this.$modal.msgError('请选择周报时间范围')
        return
      }
      this.weeklySubmitting = true
      generateProjectWeeklyReportApi({
        projectId: this.projectId,
        weekStart: this.weeklyRange[0],
        weekEnd: this.weeklyRange[1],
        forceRegenerate: this.forceRegenerate
      })
        .then((res) => {
          const reportId = extractAjaxPayload(res)
          this.$modal.msgSuccess('周报生成成功')
          return this.loadWeeklyReports(reportId)
        })
        .finally(() => {
          this.weeklySubmitting = false
        })
    },
    startPolling(analysisTaskId) {
      if (!analysisTaskId || !this.canQueryTask) {
        return
      }
      this.stopPolling()
      this.pollTask(analysisTaskId)
      this.pollingTimer = setInterval(() => {
        this.pollTask(analysisTaskId)
      }, 3000)
    },
    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }
    },
    pollTask(analysisTaskId) {
      if (!this.canQueryTask) {
        this.stopPolling()
        return
      }
      getProjectAiTaskApi(analysisTaskId)
        .then((res) => {
          const task = res.data || {}
          this.summary.analysisTaskId = task.id || analysisTaskId
          this.summary.analysisStatus = task.status || this.summary.analysisStatus
          this.summary.errorMessage = task.errorMessage || ''
          if (this.summary.analysisStatus === 'FAILED') {
            this.stopPolling()
            return
          }
          if (!this.isTaskRunning(this.summary.analysisStatus)) {
            this.stopPolling()
            this.reloadAll()
          }
        })
        .catch(() => {
          this.stopPolling()
        })
    },
    isTaskRunning(status) {
      return status === 'PENDING' || status === 'RUNNING'
    },
    resolveHealthLevelType(level) {
      if (level === 'HEALTHY') {
        return 'success'
      }
      if (level === 'WARNING') {
        return 'warning'
      }
      if (level === 'DANGER') {
        return 'danger'
      }
      return 'info'
    },
    resolveRiskLevelType(level) {
      if (level === 'HIGH') {
        return 'danger'
      }
      if (level === 'MEDIUM') {
        return 'warning'
      }
      return 'info'
    },
    resolveTaskStatusType(status) {
      if (status === 'SUCCESS') {
        return 'success'
      }
      if (status === 'FAILED') {
        return 'danger'
      }
      return 'warning'
    },
    formatHealthLevel(level) {
      if (level === 'HEALTHY') {
        return '健康'
      }
      if (level === 'WARNING') {
        return '预警'
      }
      if (level === 'DANGER') {
        return '危险'
      }
      return '暂无'
    },
    formatRiskLevel(level) {
      if (level === 'HIGH') {
        return '高'
      }
      if (level === 'MEDIUM') {
        return '中'
      }
      if (level === 'LOW') {
        return '低'
      }
      return '未知'
    },
    formatTaskStatus(status) {
      if (status === 'PENDING') {
        return '排队中'
      }
      if (status === 'RUNNING') {
        return '分析中'
      }
      if (status === 'SUCCESS') {
        return '已完成'
      }
      if (status === 'FAILED') {
        return '失败'
      }
      return '暂无'
    },
    formatRiskTypeCell(row) {
      return this.formatRiskType(row.riskType)
    },
    formatRiskType(type) {
      const mapping = {
        TASK_DELAY: '任务延期',
        TASK_NEAR_DUE: '任务临期',
        TASK_BLOCKED: '任务阻塞',
        MEMBER_OVERLOAD: '成员过载',
        WORKFLOW_BLOCKED: '流程卡点'
      }
      return mapping[type] || type || '未知'
    },
    formatWeeklyStatus(status) {
      if (status === 'SUCCESS') {
        return '成功'
      }
      if (status === 'FAILED') {
        return '失败'
      }
      return status || '未知'
    },
    resolveWeeklyStatusType(status) {
      if (status === 'SUCCESS') {
        return 'success'
      }
      if (status === 'FAILED') {
        return 'danger'
      }
      return 'info'
    },
    displayValue(value, fallback) {
      return value === undefined || value === null || value === '' ? fallback : value
    }
  }
}
</script>

<style scoped lang="scss">
.toolbar-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.summary-card,
.risk-card,
.report-list-card,
.report-content-card {
  margin-bottom: 20px;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-label {
  color: #909399;
  font-size: 12px;
}

.metric-value {
  color: #303133;
  font-size: 24px;
  font-weight: 600;
  line-height: 1;
}

.metric-value.danger {
  color: #f56c6c;
}

.narrative-block {
  margin-top: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.task-error-alert {
  margin-top: 16px;
}

.block-title {
  color: #303133;
  font-weight: 600;
  margin-bottom: 12px;
}

.block-content,
.report-content {
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.report-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 360px;
  overflow-y: auto;
}

.report-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.report-item:hover,
.report-item.active {
  border-color: #409eff;
  background: #f4f9ff;
}

.report-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.report-item-range {
  color: #303133;
  font-size: 13px;
}

.report-item-time,
.report-meta {
  color: #909399;
  font-size: 12px;
}

.report-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

@media (max-width: 1400px) {
  .metric-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
