package com.flycms.module.dict.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.dict.dao.DictDataDao;
import com.flycms.module.dict.dao.DictTypeDao;
import com.flycms.module.dict.model.DictData;
import com.flycms.module.dict.model.DictType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 数据字典服务：类型/数据 CRUD + 唯一性校验 + 表单候选项读取。
 *
 * <p>绑定语义（若依式）：fly_model_field.dict_type 指向本服务的类型键，
 * select/radio/checkbox 表单候选项优先取字典启用数据，field.options 作回退。
 * dict_type 键创建后不可改（同字段名不可改的约束，避免字段绑定悬空）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class DictService {

    /** dict_type 键格式：小写字母开头，小写字母/数字/下划线（与字段名同规） */
    private static final String DICT_TYPE_PATTERN = "^[a-z][a-z0-9_]{0,63}$";

    @Autowired
    private DictTypeDao dictTypeDao;
    @Autowired
    private DictDataDao dictDataDao;

    // /////////////////// 字典类型 ///////////////////

    public PageVo<DictType> findTypePage(int pageNum, int rows, String keyword) {
        PageVo<DictType> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(dictTypeDao.findDictTypes(pageVo.getOffset(), rows, keyword));
        pageVo.setCount(dictTypeDao.countDictTypes(keyword));
        return pageVo;
    }

    public List<DictType> findEnabledTypes() {
        return dictTypeDao.findEnabledDictTypes();
    }

    public DictType findTypeById(Long id) {
        return dictTypeDao.findDictTypeById(id);
    }

    public DictType findTypeByKey(String dictType) {
        return dictTypeDao.findDictTypeByKey(dictType);
    }

    public DataVo addType(DictType form) {
        if (StringUtils.isBlank(form.getDictName())) {
            return DataVo.failure("字典名称不能为空");
        }
        String key = StringUtils.trimToEmpty(form.getDictType());
        if (StringUtils.isBlank(key)) {
            return DataVo.failure("字典类型键不能为空");
        }
        if (!key.matches(DICT_TYPE_PATTERN)) {
            return DataVo.failure("字典类型键格式不正确（小写字母开头，仅小写字母/数字/下划线）");
        }
        if (dictTypeDao.checkDictTypeKey(key, null) > 0) {
            return DataVo.failure("字典类型键已存在：" + key);
        }
        form.setDictType(key);
        form.setId(SnowFlake.getInstance().nextId());
        form.setStatus(form.getStatus() == 0 ? 0 : 1);
        form.setCreateTime(new Date());
        if (dictTypeDao.addDictType(form) > 0) {
            return DataVo.success("字典类型已添加", DataVo.NOOP);
        }
        return DataVo.failure("操作失败");
    }

    public DataVo updateType(DictType form) {
        if (form.getId() == null || form.getId() <= 0) {
            return DataVo.failure("字典类型 id 不能为空");
        }
        DictType old = dictTypeDao.findDictTypeById(form.getId());
        if (old == null) {
            return DataVo.failure("字典类型不存在");
        }
        if (StringUtils.isBlank(form.getDictName())) {
            return DataVo.failure("字典名称不能为空");
        }
        // 类型键不可改：字段绑定按键存储，改键会使既有绑定悬空
        form.setDictType(old.getDictType());
        form.setStatus(form.getStatus() == 0 ? 0 : 1);
        if (dictTypeDao.updateDictType(form) > 0) {
            return DataVo.success("字典类型已更新", DataVo.NOOP);
        }
        return DataVo.failure("操作失败");
    }

    /** 删除类型：级联删数据。被模型字段引用时拒绝（先解除字段绑定）。 */
    public DataVo deleteType(Long id, int bindCount) {
        DictType old = dictTypeDao.findDictTypeById(id);
        if (old == null) {
            return DataVo.failure("字典类型不存在");
        }
        if (bindCount > 0) {
            return DataVo.failure("该字典被 " + bindCount + " 个模型字段引用，请先在字段管理里解除绑定");
        }
        dictDataDao.deleteDictDataByType(old.getDictType());
        dictTypeDao.deleteDictTypeById(id);
        return DataVo.success("字典类型及其数据已删除", DataVo.NOOP);
    }

    // /////////////////// 字典数据 ///////////////////

    public PageVo<DictData> findDataPage(int pageNum, int rows, String dictType, String keyword) {
        PageVo<DictData> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(dictDataDao.findDictDatas(dictType, keyword, pageVo.getOffset(), rows));
        pageVo.setCount(dictDataDao.countDictDatas(dictType, keyword));
        return pageVo;
    }

    /** 表单候选项：某类型的启用数据（sort, id 排序） */
    public List<DictData> findEnabledData(String dictType) {
        return dictDataDao.findEnabledByType(dictType);
    }

    public DataVo addData(DictData form) {
        if (StringUtils.isBlank(form.getDictType())) {
            return DataVo.failure("所属字典类型不能为空");
        }
        if (dictTypeDao.findDictTypeByKey(form.getDictType()) == null) {
            return DataVo.failure("字典类型不存在：" + form.getDictType());
        }
        if (StringUtils.isBlank(form.getDictLabel())) {
            return DataVo.failure("数据标签不能为空");
        }
        String value = StringUtils.trimToEmpty(form.getDictValue());
        if (StringUtils.isBlank(value)) {
            return DataVo.failure("数据键值不能为空");
        }
        if (dictDataDao.checkDictValue(form.getDictType(), value, null) > 0) {
            return DataVo.failure("该键值在此字典下已存在：" + value);
        }
        form.setDictValue(value);
        form.setId(SnowFlake.getInstance().nextId());
        form.setStatus(form.getStatus() == 0 ? 0 : 1);
        form.setCreateTime(new Date());
        if (dictDataDao.addDictData(form) > 0) {
            return DataVo.success("字典数据已添加", DataVo.NOOP);
        }
        return DataVo.failure("操作失败");
    }

    public DataVo updateData(DictData form) {
        if (form.getId() == null || form.getId() <= 0) {
            return DataVo.failure("字典数据 id 不能为空");
        }
        DictData old = dictDataDao.findDictDataById(form.getId());
        if (old == null) {
            return DataVo.failure("字典数据不存在");
        }
        if (StringUtils.isBlank(form.getDictLabel())) {
            return DataVo.failure("数据标签不能为空");
        }
        String value = StringUtils.trimToEmpty(form.getDictValue());
        if (StringUtils.isBlank(value)) {
            return DataVo.failure("数据键值不能为空");
        }
        if (dictDataDao.checkDictValue(old.getDictType(), value, form.getId()) > 0) {
            return DataVo.failure("该键值在此字典下已存在：" + value);
        }
        form.setDictValue(value);
        form.setStatus(form.getStatus() == 0 ? 0 : 1);
        if (dictDataDao.updateDictData(form) > 0) {
            return DataVo.success("字典数据已更新", DataVo.NOOP);
        }
        return DataVo.failure("操作失败");
    }

    public DataVo deleteData(Long id) {
        if (dictDataDao.findDictDataById(id) == null) {
            return DataVo.failure("字典数据不存在");
        }
        dictDataDao.deleteDictDataById(id);
        return DataVo.success("字典数据已删除", DataVo.NOOP);
    }
}
