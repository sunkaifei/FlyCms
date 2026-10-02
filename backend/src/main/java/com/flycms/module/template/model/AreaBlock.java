package com.flycms.module.template.model;

import java.io.Serializable;

/**
 * 区域区块编排实体（规划 §12.1 / §8.4 V2 / P10）。
 *
 * <p>把"布局"从模板代码变成数据：模板用 {@code <@fly_area name="content_top"/>} 声明
 * "这里可以放东西"，后台「布局管理」决定"放什么、什么顺序"。
 * 存的是<b>绑定关系</b>（碎片 key / 标签代码 / HTML），不存模板正文——与 D17 一致。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public class AreaBlock implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 区块类型：引用碎片（block_ref = fly_block.block_key） */
    public static final String TYPE_BLOCK = "BLOCK";
    /** 区块类型：标签代码片段（block_ref 为一段 FreeMarker，如 {@code <@fly_list_model .../>}） */
    public static final String TYPE_TAG = "TAG";
    /** 区块类型：自定义 HTML（原样输出） */
    public static final String TYPE_HTML = "HTML";

    public String getWrapperClass() {
        return wrapperClass;
    }

    public void setWrapperClass(String wrapperClass) {
        this.wrapperClass = wrapperClass;
    }

    private Long id;
    private String themeCode;
    private String areaName;
    private String blockType;
    private String blockTitle;
    private String blockRef;
    /** S1-a 区块属性：包装器 CSS 类（渲染时附加到区块外层 div，如 "card shadow-lg"） */
    private String wrapperClass;
    private Integer sort;
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getThemeCode() {
        return themeCode;
    }

    public void setThemeCode(String themeCode) {
        this.themeCode = themeCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public String getBlockTitle() {
        return blockTitle;
    }

    public void setBlockTitle(String blockTitle) {
        this.blockTitle = blockTitle;
    }

    public String getBlockRef() {
        return blockRef;
    }

    public void setBlockRef(String blockRef) {
        this.blockRef = blockRef;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
