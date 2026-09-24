package com.flycms.module.other.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据库备份服务（纯 Java 导出，不依赖 mysqldump 可执行文件，Windows/Linux 通用）。
 *
 * 产出为标准 SQL 文件（utf8mb4）：每表 SHOW CREATE TABLE 的 DDL + 流式分批读取的
 * INSERT 数据，备份目录为工作目录下 ./backup/（与 uploadfiles/ 同级惯例）。
 * 文件名格式 flycms_yyyyMMdd_HHmmss.sql，列表/删除/下载均做路径穿越校验。
 *
 * 还原不在本期范围：整库覆盖风险高，且本服务产出的 SQL 可直接用
 * mysql 客户端 source 导入，下载后手工或脚本还原即可。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class DbBackupService {

    private static final Logger logger = LoggerFactory.getLogger(DbBackupService.class);

    /** 备份目录：工作目录下 ./backup/ */
    private static final Path BACKUP_DIR = Paths.get("backup");

    /** 文件名白名单：只允许字母数字下划线中划线和 .sql 后缀，杜绝路径穿越 */
    private static final Pattern SAFE_NAME = Pattern.compile("^flycms_[A-Za-z0-9_\\-]+\\.sql$");

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final DataSource dataSource;

    public DbBackupService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** 备份文件行：文件名/大小/备份时间 */
    public static class BackupFile {
        private String name;
        private long sizeBytes;
        private long backupTime;

        public BackupFile(String name, long sizeBytes, long backupTime) {
            this.name = name;
            this.sizeBytes = sizeBytes;
            this.backupTime = backupTime;
        }

        public String getName() { return name; }
        public long getSizeBytes() { return sizeBytes; }
        public long getBackupTime() { return backupTime; }
    }

    /**
     * 执行全库备份（结构 + 数据）。表数量与数据量为 CMS 量级（几十表），同步执行即可。
     *
     * @return 备份文件行（含耗时前的文件名/大小/时间）
     * @throws IOException 文件写入失败
     * @throws IllegalStateException 数据库查询失败
     */
    public BackupFile backup() throws IOException {
        Files.createDirectories(BACKUP_DIR);
        String fileName = "flycms_" + TS.format(Instant.now().atZone(ZoneId.systemDefault())) + ".sql";
        Path target = BACKUP_DIR.resolve(fileName);
        long start = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection();
             BufferedWriter out = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {

            header(out, conn);

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = conn.createStatement().executeQuery("SHOW TABLES")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }

            for (String table : tables) {
                writeTable(conn, out, table);
            }
            out.write("-- backup finished at " + new Date() + "\n");
        } catch (java.sql.SQLException e) {
            // 失败即删半成品，避免残留脏文件被误下载还原
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            logger.error("数据库备份失败", e);
            throw new IllegalStateException("备份失败：" + e.getMessage(), e);
        }
        logger.info("数据库备份完成：{}（{} ms）", fileName, System.currentTimeMillis() - start);
        return stat(target);
    }

    /** 列出全部备份文件（按时间倒序） */
    public List<BackupFile> list() throws IOException {
        List<BackupFile> files = new ArrayList<>();
        if (!Files.isDirectory(BACKUP_DIR)) {
            return files;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BACKUP_DIR, "flycms_*.sql")) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    files.add(stat(p));
                }
            }
        }
        files.sort(Comparator.comparingLong(BackupFile::getBackupTime).reversed());
        return files;
    }

    /** 删除指定备份文件 */
    public boolean delete(String name) throws IOException {
        Path target = safeResolve(name);
        return Files.deleteIfExists(target);
    }

    /** 取备份文件绝对路径（供控制器流式下载），非法名抛 IllegalArgumentException */
    public Path resolveForDownload(String name) {
        return safeResolve(name);
    }

    // /////////////////// 内部实现 ///////////////////

    /** 路径穿越校验 + 文件存在性检查 */
    private Path safeResolve(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("非法的备份文件名");
        }
        Path target = BACKUP_DIR.resolve(name).normalize();
        if (!target.startsWith(BACKUP_DIR.toAbsolutePath().normalize())
                && !BACKUP_DIR.isAbsolute()) {
            throw new IllegalArgumentException("非法的备份文件名");
        }
        return target;
    }

    private BackupFile stat(Path p) {
        try {
            FileTime t = Files.getLastModifiedTime(p);
            return new BackupFile(p.getFileName().toString(), Files.size(p), t.toMillis());
        } catch (IOException e) {
            return new BackupFile(p.getFileName().toString(), 0L, 0L);
        }
    }

    private void header(BufferedWriter out, Connection conn) throws IOException, java.sql.SQLException {
        String schema;
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT DATABASE()")) {
            rs.next();
            schema = rs.getString(1);
        }
        out.write("-- FlyCms Database Backup\n");
        out.write("-- database: " + schema + "\n");
        out.write("-- generated: " + new Date() + "\n");
        out.write("-- host: " + conn.getMetaData().getDatabaseProductVersion() + "\n\n");
        out.write("SET NAMES utf8mb4;\n");
        out.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
    }

    private void writeTable(Connection conn, BufferedWriter out, String table)
            throws IOException, java.sql.SQLException {
        out.write("-- ----------------------------\n");
        out.write("-- Table structure for " + table + "\n");
        out.write("-- ----------------------------\n");
        out.write("DROP TABLE IF EXISTS `" + table + "`;\n");

        // 1. 表结构
        try (PreparedStatement ps = conn.prepareStatement("SHOW CREATE TABLE `" + table + "`");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                out.write(rs.getString(2) + ";\n\n");
            }
        }

        // 2. 数据（流式分批读，列名/值全转义；行内 NULL 与二进制安全处理）
        long rows = 0;
        try (Statement st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            st.setFetchSize(1000);
            try (ResultSet rs = st.executeQuery("SELECT * FROM `" + table + "`")) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                StringBuilder colsSql = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) colsSql.append(", ");
                    colsSql.append('`').append(meta.getColumnName(i)).append('`');
                }
                StringBuilder batch = new StringBuilder();
                int inBatch = 0;
                while (rs.next()) {
                    if (inBatch == 0) {
                        batch.append("INSERT INTO `").append(table).append("` (")
                                .append(colsSql).append(") VALUES\n");
                    }
                    batch.append('(');
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) batch.append(", ");
                        batch.append(escape(rs.getObject(i)));
                    }
                    batch.append(')');
                    inBatch++;
                    rows++;
                    // 每 100 行落一个 multi-row INSERT，兼顾文件大小与导入速度
                    if (inBatch >= 100) {
                        batch.append(";\n");
                        out.write(batch.toString());
                        batch.setLength(0);
                        inBatch = 0;
                    } else {
                        batch.append(",\n");
                    }
                }
                if (inBatch > 0) {
                    // 收尾：去掉最后多余的 ",\n" 再补分号
                    batch.setLength(batch.length() - 2);
                    batch.append(";\n");
                    out.write(batch.toString());
                }
            }
        }
        if (rows > 0) {
            out.write("\n-- " + rows + " rows for " + table + "\n");
        }
        out.write("\n");
    }

    /** SQL 字面量转义（\0 \n \r \ ' " \x1a），NULL 返回 NULL，其余 toString 转义 */
    private String escape(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof byte[]) {
            return "_binary " + hexLiteral((byte[]) value);
        }
        String s = value.toString();
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('\'');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\0': sb.append("\\0"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case 0x1a: sb.append("\\Z"); break;
                default: sb.append(c);
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    private String hexLiteral(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2 + 3);
        sb.append("X'");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        sb.append('\'');
        return sb.toString();
    }
}
