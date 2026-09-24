package com.flycms.module.form.dao;

import com.flycms.module.form.model.Form;
import com.flycms.module.form.model.FormData;
import com.flycms.module.form.model.FormField;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 表单系统 DAO（规划阶段 F）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface FormDao {

    // ///////////// 表单 /////////////

    public int addForm(Form form);

    public int updateForm(Form form);

    public int deleteForm(@Param("id") Long id);

    public Form findFormById(@Param("id") Long id);

    public Form findFormByCode(@Param("formCode") String formCode);

    public int checkFormCode(@Param("formCode") String formCode, @Param("id") Long id);

    public List<Form> getFormList(@Param("formName") String formName,
                                  @Param("status") Integer status,
                                  @Param("offset") int offset,
                                  @Param("rows") int rows);

    public int getFormCount(@Param("formName") String formName, @Param("status") Integer status);

    // ///////////// 表单字段 /////////////

    public int addFormField(FormField field);

    public int updateFormField(FormField field);

    public int deleteFormField(@Param("id") Long id);

    public int deleteFormFieldByFormId(@Param("formId") Long formId);

    public FormField findFormFieldById(@Param("id") Long id);

    public List<FormField> getFormFieldList(@Param("formId") Long formId);

    // ///////////// 表单数据 /////////////

    public int addFormData(FormData data);

    public int deleteFormData(@Param("id") Long id);

    public int updateFormDataStatus(@Param("id") Long id, @Param("status") Integer status);

    public FormData findFormDataById(@Param("id") Long id);

    /**
     * 表单数据分页列表（join 表单名与编码，便于后台直接展示来源）
     */
    public List<FormData> getFormDataList(@Param("formId") Long formId,
                                          @Param("status") Integer status,
                                          @Param("createTime") String createTime,
                                          @Param("offset") int offset,
                                          @Param("rows") int rows);

    public int getFormDataCount(@Param("formId") Long formId,
                                @Param("status") Integer status,
                                @Param("createTime") String createTime);

    /**
     * 频控：统计同一 IP（或用户）在指定日期之后的提交次数
     */
    public int countSubmitByIp(@Param("formId") Long formId, @Param("ip") String ip, @Param("since") String since);

    public int countSubmitByUser(@Param("formId") Long formId, @Param("userId") Long userId, @Param("since") String since);
}
