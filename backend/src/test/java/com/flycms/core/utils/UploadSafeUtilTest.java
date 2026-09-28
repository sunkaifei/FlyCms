package com.flycms.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传安全三重校验测试（阶段 K4）。
 *
 * <p>老 CMS（Dede/PHPCMS）的上传漏洞几乎都来自"只信客户端声明"：
 * 只看 Content-Type 或只看扩展名。这里验证 FlyCms 的三道校验确实连成一条闭链。
 */
class UploadSafeUtilTest {

    /** 最小合法 PNG 头（sig + IHDR 起始） */
    private static final byte[] PNG_HEAD = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
    };

    /** 最小合法 JPG 头 */
    private static final byte[] JPG_HEAD = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    @Test
    @DisplayName("正常图片：扩展名+MIME+魔数三者一致时通过")
    void acceptsRealImage() {
        MockMultipartFile png = new MockMultipartFile("file", "a.png", "image/png", PNG_HEAD);
        assertEquals(UploadSafeUtil.ImageType.PNG, UploadSafeUtil.safeImage(png, 0));

        MockMultipartFile jpg = new MockMultipartFile("file", "a.jpg", "image/jpeg", JPG_HEAD);
        assertEquals(UploadSafeUtil.ImageType.JPG, UploadSafeUtil.safeImage(jpg, 0));
    }

    @Test
    @DisplayName("伪装脚本：扩展名改 .png 但内容不是图片 → 拒绝")
    void rejectsFakeExtension() {
        byte[] php = "<?php system($_GET['c']); ?>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile evil = new MockMultipartFile("file", "shell.png", "image/png", php);
        assertNull(UploadSafeUtil.safeImage(evil, 0));
    }

    @Test
    @DisplayName("双后缀绕过：shell.php.jpg 扩展名取最后一段=jpg，但魔数不符 → 拒绝")
    void rejectsDoubleExtension() {
        byte[] php = "<?php echo 1;".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile evil = new MockMultipartFile("file", "shell.php.jpg", "image/jpeg", php);
        assertNull(UploadSafeUtil.safeImage(evil, 0));
    }

    @Test
    @DisplayName("MIME 与扩展名指向不同类型 → 按扩展名重新判定，魔数不符则拒绝")
    void rejectsMismatchedMime() {
        // 声明 image/jpeg，文件名 a.png，内容却是 PNG —— 类型不一致，最终按扩展名判定为 PNG 且魔数匹配
        MockMultipartFile f = new MockMultipartFile("file", "a.png", "image/jpeg", PNG_HEAD);
        assertEquals(UploadSafeUtil.ImageType.PNG, UploadSafeUtil.safeImage(f, 0));
    }

    @Test
    @DisplayName("超限文件被拒")
    void rejectsOversize() {
        MockMultipartFile png = new MockMultipartFile("file", "big.png", "image/png", PNG_HEAD);
        assertNull(UploadSafeUtil.safeImage(png, 4));
    }

    @Test
    @DisplayName("空文件 / null 被拒")
    void rejectsEmpty() {
        assertNull(UploadSafeUtil.safeImage(null, 0));
        assertNull(UploadSafeUtil.safeImage(new MockMultipartFile("file", "a.png", "image/png", new byte[0]), 0));
    }

    @Test
    @DisplayName("强制重命名：不含原始文件名字符，扩展名来自白名单")
    void renameIsSafe() {
        String name = UploadSafeUtil.rename(UploadSafeUtil.ImageType.PNG);
        assertNotNull(name);
        assertTrue(name.endsWith(".png"));
        // 32 位十六进制 + ".png"
        assertTrue(name.matches("^[0-9a-f]{32}\\.png$"), name);
    }
}
