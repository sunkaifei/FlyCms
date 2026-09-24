package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.module.other.service.DbBackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 系统工具 REST 接口：数据库备份（纯 Java 导出，见 DbBackupService）。
 *
 * 权限节点（stage-k-db-backup.sql）：
 *   900290 /api/system/tools/db/list      备份列表（菜单）
 *   900291 /api/system/tools/db/backup    执行备份
 *   900292 /api/system/tools/db/download  下载
 *   900293 /api/system/tools/db/delete    删除
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/tools")
public class ApiToolsController extends ApiBaseController {

    @Autowired
    private DbBackupService dbBackupService;

    /** 备份文件列表（按时间倒序） */
    @ResponseBody
    @GetMapping("/db/list")
    public DataVo list() {
        requirePermission("/api/system/tools/db/list");
        try {
            List<DbBackupService.BackupFile> files = dbBackupService.list();
            return DataVo.success("操作成功", files);
        } catch (IOException e) {
            return DataVo.failure("读取备份目录失败：" + e.getMessage());
        }
    }

    /** 执行全库备份（结构 + 数据），同步返回新文件信息 */
    @ResponseBody
    @PostMapping("/db/backup")
    public DataVo backup() {
        requirePermission("/api/system/tools/db/backup");
        try {
            DbBackupService.BackupFile file = dbBackupService.backup();
            return DataVo.success("备份完成", file);
        } catch (IOException | IllegalStateException e) {
            return DataVo.failure(e.getMessage());
        }
    }

    /** 下载备份文件 */
    @GetMapping("/db/download")
    public ResponseEntity<byte[]> download(@RequestParam("name") String name) throws IOException {
        requirePermission("/api/system/tools/db/download");
        Path path = dbBackupService.resolveForDownload(name);
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        byte[] body = Files.readAllBytes(path);
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(body.length)
                .body(body);
    }

    /** 删除备份文件 */
    @ResponseBody
    @PostMapping("/db/delete")
    public DataVo delete(@RequestParam("name") String name) {
        requirePermission("/api/system/tools/db/delete");
        try {
            boolean ok = dbBackupService.delete(name);
            return ok ? DataVo.success("删除成功", null) : DataVo.failure("文件不存在");
        } catch (IllegalArgumentException | IOException e) {
            return DataVo.failure(e.getMessage());
        }
    }
}
