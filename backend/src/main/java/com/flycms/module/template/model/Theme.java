package com.flycms.module.template.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 主题实体（规划 §6.1 / §12.1）。对应 {@code fly_theme} 表，同时承载从磁盘
 * {@code theme.json} 解析出的设计系统与能力声明（filesystem 为事实源，DB 仅登记）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public class Theme implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主题目录名（= theme.json 的 code） */
    private String code;
    private String name;
    private String version;
    private String author;
    /** 父主题 code，空=独立主题 */
    private String parentCode;
    private String thumbnail;
    private String description;
    private String device;
    private Integer status;
    private Integer isCurrent;
    /** theme.json 全文 */
    private String configJson;
    private Date createTime;

    /** 能力声明 supports.templates，用于切换预检 */
    private List<String> supportsTemplates = new ArrayList<>();
    /** 区域声明 regions（V2 可视化用） */
    private List<String> regions = new ArrayList<>();
    /** 可在后台模板选择器中选的自定义模板 */
    private List<CustomTemplate> customTemplates = new ArrayList<>();
    /** 主题级设计系统：调色板/字号/布局（对应 WP theme.json settings） */
    private ThemeSettings settings = new ThemeSettings();

    // /////////////////// 嵌套结构 ///////////////////

    /** 后台模板选择器项（theme.json customTemplates） */
    public static class CustomTemplate implements Serializable {
        private static final long serialVersionUID = 1L;
        private String file;
        private String name;
        private List<String> postTypes = new ArrayList<>();
        public String getFile() {
            return file;
        }
        public void setFile(String file) {
            this.file = file;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public List<String> getPostTypes() {
            return postTypes;
        }
        public void setPostTypes(List<String> postTypes) {
            this.postTypes = postTypes;
        }
    }

    /** 主题级设计系统（theme.json settings） */
    public static class ThemeSettings implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<PaletteColor> palette = new ArrayList<>();
        private List<FontSize> fontSizes = new ArrayList<>();
        private String contentSize;
        private String wideSize;
        public List<PaletteColor> getPalette() {
            return palette;
        }
        public void setPalette(List<PaletteColor> palette) {
            this.palette = palette;
        }
        public List<FontSize> getFontSizes() {
            return fontSizes;
        }
        public void setFontSizes(List<FontSize> fontSizes) {
            this.fontSizes = fontSizes;
        }
        public String getContentSize() {
            return contentSize;
        }
        public void setContentSize(String contentSize) {
            this.contentSize = contentSize;
        }
        public String getWideSize() {
            return wideSize;
        }
        public void setWideSize(String wideSize) {
            this.wideSize = wideSize;
        }
    }

    public static class PaletteColor implements Serializable {
        private static final long serialVersionUID = 1L;
        private String slug;
        private String color;
        private String name;
        public String getSlug() {
            return slug;
        }
        public void setSlug(String slug) {
            this.slug = slug;
        }
        public String getColor() {
            return color;
        }
        public void setColor(String color) {
            this.color = color;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    public static class FontSize implements Serializable {
        private static final long serialVersionUID = 1L;
        private String slug;
        private String size;
        public String getSlug() {
            return slug;
        }
        public void setSlug(String slug) {
            this.slug = slug;
        }
        public String getSize() {
            return size;
        }
        public void setSize(String size) {
            this.size = size;
        }
    }

    // /////////////////// getter/setter ///////////////////

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Integer isCurrent) {
        this.isCurrent = isCurrent;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public List<String> getSupportsTemplates() {
        return supportsTemplates;
    }

    public void setSupportsTemplates(List<String> supportsTemplates) {
        this.supportsTemplates = supportsTemplates;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public List<CustomTemplate> getCustomTemplates() {
        return customTemplates;
    }

    public void setCustomTemplates(List<CustomTemplate> customTemplates) {
        this.customTemplates = customTemplates;
    }

    public ThemeSettings getSettings() {
        return settings;
    }

    public void setSettings(ThemeSettings settings) {
        this.settings = settings;
    }
}
