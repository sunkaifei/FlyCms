package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.AdminSessionUtils;
import com.flycms.module.template.service.TagManualService;
import com.flycms.module.template.service.TemplateCenterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模板中心 REST（规划 §8 阶段 D）：版本历史/回滚、皮肤 CRUD、皮肤包导入导出、模板试渲染、在线标签手册。
 *
 * 在线编辑的 read/save/create/delete 仍在 ApiWebsiteController（保持既有权限与路径不变），
 * 其 save 已改为走 TemplateCenterService，因此同样享有"语法校验 + 版本快照 + 即时生效"。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiTemplateController extends ApiBaseController {

    @Autowired
    private TemplateCenterService templateCenterService;
    @Autowired
    private TagManualService tagManualService;

    // /////////////////// D1 版本历史与回滚 ///////////////////

    /** 版本历史（倒序 100 条） */
    @ResponseBody
    @GetMapping("/system/template/versions")
    public DataVo versions(@RequestParam(value = "file") String file,
                           @RequestParam(value = "skin", required = false) String skin) {
        requirePermission("/api/system/template/files");
        return DataVo.success("操作成功", templateCenterService.versions(skin(), file));
    }

    /** 回滚：把历史版本作为新版本写入，历史不丢 */
    @ResponseBody
    @PostMapping("/system/template/restore")
    public DataVo restore(@RequestParam("file") String file,
                          @RequestParam("version") int version) {
        requirePermission("/api/system/template/restore");
        Long adminId = currentAdminId();
        return templateCenterService.restore(skin(), file, version, adminId);
    }

    // /////////////////// D8 试渲染 ///////////////////

    /** 不落盘渲染当前编辑器内容，用于"写完立刻知道效果" */
    @ResponseBody
    @PostMapping("/system/template/preview")
    public DataVo preview(@RequestParam(value = "content", required = false) String content,
                          @RequestParam(value = "file", required = false) String file,
                          @RequestParam(value = "model", required = false) String modelCode) {
        requirePermission("/api/system/template/preview");
        Map<String, Object> model = templateCenterService.sampleModel(modelCode);
        if (StringUtils.isNotBlank(file)) {
            model.put("file", file);
        }
        return templateCenterService.preview(content, model);
    }

    /** 仅做语法校验（保存前手速检查，不渲染） */
    @ResponseBody
    @PostMapping("/system/template/check")
    public DataVo check(@RequestParam(value = "content", required = false) String content,
                        @RequestParam(value = "file", required = false) String file) {
        requirePermission("/api/system/template/files");
        return templateCenterService.validateSyntax(
                StringUtils.isBlank(file) ? "__check__" : file, content == null ? "" : content);
    }

    // /////////////////// D2/D3 皮肤管理 ///////////////////

    /** 皮肤列表 + 当前皮肤 */
    @ResponseBody
    @GetMapping("/system/skin/list")
    public DataVo skins() {
        requirePermission("/api/system/template/files");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skins", templateCenterService.listSkins());
        data.put("current", templateCenterService.currentSkin());
        return DataVo.success("操作成功", data);
    }

    /** 新建皮肤（可从现有皮肤复制） */
    @ResponseBody
    @PostMapping("/system/skin/save")
    public DataVo saveSkin(@RequestParam("skin") String skin,
                           @RequestParam(value = "from", required = false) String from) {
        requirePermission("/api/system/skin/save");
        return templateCenterService.createSkin(skin, from);
    }

    /** 删除皮肤（拒绝删当前使用皮肤） */
    @ResponseBody
    @PostMapping("/system/skin/delete")
    public DataVo deleteSkin(@RequestParam("skin") String skin) {
        requirePermission("/api/system/skin/delete");
        return templateCenterService.deleteSkin(skin);
    }

    // /////////////////// D4/D5 皮肤包 ///////////////////

    /** D4 导出皮肤包（zip 下载，含 manifest.json） */
    @GetMapping("/system/skin/export")
    public ResponseEntity<org.springframework.core.io.Resource> exportSkin(
            @RequestParam(value = "skin", required = false) String skin) {
        requirePermission("/api/system/skin/export");
        String target = StringUtils.isBlank(skin) ? templateCenterService.currentSkin() : skin;
        byte[] zip;
        try {
            zip = templateCenterService.exportSkin(target);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
        ByteArrayResource resource = new ByteArrayResource(zip);
        String filename = URLEncoder.encode(target + ".zip", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zip.length)
                .body(resource);
    }

    /** D5 导入皮肤包（zip-slip 防御 + 后缀白名单 + 50MB 上限） */
    @ResponseBody
    @PostMapping("/system/skin/import")
    public DataVo importSkin(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "overwrite", defaultValue = "0") int overwrite) {
        requirePermission("/api/system/skin/import");
        return templateCenterService.importSkin(file, overwrite == 1);
    }

    // /////////////////// D7 在线标签手册 ///////////////////

    /** 全部标签手册（按分组），后台组件面板/标签手册页数据源 */
    @ResponseBody
    @GetMapping("/system/tags/manual")
    public DataVo manual() {
        requirePermission("/api/system/template/files");
        return DataVo.success("操作成功", tagManualService.manual());
    }

    // /////////////////// 内部 ///////////////////

    private String skin() {
        return templateCenterService.currentSkin();
    }

    private Long currentAdminId() {
        com.flycms.module.admin.model.Admin admin = AdminSessionUtils.getLoginMember(request);
        return admin == null ? null : admin.getId();
    }
}
