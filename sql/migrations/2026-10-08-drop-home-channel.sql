-- ============================================================
-- 删除与站点根首页重复的「首页」栏目（corp 预设遗留）
--
-- 背景（2026-10-08 排查）：
--   presets/corp.json 里曾有一条栏目 { name:首页, dir:home, channelType:3, listTemplate:index.html }，
--   作者的意图是"用聚合栏目 + index.html 做站点首页"，但三处实现都没接上：
--     ① 入口占不到 /：channelType != 2 的栏目一律渲染为 /{channelDir}/，它的地址只能是 /home/
--     ② list_template 历史上无人消费：getListTemplate() 全后端 0 个调用点
--     ③ 顶级聚合栏目天然取不到数：renderAggregate 用 offspring(id) 收集子栏目，而它的 father_id=0
--   结果：导航里出现第二个「首页」（一个来自 header.html 写死的 <a href="/">，一个来自本栏目），
--         且 /home/ 是一页空列表 + 重复面包屑「首页 / 首页」。
--
-- 同步修复：TemplateResolver 新增第 0 档"调用方显式指定模板"，ChannelRenderService 的
--          renderList / renderAggregate 开始消费 channel.getListTemplate()，
--          后台「栏目管理」的列表模板输入框不再是假功能。
--          并从 presets/corp.json 移除该栏目及其 guides 引用，避免重新导入时复发。
-- ============================================================

-- 只删这条 corp 预设遗留的聚合栏目（channel_type=3），不碰用户后来自建的其它 home 栏目
DELETE FROM fly_channel
 WHERE channel_dir = 'home'
   AND channel_type = 3
   AND father_id = 0
   AND model_id = 0;

-- 核对：应为 0；导航里应只剩 header.html 写死的那一个「首页」
SELECT COUNT(*) AS home_channel_left FROM fly_channel WHERE channel_dir = 'home';
SELECT id, channel_dir, channel_name, channel_type, list_template, father_id, model_id
  FROM fly_channel ORDER BY id;
