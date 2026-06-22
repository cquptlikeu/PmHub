<template>
  <div>
    <el-dialog title="发起审批流程" :visible.sync="visible" width="70%" :before-close="handleClose" @open="handleOpen">
      <div class="layout">
        <div v-if="formOpen" class="form-conf">
          <parser :key="formOpen" ref="parser" :form-conf="formData" @submit="submit" @getData="getData" />
        </div>
        <el-divider direction="vertical" />
        <div class="action">
          <el-button type="primary" size="small" @click="handleProcessView">查看流程图</el-button>
          <el-button type="primary" size="small" @click="handleJumpInfo">跳转到单据详情</el-button>
          <el-button type="primary" size="small" @click="handleDownload">下载最终交付物</el-button>
        </div>
      </div>
    </el-dialog>
    <!-- 娴佺▼鍥?-->
    <el-dialog :title="processView.title" :visible.sync="processView.open" width="70%" append-to-body>
      <process-viewer :key="`designer-${processView.index}`" :xml="processView.xmlData" :style="{ height: '400px' }" />
    </el-dialog>
  </div>
</template>

<script>
import { getProcessForm, getBpmnXml } from '@/api/workflow/process'
import { startProcessApi } from '@/api/pmhub-project/my-task'
import { getUserProfile } from '@/api/system/user'
import { getFileListApi } from '@/api/pmhub-project/my-project.js'
import Parser from '@/utils/generator/parser'
import ProcessViewer from '@/components/ProcessViewer'

let INFO_PATH = ''
let INFO_URL = ''

export default {
  name: 'Start',
  components: {
    Parser,
    ProcessViewer
  },
  props: {
    visible: {
      type: Boolean,
      required: true
    },
    taskId: {
      type: String,
      required: true
    },
    // 鍚庣杩斿洖鐨勫鎵硅缃暟鎹?
    workFlowable: {
      type: Object,
      required: true
    },
    // 閲嶆柊鑾峰彇浠诲姟鍒楄〃鏁版嵁
    getTableData: {
      type: Function,
      required: true
    },
    // 褰撳墠鍗曟嵁璇︽儏鏁版嵁
    info: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      user: {},
      formOpen: false,
      formData: {},
      processView: {
        title: '',
        open: false,
        index: undefined,
        xmlData: ''
      },
      taskFileList: []
    }
  },
  created() {
    INFO_PATH = `${window.location.pathname}/info`
    INFO_URL = `${window.location.origin}${INFO_PATH}`
    getUserProfile().then((response) => {
      // 鎷垮埌褰撳墠鐧诲綍鐨勭敤鎴蜂俊鎭紝瀹℃壒娴佺▼琛ㄥ崟闇€瑕佸～鍐欑敤鎴锋樀绉板瓧娈?
      this.user = response.data
    })
  },
  methods: {
    handleClose() {
      this.taskFileList = []
      this.formOpen = false
      this.$emit('update:visible', false)
    },
    handleOpen() {
      getFileListApi({
        pageNum: 1,
        pageSize: 50,
        fileName: undefined,
        id: this.taskId,
        type: 'task'
      })
        .then((res) => {
          // 鎷垮埌浜や粯鐗╂暟鎹?
          this.taskFileList = res.data.list
          this.initData()
        })
        .catch(() => {
          this.taskFileList = []
        })
    },
    // 鏌ョ湅娴佺▼鍥?
    handleProcessView() {
      const definitionId = this.workFlowable?.definitionId
      this.processView.title = '流程图'
      this.processView.index = definitionId
      // 鍙戦€佽姹傦紝鑾峰彇xml
      getBpmnXml(definitionId).then((res) => {
        this.processView.xmlData = res.data
      })
      this.processView.open = true
    },
    // 璺宠浆鍒拌鎯呴〉
    handleJumpInfo() {
      window.open(`${INFO_PATH}?taskId=${this.taskId}`)
    },
    // 涓嬭浇浜や粯鐗?
    handleDownload() {
      const projectFileId = this.taskFileList[0]?.projectFileId
      const fileUrl = this.taskFileList[0]?.fileUrl
      if (
        projectFileId === undefined ||
        fileUrl === undefined ||
        projectFileId === 'undefined' ||
        fileUrl === 'undefined'
      ) {
        this.$modal.msgError('没有交付物数据')
        return
      }
      this.download(
        '/project/file/download',
        {
          projectFileId,
          fileUrl
        },
        this.taskFileList[0]?.fileName
      )
    },
    initData() {
      getProcessForm({
        definitionId: this.workFlowable?.definitionId,
        deployId: this.workFlowable?.deploymentId
      }).then((res) => {
        if (res.data) {
          const fields = res.data.fields || []
          if (fields.length >= 12) {
            fields[0].__config__.defaultValue = '项目管理-任务审批'
            fields[1].__config__.defaultValue = this.user.nickName
            fields[2].__config__.defaultValue = `${INFO_URL}?taskId=${this.taskId}&projectFileId=${this.taskFileList[0]?.projectFileId}&fileUrl=${this.taskFileList[0]?.fileUrl}`
            fields[3].__config__.defaultValue = this.info.taskName
            fields[4].__config__.defaultValue = this.info.projectName
            fields[5].__config__.defaultValue = this.info.statusName
            fields[6].__config__.defaultValue = this.info.taskPriorityName
            fields[7].__config__.defaultValue = this.info.stageName
            fields[8].__config__.defaultValue = this.info.beginTime
            fields[9].__config__.defaultValue = this.info.endTime
            fields[10].__config__.defaultValue = this.info.closeTime
            // 浜や粯鐗?
            fields[11].__config__.defaultValue = this.taskFileList[0]?.fileName
          }
          this.formData = res.data
          this.formOpen = true
        }
      })
    },
    /** 鎺ユ敹瀛愮粍浠朵紶鐨勫€?*/
    getData(data) {
      if (data) {
        const variables = []
        data.fields.forEach((item) => {
          const variableData = {}
          variableData.label = item.__config__.label
          // 琛ㄥ崟鍊间负澶氫釜閫夐」鏃?
          if (item.__config__.defaultValue instanceof Array) {
            const array = []
            item.__config__.defaultValue.forEach((val) => {
              array.push(val)
            })
            variableData.val = array
          } else {
            variableData.val = item.__config__.defaultValue
          }
          variables.push(variableData)
        })
        this.variables = variables
      }
    },
    submit(data) {
      if (data && this.workFlowable?.definitionId) {
        // 鍚姩娴佺▼骞跺皢琛ㄥ崟鏁版嵁鍔犲叆娴佺▼鍙橀噺
        const url = encodeURIComponent(`${INFO_URL}?taskId=${this.taskId}`)
        startProcessApi(this.taskId, this.workFlowable?.definitionId, url, JSON.stringify(data.valData)).then((res) => {
          this.$modal.msgSuccess(res.msg)
          this.getTableData()
          this.handleClose()
        })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.layout {
  display: flex;
  // .form-conf {
  //   width: 80%;
  // }
  .el-divider--vertical {
    height: auto;
    margin: 0 30px;
  }
  .action {
    display: flex;
    flex-direction: column;
    .el-button {
      margin-bottom: 15px;
    }
    .el-button + .el-button {
      margin-left: 0px;
    }
  }
}
</style>
