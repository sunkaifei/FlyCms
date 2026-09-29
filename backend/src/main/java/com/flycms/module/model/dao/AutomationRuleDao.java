package com.flycms.module.model.dao;

import com.flycms.module.model.model.AutomationRule;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 自动化规则 DAO（G17）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface AutomationRuleDao {

    List<AutomationRule> findAll();

    List<AutomationRule> findEnabledByEvent(@Param("event") String event);

    AutomationRule findById(@Param("id") Long id);

    int add(AutomationRule rule);

    int update(AutomationRule rule);

    int delete(@Param("id") Long id);
}
