package com.flycms.core.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * 上传安全工具（规划 §8 阶段 A5，对标帝国"参数白名单式安全设计"）。
 *
 * 老 CMS 的上传漏洞（Dede/PHPCMS 重灾区）几乎都源于"只信客户端声明"：
 * 只看 Content-Type（客户端可控）或只看扩展名（可双后缀绕过 xx.php.jpg）。
 * 这里强制三道校验，缺一不可：
 * 1. 扩展名白名单（取最后一个点之后，大小写无关）；
 * 2. MIME 白名单（允许 IE 历史别名，如 image/pjpeg、image/x-png）；
 * 3. 文件头魔数校验（防止"改扩展名的可执行文件"落到静态目录）；
 * 通过后一律强制重命名（UUID + 白名单扩展名），杜绝 ../、特殊字符与双后缀。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public final class UploadSafeUtil {

    private UploadSafeUtil() {
    }

    /** 图片类型：扩展名 + 允许 MIME + 文件头魔数（十六进制，自文件起始处匹配） */
    public enum ImageType {
        JPG("jpg", new String[]{"image/jpeg", "image/pjpeg", "image/jpg"}, "FFD8FF"),
        PNG("png", new String[]{"image/png", "image/x-png"}, "89504E47"),
        GIF("gif", new String[]{"image/gif"}, "47494638"),
        BMP("bmp", new String[]{"image/bmp", "image/x-ms-bmp", "image/x-bmp"}, "424D"),
        WEBP("webp", new String[]{"image/webp"}, "52494646");

        private final String ext;
        private final String[] mimeTypes;
        private final String magic;

        ImageType(String ext, String[] mimeTypes, String magic) {
            this.ext = ext;
            this.mimeTypes = mimeTypes;
            this.magic = magic;
        }

        public String getExt() {
            return ext;
        }

        public boolean mimeMatches(String contentType) {
            if (contentType == null) {
                return false;
            }
            String v = contentType.toLowerCase(Locale.ROOT);
            return Arrays.asList(mimeTypes).contains(v);
        }

        /** 文件头是否匹配魔数（WEBP 额外校验第 8 字节起的 "WEBP"） */
        public boolean magicMatches(byte[] head) {
            String hex = toHex(head, magic.length() / 2);
            if (!magic.equalsIgnoreCase(hex)) {
                return false;
            }
            if (this == WEBP) {
                return head.length >= 12
                        && "WEBP".equalsIgnoreCase(new String(head, 8, 4, java.nio.charset.StandardCharsets.US_ASCII));
            }
            return true;
        }
    }

    /** 默认单文件大小上限 2MB，与各上传端点历史限制一致 */
    public static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;

    /**
     * 安全文件名（强制重命名）：UUID + 白名单扩展名，不含任何原始文件名字符。
     */
    public static String rename(ImageType type) {
        return UUID.randomUUID().toString().replace("-", "") + "." + type.getExt();
    }

    /**
     * 三重校验。通过返回图片类型，任一项不通过返回 null。
     *
     * @param file    上传文件
     * @param maxSize 大小上限（字节），<=0 时不校验大小
     */
    public static ImageType safeImage(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (maxSize > 0 && file.getSize() > maxSize) {
            return null;
        }
        ImageType type = null;
        for (ImageType t : ImageType.values()) {
            if (t.mimeMatches(file.getContentType())) {
                type = t;
                break;
            }
        }
        if (type == null) {
            // MIME 缺失/异常时才退而求其次看扩展名——但仍必须过魔数校验
            type = typeByExt(extOf(file.getOriginalFilename()));
            if (type == null) {
                return null;
            }
        } else if (!type.getExt().equalsIgnoreCase(extOf(file.getOriginalFilename()))) {
            // MIME 与扩展名必须指向同一类型，防"改名绕过"
            ImageType byExt = typeByExt(extOf(file.getOriginalFilename()));
            if (byExt == null) {
                return null;
            }
            type = byExt;
        }
        byte[] head = new byte[12];
        try (java.io.InputStream in = file.getInputStream()) {
            int n = in.read(head);
            if (n < 2) {
                return null;
            }
            if (n < head.length) {
                head = Arrays.copyOf(head, n);
            }
        } catch (IOException e) {
            return null;
        }
        return type.magicMatches(head) ? type : null;
    }

    /** 取原始文件名最后一个点之后的扩展名（无·则返回空串） */
    private static String extOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int i = originalName.lastIndexOf('.');
        return i < 0 || i == originalName.length() - 1 ? "" : originalName.substring(i + 1);
    }

    private static ImageType typeByExt(String ext) {
        if (ext == null) {
            return null;
        }
        String v = ext.toLowerCase(Locale.ROOT);
        for (ImageType t : ImageType.values()) {
            if (t.getExt().equals(v)) {
                return t;
            }
        }
        return null;
    }

    private static String toHex(byte[] data, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len && i < data.length; i++) {
            sb.append(String.format("%02X", data[i]));
        }
        return sb.toString();
    }
}
