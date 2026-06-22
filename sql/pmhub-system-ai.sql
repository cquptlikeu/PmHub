DELETE FROM `sys_menu`
WHERE `perms` IN (
  'project:ai:summary',
  'project:ai:risks',
  'project:ai:task',
  'project:ai:analyze',
  'project:ai:weeklyReportGenerate',
  'project:ai:weeklyReportList'
);

INSERT INTO `sys_menu` (
  `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`,
  `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
  `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
  ('AI摘要', 2016, 30, '', NULL, NULL, 1, 0, 'F', '0', '0', 'project:ai:summary', '#', 'admin', NOW(), 'admin', NOW(), '项目 AI 摘要权限'),
  ('AI风险列表', 2016, 31, '', NULL, NULL, 1, 0, 'F', '0', '0', 'project:ai:risks', '#', 'admin', NOW(), 'admin', NOW(), '项目 AI 风险列表权限'),
  ('AI任务查询', 2016, 32, '', NULL, NULL, 1, 0, 'F', '0', '0', 'project:ai:task', '#', 'admin', NOW(), 'admin', NOW(), '项目 AI 分析任务查询权限'),
  ('AI开始分析', 2016, 33, '', NULL, NULL, 1, 0, 'F', '0', '0', 'project:ai:analyze', '#', 'admin', NOW(), 'admin', NOW(), '项目 AI 分析触发权限'),
  ('AI周报生成', 2016, 34, '', NULL, NULL, 1, 0, 'F', '0', '0', 'project:ai:weeklyReportGenerate', '#', 'admin', NOW(), 'admin', NOW(), '项目 AI 周报生成权限'),
  ('AI周报列表', 2016, 35, '', NULL, NULL, 1, 0, 'F', '0', '0', 'project:ai:weeklyReportList', '#', 'admin', NOW(), 'admin', NOW(), '项目 AI 周报列表权限');
