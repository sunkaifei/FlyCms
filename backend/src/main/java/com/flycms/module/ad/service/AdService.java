package com.flycms.module.ad.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.ad.dao.AdDao;
import com.flycms.module.ad.dao.AdPositionDao;
import com.flycms.module.ad.model.Ad;
import com.flycms.module.ad.model.AdPosition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 广告服务：广告位/广告 CRUD + 前台投放读取（时间窗过滤、权重排序）+ 展示/点击计数。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class AdService {

    /** 合法广告类型 */
    private static final List<String> AD_TYPES = List.of("image", "text", "code");
    /** ad_key 格式：小写字母开头，小写字母/数字/下划线（模板调用标识） */
    private static final String AD_KEY_PATTERN = "^[a-z][a-z0-9_]{0,63}$";

    @Autowired
    private AdPositionDao adPositionDao;
    @Autowired
    private AdDao adDao;
    @Autowired
    private com.flycms.module.ad.dao.AdStatDao adStatDao;

    // /////////////////// 广告位 ///////////////////

    public PageVo<AdPosition> findPositionPage(int pageNum, int rows, String keyword) {
        PageVo<AdPosition> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(adPositionDao.findPositions(pageVo.getOffset(), rows, keyword));
        pageVo.setCount(adPositionDao.countPositions(keyword));
        return pageVo;
    }

    public AdPosition findPositionById(Long id) {
        return adPositionDao.findPositionById(id);
    }

    public DataVo addPosition(AdPosition form) {
        if (StringUtils.isBlank(form.getName())) {
            return DataVo.failure("广告位名称不能为空");
        }
        String key = StringUtils.trimToEmpty(form.getAdKey());
        if (StringUtils.isBlank(key)) {
            return DataVo.failure("调用标识不能为空");
        }
        if (!key.matches(AD_KEY_PATTERN)) {
            return DataVo.failure("调用标识格式不正确（小写字母开头，仅小写字母/数字/下划线）");
        }
        if (adPositionDao.checkAdKey(key, null) > 0) {
            return DataVo.failure("调用标识已存在：" + key);
        }
        form.setAdKey(key);
        form.setId(SnowFlake.getInstance().nextId());
        form.setStatus(form.getStatus() == 0 ? 0 : 1);
        form.setCreateTime(new Date());
        if (adPositionDao.addPosition(form) > 0) {
            return DataVo.success("广告位已添加", DataVo.NOOP);
        }
        return DataVo.failure("操作失败");
    }

    public DataVo updatePosition(AdPosition form) {
        if (form.getId() == null || form.getId() <= 0) {
            return DataVo.failure("广告位 id 不能为空");
        }
        AdPosition old = adPositionDao.findPositionById(form.getId());
        if (old == null) {
            return DataVo.failure("广告位不存在");
        }
        if (StringUtils.isBlank(form.getName())) {
            return DataVo.failure("广告位名称不能为空");
        }
        // ad_key 不可改：模板按 key 调用，改键会让模板调用悬空
        form.setAdKey(old.getAdKey());
        form.setStatus(form.getStatus() == 0 ? 0 : 1);
        if (adPositionDao.updatePosition(form) > 0) {
            return DataVo.success("广告位已更新", DataVo.NOOP);
        }
        return DataVo.failure("操作失败");
    }

    /** 删除广告位：位下有广告时拒绝（先清空广告） */
    public DataVo deletePosition(Long id) {
        AdPosition old = adPositionDao.findPositionById(id);
        if (old == null) {
            return DataVo.failure("广告位不存在");
        }
        int adCount = adPositionDao.countAdsByPosition(id);
        if (adCount > 0) {
            return DataVo.failure("该广告位下还有 " + adCount + " 个广告，请先删除广告");
        }
        adPositionDao.deletePositionById(id);
        return DataVo.success("广告位已删除", DataVo.NOOP);
    }

    // /////////////////// 广告 ///////////////////

    public PageVo<Ad> findAdPage(int pageNum, int rows, Long positionId, String keyword) {
        PageVo<Ad> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(adDao.findAds(positionId, keyword, pageVo.getOffset(), rows));
        pageVo.setCount(adDao.countAds(positionId, keyword));
        return pageVo;
    }

    public Ad findAdById(Long id) {
        return adDao.findAdById(id);
    }

    public DataVo addAd(Ad form) {
        DataVo check = validateAd(form);
        if (check.getCode() != DataVo.CODE_SUCCESS) {
            return check;
        }
        if (adPositionDao.findPositionById(form.getPositionId()) == null) {
            return DataVo.failure("所属广告位不存在");
        }
        form.setId(SnowFlake.getInstance().nextId());
        form.setStatus(form.getStatus() == 0 ? 0 : 1);
        form.setCreateTime(new Date());
        if (adDao.addAd(form) > 0) {
            return DataVo.success("广告已添加", DataVo.NOOP);
        }
        return DataVo.failure("操作失败");
    }

    public DataVo updateAd(Ad form) {
        if (form.getId() == null || form.getId() <= 0) {
            return DataVo.failure("广告 id 不能为空");
        }
        Ad old = adDao.findAdById(form.getId());
        if (old == null) {
            return DataVo.failure("广告不存在");
        }
        DataVo check = validateAd(form);
        if (check.getCode() != DataVo.CODE_SUCCESS) {
            return check;
        }
        // 所属广告位不可改（换位=删除重建，避免统计口径混乱）
        form.setPositionId(old.getPositionId());
        form.setStatus(form.getStatus() == 0 ? 0 : 1);
        if (adDao.updateAd(form) > 0) {
            return DataVo.success("广告已更新", DataVo.NOOP);
        }
        return DataVo.failure("操作失败");
    }

    public DataVo deleteAd(Long id) {
        if (adDao.findAdById(id) == null) {
            return DataVo.failure("广告不存在");
        }
        adDao.deleteAdById(id);
        return DataVo.success("广告已删除", DataVo.NOOP);
    }

    /** 类型相关素材校验：image 必填 image_url，text 必填 text_content，code 必填 html_code */
    private DataVo validateAd(Ad form) {
        if (form.getPositionId() == null) {
            return DataVo.failure("所属广告位不能为空");
        }
        if (StringUtils.isBlank(form.getName())) {
            return DataVo.failure("广告名称不能为空");
        }
        String type = StringUtils.trimToEmpty(form.getAdType());
        if (!AD_TYPES.contains(type)) {
            return DataVo.failure("广告类型只允许 image/text/code");
        }
        form.setAdType(type);
        if ("image".equals(type) && StringUtils.isBlank(form.getImageUrl())) {
            return DataVo.failure("图片广告必须上传或填写图片地址");
        }
        if ("text".equals(type) && StringUtils.isBlank(form.getTextContent())) {
            return DataVo.failure("文字广告必须填写文字内容");
        }
        if ("code".equals(type) && StringUtils.isBlank(form.getHtmlCode())) {
            return DataVo.failure("代码广告必须填写代码");
        }
        if (form.getWeight() == null) {
            form.setWeight(1);
        }
        if (form.getStartTime() != null && form.getEndTime() != null
                && form.getEndTime().before(form.getStartTime())) {
            return DataVo.failure("结束时间不能早于开始时间");
        }
        return DataVo.success("ok", DataVo.NOOP);
    }

    // /////////////////// 前台投放 ///////////////////

    public AdPosition findPositionByKey(String adKey) {
        return adPositionDao.findPositionByKey(adKey);
    }

    /** 前台投放列表：启用 + 时间窗内，权重优先 */
    public List<Ad> listActiveAds(Long positionId, int rows) {
        return adDao.listActiveByPosition(positionId, rows);
    }

    /** 展示计数 +1（标签渲染/JS 分发时批量）：累计总数 + 按日明细双写 */
    public void incrementViews(Long positionId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        adDao.incrementView(ids);
        for (Long adId : ids) {
            adStatDao.upsertView(SnowFlake.getInstance().nextId(), adId, positionId);
        }
    }

    /** 点击计数 +1（/ad/click/{id}）：累计总数 + 按日明细双写 */
    public void incrementClick(Ad ad) {
        adDao.incrementClick(ad.getId());
        adStatDao.upsertClick(SnowFlake.getInstance().nextId(), ad.getId(), ad.getPositionId());
    }

    /** 按日统计（adId/positionId 可选过滤），返回 [{statDate, views, clicks}] */
    public List<java.util.Map<String, Object>> listDailyStat(Long adId, Long positionId, int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        return adStatDao.listDaily(adId, positionId, safeDays);
    }

    /**
     * 缺省 HTML（标签直出与 JS 分发共用）：图片/文字带点击计数跳转，代码原样输出。
     */
    public String renderDefaultHtml(java.util.List<Ad> ads) {
        StringBuilder html = new StringBuilder();
        if (ads == null) {
            return "";
        }
        for (Ad ad : ads) {
            String type = ad.getAdType();
            String clickUrl = "/ad/click/" + ad.getId();
            String linkUrl = ad.getUrl();
            String target = StringUtils.isNotBlank(linkUrl) ? " target=\"_blank\"" : "";
            String href = StringUtils.isNotBlank(linkUrl) ? clickUrl : "javascript:void(0)";
            if ("image".equals(type)) {
                html.append("<a href=\"").append(href).append("\"").append(target)
                        .append(" title=\"").append(ad.getName()).append("\">")
                        .append("<img src=\"").append(ad.getImageUrl()).append("\" alt=\"")
                        .append(ad.getName()).append("\" style=\"border:0;\"/></a>");
            } else if ("text".equals(type)) {
                html.append("<a href=\"").append(href).append("\"").append(target).append(">")
                        .append(ad.getTextContent()).append("</a>");
            } else if (ad.getHtmlCode() != null) {
                html.append(ad.getHtmlCode());
            }
        }
        return html.toString();
    }

    /**
     * JS 调用代码体（联盟式分发，/ad/js/{adKey} 输出）：document.write 缺省 HTML。
     * HTML 进入 JS 字符串字面量前转义 反斜杠/单引号/换行。
     */
    public String renderJs(java.util.List<Ad> ads) {
        String literal = renderDefaultHtml(ads)
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n");
        return "document.write('" + literal + "');";
    }
}
