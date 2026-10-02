package com.flycms.core.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

/**
 * 图片水印引擎（R 批次，对标帝国/Dede/PHPCMS/迅睿的水印设置共识）。
 *
 * <p>能力：文字/图片两种水印来源 × 九宫格位置（1 左上…5 居中…9 右下，10 随机格）×
 * 透明度（10–100%）× 边距（px）。按原格式写回（png 保透明、jpg 按 quality、
 * gif 跳过保动画）。小图（宽 < 最小宽）跳过不加水印。
 *
 * <p>配置从站点参数读取（fly_wm_*，见 ApiWebsiteController 白名单），全部数值键
 * 在 Controller 侧钳制白名单区间；本工具只做纯图像运算，无外部输入拼路径。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public final class ImageWatermarkUtil {

    private ImageWatermarkUtil() {
    }

    /** 水印配置（由调用方从站点参数组装，字段已钳制区间） */
    public static final class Config {
        public String type;        // text / image
        public String text;        // 水印文字（type=text）
        public int fontSize;       // 12–72
        public String fontColor;   // #RRGGBB
        public String imagePath;   // 水印图（type=image；站内绝对路径，磁盘）
        public int position;       // 1–9 九宫格，10 随机
        public int opacity;        // 10–100（%）
        public int margin;         // 0–100 px
        public int minWidth;       // 低于此宽不加水印
        public float quality;      // 0.5–1.0（jpg 写回质量）

        public static Config fromSiteParams(Map<String, String> kv) {
            Config c = new Config();
            c.type = "image".equalsIgnoreCase(kv.getOrDefault("fly_wm_type", "text")) ? "image" : "text";
            c.text = kv.getOrDefault("fly_wm_text", "");
            c.fontSize = clampInt(kv.get("fly_wm_font_size"), 16, 12, 72);
            c.fontColor = kv.getOrDefault("fly_wm_font_color", "#FFFFFF");
            c.imagePath = kv.getOrDefault("fly_wm_image", "");
            c.position = clampInt(kv.get("fly_wm_position"), 9, 1, 10);
            c.opacity = clampInt(kv.get("fly_wm_opacity"), 60, 10, 100);
            c.margin = clampInt(kv.get("fly_wm_margin"), 10, 0, 100);
            c.minWidth = clampInt(kv.get("fly_wm_min_width"), 300, 0, 4000);
            c.quality = clampInt(kv.get("fly_wm_quality"), 85, 50, 100) / 100f;
            return c;
        }

        private static int clampInt(String raw, int def, int min, int max) {
            try {
                return Math.max(min, Math.min(max, Integer.parseInt(raw.trim())));
            } catch (Exception e) {
                return def;
            }
        }
    }

    /**
     * 给图片文件加水印（原地覆写）。
     *
     * @return true=已加水印；false=跳过（gif/小图/配置不全/水印图缺失）——跳过不是错误
     */
    public static boolean apply(File imageFile, Config cfg) {
        try {
            if (imageFile == null || !imageFile.exists() || cfg == null) {
                return false;
            }
            String name = imageFile.getName().toLowerCase();
            String format = name.endsWith(".png") ? "png"
                    : name.endsWith(".gif") ? "gif"
                    : name.endsWith(".bmp") ? "bmp" : "jpg";
            if ("gif".equals(format)) {
                return false; // 保动画：GIF 不加水印（帝国/Dede 同口径）
            }
            BufferedImage src = ImageIO.read(imageFile);
            if (src == null || src.getWidth() < cfg.minWidth) {
                return false;
            }
            Graphics2D g = src.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float alpha = cfg.opacity / 100f;
            boolean drawn;
            if ("image".equals(cfg.type)) {
                drawn = drawImageWatermark(g, src, cfg, alpha);
            } else {
                drawn = drawTextWatermark(g, src, cfg, alpha);
            }
            g.dispose();
            if (!drawn) {
                return false;
            }
            return writeImage(src, imageFile, format, cfg.quality);
        } catch (Exception e) {
            // 水印失败不阻塞上传（保留原图）
            return false;
        }
    }

    private static boolean drawTextWatermark(Graphics2D g, BufferedImage src, Config cfg, float alpha) {
        if (cfg.text == null || cfg.text.isBlank()) {
            return false;
        }
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, cfg.fontSize)
                .deriveFont((float) cfg.fontSize);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(cfg.text);
        int th = fm.getHeight();
        Point p = positionOf(src.getWidth(), src.getHeight(), tw, th, cfg);
        if (p == null) {
            return false;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        Color color = parseColor(cfg.fontColor, Color.WHITE);
        g.setColor(color);
        // 文字基线偏移：y 为顶部，需加 ascent
        g.drawString(cfg.text, p.x, p.y + fm.getAscent());
        return true;
    }

    private static boolean drawImageWatermark(Graphics2D g, BufferedImage src, Config cfg, float alpha) {
        if (cfg.imagePath == null || cfg.imagePath.isBlank()) {
            return false;
        }
        BufferedImage mark;
        try {
            mark = ImageIO.read(new File(cfg.imagePath));
        } catch (Exception e) {
            return false;
        }
        if (mark == null) {
            return false;
        }
        // 水印图按原图宽 1/6 等比缩放（限 40–400px），过大过小都不合适
        int mw = Math.max(40, Math.min(400, src.getWidth() / 6));
        int mh = Math.max(1, mark.getHeight() * mw / Math.max(1, mark.getWidth()));
        Image scaled = mark.getScaledInstance(mw, mh, Image.SCALE_SMOOTH);
        Point p = positionOf(src.getWidth(), src.getHeight(), mw, mh, cfg);
        if (p == null) {
            return false;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(scaled, p.x, p.y, null);
        return true;
    }

    /**
     * 九宫格定位：1 2 3 / 4 5 6 / 7 8 9（10=随机取一格）。
     * 返回水印左上角坐标；图太小放不下（元素宽高超出）返回 null。
     */
    private static Point positionOf(int imgW, int imgH, int eleW, int eleH, Config cfg) {
        int availW = imgW - eleW - cfg.margin * 2;
        int availH = imgH - eleH - cfg.margin * 2;
        if (availW < 0 || availH < 0) {
            return null;
        }
        int col = (cfg.position == 10)
                ? (int) (Math.random() * 3) + 1
                : (cfg.position - 1) % 3 + 1;
        int row = (cfg.position == 10)
                ? (int) (Math.random() * 3) + 1
                : (cfg.position - 1) / 3 + 1;
        int x = cfg.margin + (col - 1) * availW / 2;
        int y = cfg.margin + (row - 1) * availH / 2;
        return new Point(x, y);
    }

    private static Color parseColor(String hex, Color def) {
        try {
            return Color.decode(hex == null || hex.isBlank() ? "#FFFFFF" : hex.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 按原格式写回（png 保透明；jpg 用质量参数；bmp 默认写） */
    private static boolean writeImage(BufferedImage img, File file, String format, float quality) {
        try {
            if ("jpg".equals(format)) {
                var writer = ImageIO.getImageWritersByFormatName("jpg").next();
                var out = ImageIO.createImageOutputStream(file);
                writer.setOutput(out);
                var param = writer.getDefaultWriteParam();
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
                writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
                out.flush();
                out.close();
                writer.dispose();
                return true;
            }
            return ImageIO.write(img, format, file);
        } catch (Exception e) {
            return false;
        }
    }
}
