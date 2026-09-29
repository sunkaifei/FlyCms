package com.flycms.core.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * G16 草稿预览令牌：HMAC-SHA256 签名的短时效令牌，绑定（adminId, modelCode, targetId）。
 *
 * <p>密钥 = 每次启动随机生成（预览令牌本就短时效，重启失效是可接受语义，无需持久化）。
 * 预览响应必须带 {@code X-Robots-Tag: noindex}（由 ModelController 设置），防止草稿进搜索引擎。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public final class PreviewTokenUtils {

    private static final String SECRET = randomSecret();
    private static final long TTL_MILLIS = 30 * 60 * 1000L;

    private PreviewTokenUtils() {
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** 签发：返回 {@code adminId.expiryMillis.sig} */
    public static String mint(Long adminId, String modelCode, Long targetId) {
        long expiry = System.currentTimeMillis() + TTL_MILLIS;
        String payload = adminId + "." + modelCode + "." + targetId + "." + expiry;
        return payload + "." + sign(payload);
    }

    /**
     * 校验：签名 + 有效期 + (modelCode, targetId) 绑定。
     * adminId 只参与签名（审计溯源），校验侧无需知悉。
     */
    public static boolean verify(String modelCode, Long targetId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 5) {
            return false;
        }
        String payload = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        if (!constantTimeEquals(sign(payload), parts[4])) {
            return false;
        }
        try {
            Long.parseLong(parts[0]);
            Long.parseLong(parts[2]);
            return parts[1].equals(modelCode)
                    && Long.parseLong(parts[2]) == targetId
                    && Long.parseLong(parts[3]) >= System.currentTimeMillis();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 初始化失败", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
