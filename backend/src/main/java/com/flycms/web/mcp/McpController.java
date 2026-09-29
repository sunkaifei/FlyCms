package com.flycms.web.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.flycms.core.utils.PreviewTokenUtils;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.tag.service.TagService;
import com.flycms.module.config.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * G23 MCP server（Model Context Protocol，Streamable HTTP 传输的最小无状态实现）。
 *
 * <p>让外部 Agent（Claude / Cursor 等）安全读写站点内容。暴露 4 个工具：
 * <ul>
 *   <li>{@code model_list} — 已启用模型清单</li>
 *   <li>{@code content_search} — 关键词跨模型聚合搜索（TagService，仅已发布）</li>
 *   <li>{@code content_get} — 按 model+id 读已发布内容（含自定义字段与附件/关联展开）</li>
 *   <li>{@code content_create_draft} — 创建内容（<b>强制 status=0 草稿</b>——MCP 写入红线，
 *       AI 产物必须人工审核后才可发布）</li>
 * </ul>
 *
 * <p>门禁：系统参数 {@code fly_mcp_token} 非空时功能开启，请求须带
 * {@code Authorization: Bearer <token>}（常量时间比较）；留空 = 整个端点 404（默认关闭）。
 * 传输：仅 Streamable HTTP 的 JSON 应答（无 SSE——工具型调用不需要服务端主动推送）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private static final String PROTOCOL_VERSION = "2025-03-26";
    private static final String SERVER_NAME = "FlyCms MCP";

    @Autowired
    private ConfigService configService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private TagService tagService;

    @GetMapping("/mcp")
    public ResponseEntity<String> getNotSupported() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("{\"error\":\"MCP endpoint accepts POST only\"}");
    }

    // 不限 consumes：JSON-RPC 客户端的 Content-Type 各异，门禁（令牌/开关）必须先于媒体类型判断
    @PostMapping(value = "/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> post(@RequestBody String body,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        // 门禁：token 留空 = 功能关闭（404，不暴露端点存在）
        String token = StringUtils.trimToEmpty(configService.getStringByKey("fly_mcp_token"));
        if (token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String presented = StringUtils.startsWithIgnoreCase(authorization, "Bearer ")
                ? authorization.substring(7).trim() : "";
        if (!constantTimeEquals(presented, token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(jsonRpcError(null, -32001, "Unauthorized"));
        }

        JSONObject req;
        try {
            req = JSON.parseObject(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(jsonRpcError(null, -32700, "Parse error"));
        }
        if (req == null || StringUtils.isBlank(req.getString("method"))) {
            return ResponseEntity.badRequest().body(jsonRpcError(null, -32600, "Invalid Request"));
        }
        String method = req.getString("method");
        Object id = req.get("id");
        JSONObject params = req.getJSONObject("params") == null
                ? new JSONObject() : req.getJSONObject("params");

        // 通知（无 id）：初始化完成通知等，202 空体
        if (id == null) {
            return ResponseEntity.accepted().build();
        }

        try {
            switch (method) {
                case "initialize" -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("protocolVersion", PROTOCOL_VERSION);
                    result.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
                    result.put("serverInfo", Map.of("name", SERVER_NAME, "version", "1.0"));
                    return ResponseEntity.ok(jsonRpcResult(id, result));
                }
                case "notifications/initialized" -> {
                    return ResponseEntity.accepted().build();
                }
                case "ping" -> {
                    return ResponseEntity.ok(jsonRpcResult(id, Map.of()));
                }
                case "tools/list" -> {
                    return ResponseEntity.ok(jsonRpcResult(id, Map.of("tools", tools())));
                }
                case "tools/call" -> {
                    String name = StringUtils.defaultString(params.getString("name"));
                    JSONObject args = params.getJSONObject("arguments") == null
                            ? new JSONObject() : params.getJSONObject("arguments");
                    return ResponseEntity.ok(jsonRpcResult(id, callTool(name, args)));
                }
                default -> {
                    return ResponseEntity.ok(jsonRpcError(id, -32601, "Method not found: " + method));
                }
            }
        } catch (Exception e) {
            log.warn("[MCP] tools/call 执行失败：{}", e.getMessage());
            return ResponseEntity.ok(jsonRpcError(id, -32603, "Internal error: " + e.getMessage()));
        }
    }

    // /////////////////// 工具定义 ///////////////////

    private List<Map<String, Object>> tools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(tool("model_list", "列出站点已启用的内容模型（code/name）", Map.of()));
        tools.add(tool("content_search", "按关键词跨模型搜索已发布内容，返回标题与访问链接",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of("type", "string", "description", "搜索关键词"),
                                "limit", Map.of("type", "integer", "description", "返回条数，默认 10，上限 50")),
                        "required", List.of("query"))));
        tools.add(tool("content_get", "按模型 code 与内容 id 读取一条已发布内容（含自定义字段）",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "modelCode", Map.of("type", "string", "description", "模型 code，如 articles"),
                                "id", Map.of("type", "string", "description", "内容 id")),
                        "required", List.of("modelCode", "id"))));
        tools.add(tool("content_create_draft", "创建一条内容（强制保存为待审草稿，需人工在后台审核发布）",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "modelCode", Map.of("type", "string", "description", "模型 code，如 articles"),
                                "title", Map.of("type", "string", "description", "标题"),
                                "content", Map.of("type", "string", "description", "正文（HTML 或纯文本）"),
                                "keywords", Map.of("type", "string", "description", "SEO 关键词，逗号分隔"),
                                "description", Map.of("type", "string", "description", "SEO 描述")),
                        "required", List.of("modelCode", "title"))));
        return tools;
    }

    private Map<String, Object> tool(String name, String description, Object inputSchema) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("inputSchema", inputSchema);
        return m;
    }

    // /////////////////// 工具执行 ///////////////////

    private Map<String, Object> callTool(String name, JSONObject args) throws Exception {
        Object result;
        switch (name) {
            case "model_list" -> result = modelList();
            case "content_search" -> result = contentSearch(
                    args.getString("query"),
                    args.getInteger("limit") == null ? 10 : Math.min(args.getIntValue("limit"), 50));
            case "content_get" -> result = contentGet(
                    args.getString("modelCode"), args.getLong("id"));
            case "content_create_draft" -> result = contentCreateDraft(args);
            default -> {
                return Map.of("content", List.of(Map.of("type", "text", "text", "未知工具：" + name)),
                        "isError", true);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", List.of(Map.of("type", "text", "text",
                result instanceof String s ? s : JSON.toJSONString(result))));
        out.put("isError", false);
        return out;
    }

    private List<Map<String, Object>> modelList() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Model m : modelService.getEnabledModels()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("code", m.getCode());
            o.put("name", m.getName());
            o.put("titleLabel", m.getTitleLabel());
            out.add(o);
        }
        return out;
    }

    private List<Map<String, Object>> contentSearch(String query, int limit) {
        TagService.TagResult result = tagService.search(
                StringUtils.trimToEmpty(query), 1, Math.max(limit, 1));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : result.getRowsList()) {
            String code = String.valueOf(row.get("__modelCode"));
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("modelCode", code);
            o.put("id", String.valueOf(row.get("id")));
            o.put("title", row.get("title"));
            o.put("url", "/" + code + "/" + row.get("shortUrl") + ".html");
            out.add(o);
        }
        return out;
    }

    private Map<String, Object> contentGet(String modelCode, Long id) {
        Model model = modelService.findModelByCode(modelCode);
        if (model == null || id == null) {
            return Map.of("error", "模型不存在或参数错误");
        }
        Map<String, Object> row = modelDataService.findDataById(model.getId(), id);
        if (row == null || !"1".equals(String.valueOf(row.get("status")))) {
            return Map.of("error", "内容不存在或未发布");
        }
        modelDataService.expandAttachments(model.getId(), new ArrayList<>(List.of(row)));
        return row;
    }

    /** MCP 写入红线：一律落 status=0 草稿，人工审核后才可发布 */
    private Map<String, Object> contentCreateDraft(JSONObject args) throws Exception {
        String modelCode = args.getString("modelCode");
        String title = args.getString("title");
        Model model = modelService.findModelByCode(modelCode);
        if (model == null || StringUtils.isBlank(title)) {
            return Map.of("error", "模型不存在或标题为空");
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("title", title);
        form.put("status", "0");
        copyIfPresent(args, form, "content", "content");
        copyIfPresent(args, form, "keywords", "keywords");
        copyIfPresent(args, form, "description", "description");
        if (args.get("categoryId") != null) {
            form.put("categoryId", String.valueOf(args.get("categoryId")));
        }
        JSONObject extra = args.getJSONObject("fields");
        if (extra != null) {
            for (Map.Entry<String, Object> e : extra.entrySet()) {
                if (e.getValue() != null) {
                    form.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }
        }
        InsertResult vo = insertDraft(model.getId(), form);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", vo.id());
        out.put("status", 0);
        out.put("message", vo.message());
        return out;
    }

    /** 借道 ModelDataService.insertData（字段校验/唯一约束/版本快照全继承）；data = 新内容 id */
    private record InsertResult(Long id, String message) {
    }

    private InsertResult insertDraft(Long modelId, Map<String, String> form) {
        com.flycms.core.entity.DataVo real = modelDataService.insertData(modelId, form, null);
        if (real.getCode() != com.flycms.core.entity.DataVo.CODE_SUCCESS) {
            throw new IllegalStateException(real.getMessage());
        }
        Long id = real.getData() == null ? null : Long.parseLong(String.valueOf(real.getData()));
        return new InsertResult(id, real.getMessage());
    }

    private void copyIfPresent(JSONObject args, Map<String, String> form, String from, String to) {
        if (args.get(from) != null) {
            form.put(to, String.valueOf(args.get(from)));
        }
    }

    // /////////////////// JSON-RPC 辅助 ///////////////////

    private String jsonRpcResult(Object id, Object result) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("jsonrpc", "2.0");
        out.put("id", id);
        out.put("result", result);
        return JSON.toJSONString(out);
    }

    private String jsonRpcError(Object id, int code, String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("jsonrpc", "2.0");
        out.put("id", id);
        out.put("error", Map.of("code", code, "message", message));
        return JSON.toJSONString(out);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
