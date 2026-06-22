<template>
  <el-tabs v-if="projectData" v-model="activeName" class="app-container">
    <el-tab-pane label="概况" name="overview">
      <Overview :project-data="projectData" />
    </el-tab-pane>
    <el-tab-pane label="任务" name="task">
      <Task :project-data="projectData" />
    </el-tab-pane>
    <el-tab-pane label="文件" name="file">
      <File :project-data="projectData" />
    </el-tab-pane>
    <el-tab-pane label="成员" name="member">
      <Member :project-data="projectData" />
    </el-tab-pane>
    <el-tab-pane v-if="showAiTab" label="AI 风险分析" name="ai">
      <Ai :project-data="projectData" />
    </el-tab-pane>
    <el-tab-pane label="甘特图" name="gantt">敬请期待</el-tab-pane>
  </el-tabs>
</template>

<script>
import Overview from './components/Overview.vue'
import Task from './components/Task.vue'
import File from './components/File.vue'
import Member from './components/Member.vue'
import Ai from './components/Ai.vue'
import auth from '@/plugins/auth'
import { getProjectDetailApi } from '@/api/pmhub-project/my-project.js'

const AI_PERMISSION_LIST = [
  'project:ai:summary',
  'project:ai:risks',
  'project:ai:task',
  'project:ai:analyze',
  'project:ai:weeklyReportGenerate',
  'project:ai:weeklyReportList'
]

export default {
  name: 'MyProjectInfo',
  components: { Overview, Task, File, Member, Ai },
  data() {
    return {
      projectData: null,
      activeName: 'overview'
    }
  },
  computed: {
    showAiTab() {
      return auth.hasPermiOr(AI_PERMISSION_LIST)
    }
  },
  created() {
    const projectId = this.$route.query.projectId
    if (this.$route.query.tab === 'ai' && this.showAiTab) {
      this.activeName = 'ai'
    }
    getProjectDetailApi(projectId)
      .then((res) => {
        this.projectData = res.data
      })
      .catch(() => {
        this.$router.replace('/pmhub-project/my-project')
      })
  }
}
</script>

<style scoped lang="scss"></style>
