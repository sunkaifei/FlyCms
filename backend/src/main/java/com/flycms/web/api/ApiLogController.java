package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.adminlog.dao.AdminLogDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 审计日志查询（规划阶段 A4）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiLogController extends ApiBaseController {

    @Autowired
    private AdminLogDao adminLogDao;

    @ResponseBody
    @GetMapping("/system/log/list")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "adminName", required = false) String adminName,
                       @RequestParam(value = "path", required = false) String path,
                       @RequestParam(value = "startTime", required = false) String startTime,
                       @RequestParam(value = "endTime", required = false) String endTime) {
        requirePermission("/api/system/log/list");
        @SuppressWarnings({"rawtypes", "unchecked"})
        PageVo pageVo = new PageVo<>(pageNum);
        pageVo.setRows(20);
        pageVo.setList(adminLogDao.getLogPage(
                StringUtils.trimToNull(adminName), StringUtils.trimToNull(path),
                StringUtils.trimToNull(startTime), StringUtils.trimToNull(endTime),
                pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(adminLogDao.getLogCount(
                StringUtils.trimToNull(adminName), StringUtils.trimToNull(path),
                StringUtils.trimToNull(startTime), StringUtils.trimToNull(endTime)));
        return DataVo.success("操作成功", pageVo);
    }
}
