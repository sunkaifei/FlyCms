package com.flycms.module.favorite.service;

import com.flycms.core.utils.OrderbyUtils;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.favorite.dao.FavoriteDao;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.favorite.model.Favorite;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;
import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:06 2018/9/6
 */
@Service
public class FavoriteService {

    /**
     * 收藏列表允许的排序列白名单（= fly_favorite 的实际列）。
     * 原默认片段 "a.score" 引用了该表不存在的别名与列（getFavoriteList 是
     * {@code select * from fly_favorite}），会直接 SQL 报错，故一并修正为 id。
     */
    private static final java.util.Set<String> SORT_COLUMNS =
            OrderbyUtils.columns("id", "user_id", "info_type", "info_id", "model_code", "create_time");

    @Resource
    private FavoriteDao favoriteDao;
    @Autowired
    protected ModelDataService modelDataService;
    @Autowired
    protected ModelService modelService;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //用户添加信息收藏
    @Transactional
    public DataVo addFavorite(Long userId,Integer infoType,Long infoId){
        DataVo data = DataVo.failure("操作失败");
        if(infoType==1){
            // U3：收藏目标校验改走自定义模型通道（infoType 1 = articles 模型发布态内容）
            com.flycms.module.model.model.Model articleModel =
                    modelService.findModelByCode("articles");
            java.util.Map<String, Object> row = articleModel == null ? null
                    : modelDataService.findDataById(articleModel.getId(), infoId);
            if (row == null || !"1".equals(String.valueOf(row.get("status")))) {
                return data=DataVo.failure("您收藏的信息不存在！");
            }
        }else{
            data = DataVo.failure("信息类型不存在！");
        }
        if(this.checkFavoriteByUser(userId,infoType,infoId)){
            data = DataVo.failure("已成功收藏！");
        }else{
            Favorite favorite=new Favorite();
            SnowFlake snowFlake = SnowFlake.getInstance();
            favorite.setId(snowFlake.nextId());
            favorite.setUserId(userId);
            favorite.setInfoType(infoType);
            favorite.setInfoId(infoId);
            favorite.setCreateTime(new Date());
            favoriteDao.addFavorite(favorite);
            data = DataVo.success("已添加收藏！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////



    // ///////////////////////////////
    // /////   E7 收藏平台化   ////////
    // ///////////////////////////////

    /**
     * 收藏任意模型内容（E7 平台化）：目标必须存在且已发布。
     * 幂等：重复收藏返回提示而不报错。
     */
    @Transactional
    public DataVo addFavorite(Long userId, String modelCode, Long infoId) {
        DataVo data = DataVo.failure("操作失败");
        final String code;
        try {
            code = com.flycms.core.utils.SqlSafeUtil.safeModelCode(modelCode);
        } catch (IllegalArgumentException e) {
            return DataVo.failure("模型标识不合法");
        }
        com.flycms.module.model.model.Model model = modelService.findModelByCode(code);
        if (model == null) {
            return DataVo.failure("目标模型不存在");
        }
        java.util.Map<String, Object> row = modelDataService.findDataById(model.getId(), infoId);
        if (row == null || !"1".equals(String.valueOf(row.get("status")))) {
            return DataVo.failure("您收藏的信息不存在！");
        }
        // 幂等：同用户同目标（跨模型用 model_code 区分）只收藏一次
        if (favoriteDao.checkByUserAndTarget(userId, code, infoId) > 0) {
            return DataVo.failure("已成功收藏！");
        }
        Favorite favorite = new Favorite();
        SnowFlake snowFlake = SnowFlake.getInstance();
        favorite.setId(snowFlake.nextId());
        favorite.setUserId(userId);
        favorite.setInfoType(1);
        favorite.setInfoId(infoId);
        favorite.setModelCode(code);
        favorite.setCreateTime(new Date());
        favoriteDao.addFavorite(favorite);
        return DataVo.success("已添加收藏！");
    }

    /** 取消收藏（平台化：按模型 code + 内容 id） */
    @Transactional
    public DataVo removeFavorite(Long userId, String modelCode, Long infoId) {
        try {
            com.flycms.core.utils.SqlSafeUtil.safeModelCode(modelCode);
        } catch (IllegalArgumentException e) {
            return DataVo.failure("模型标识不合法");
        }
        int n = favoriteDao.deleteByUserAndTarget(userId, modelCode, infoId);
        return n > 0 ? DataVo.success("已取消收藏") : DataVo.failure("收藏不存在");
    }

    /** 收藏是否存在（平台化） */
    public boolean checkFavorite(Long userId, String modelCode, Long infoId) {
        return favoriteDao.checkByUserAndTarget(userId, modelCode, infoId) > 0;
    }

    /**
     * 收藏列表（平台化，带内容回表）：返回行含 modelCode/infoId/title/url/createTime，
     * title/url 按各自模型解析（legacy 无 model_code 行视作 articles）。
     */
    public List<Map<String, Object>> listFavoriteRows(Long userId, String modelCode,
                                                      int page, int rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        String mcFilter = StringUtils.trimToNull(modelCode);
        int offset = (Math.max(page, 1) - 1) * Math.max(rows, 1);
        List<Favorite> favs = favoriteDao.listByUser(userId, mcFilter, offset, rows);
        // 按模型分组回表
        Map<String, com.flycms.module.model.model.Model> modelById = new HashMap<>();
        List<Map<String, Object>> pending = new ArrayList<>();
        for (Favorite f : favs) {
            String code = f.getModelCode() == null ? "articles" : f.getModelCode();
            com.flycms.module.model.model.Model m = modelById.computeIfAbsent(code,
                    k -> modelService.findModelByCode(k));
            if (m == null) {
                continue;
            }
            Map<String, Object> contentRow = modelDataService.findDataById(m.getId(), f.getInfoId());
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("modelCode", code);
            o.put("modelName", m.getName());
            o.put("infoId", String.valueOf(f.getInfoId()));
            o.put("createTime", f.getCreateTime());
            if (contentRow == null) {
                o.put("title", "（内容已删除）");
                o.put("url", "");
                o.put("alive", false);
            } else {
                o.put("title", contentRow.getOrDefault("title", ""));
                o.put("url", "/" + code + "/" + contentRow.get("short_url") + ".html");
                o.put("alive", true);
            }
            out.add(o);
        }
        return out;
    }

    public int countFavoriteRows(Long userId, String modelCode) {
        return favoriteDao.countByUser(userId, StringUtils.trimToNull(modelCode));
    }

    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    /**
     * 查询收藏信息是否存在
     *
     * @param userId
     *         用户id
     * @param infoType
     *         信息类型id
     * @param infoId
     *         收藏信息id
     * @return
     */
    public boolean checkFavoriteByUser(Long userId,Integer infoType,Long infoId) {
        int totalCount = favoriteDao.checkFavoriteByUser(userId,infoType,infoId);
        return totalCount > 0 ? true : false;
    }

    /**
     *  收藏翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Favorite> getFavoriteListPage(Long userId,Integer infoType, String createTime, String orderby, String order, int pageNum, int rows) {
        PageVo<Favorite> pageVo = new PageVo<Favorite>(pageNum);
        pageVo.setRows(rows);
        List<Favorite> list = new ArrayList<Favorite>();
        if(orderby==null){
            orderby="id";
            }
        orderby = OrderbyUtils.check(orderby, "id", SORT_COLUMNS);
        if(order==null){
            order="desc";
            }
        order = OrderbyUtils.direction(order, "desc");
        pageVo.setList(favoriteDao.getFavoriteList(userId, infoType,createTime,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(favoriteDao.getFavoriteCount(userId, infoType,createTime));
        return pageVo;
    }
}
