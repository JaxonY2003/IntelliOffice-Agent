export const roleOptions = [
  {
    key: 'admin',
    label: '管理员',
    heading: '管理员登录',
    description: '管理知识库、日志记录与系统配置。',
    accent: 'blue',
    workspaceTitle: '系统控制台',
  },
  {
    key: 'manager',
    label: '部门管理者',
    heading: '部门管理者登录',
    description: '查看部门数据、审批流程与团队协作事务。',
    accent: 'gold',
    workspaceTitle: '部门协作台',
  },
  {
    key: 'employee',
    label: '员工',
    heading: '员工登录',
    description: '查询制度、发起问答、查看个人业务进度。',
    accent: 'pink',
    workspaceTitle: '个人工作台',
  },
]

export const mockAccounts = {
  employee: {
    username: 'employee01',
    password: '123456',
    displayName: '张晓宁',
    roleName: '普通员工',
  },
  manager: {
    username: 'manager01',
    password: 'manager123',
    displayName: '周明远',
    roleName: '部门管理者',
  },
  admin: {
    username: 'admin01',
    password: 'admin123',
    displayName: '林知远',
    roleName: '系统管理员',
  },
}

export const rolePresets = {
  employee: {
    starterTitle: '报销流程说明',
    starterPrompt: '帮我总结一下本月差旅报销的提交流程。',
    firstReply:
      '可以，我会先按“发起申请、补充单据、审批流转、财务核验”四个阶段帮你梳理，并补充常见遗漏项。',
  },
  manager: {
    starterTitle: '团队周报汇总',
    starterPrompt: '请整理一个适合部门例会使用的团队周报摘要。',
    firstReply:
      '没问题，我可以把项目进展、风险事项、待协调资源和下周重点拆成一页式摘要，方便你直接带去开会。',
  },
  admin: {
    starterTitle: '知识库巡检',
    starterPrompt: '帮我列一份知识库巡检清单，包含权限、内容和日志维度。',
    firstReply:
      '可以，我会把巡检清单分成账号权限、文档内容、接口健康度和审计日志四部分，适合管理员定期检查。',
  },
}
