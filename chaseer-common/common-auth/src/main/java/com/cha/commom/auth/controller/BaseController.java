package com.cha.commom.auth.controller;

import cn.hutool.core.date.DatePattern;
import com.adkfp.common.auth.constant.AuthConstant;
import com.adkfp.common.auth.dto.AuthUser;
import com.adkfp.common.auth.util.AuthUtil;
import com.adkfp.common.core.constant.BaseConstant;
import com.adkfp.common.core.context.DataPermissionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author songChaoHua
 * @Description web层通用数据处理
 * @date 2023/3/2 14:56
 */
@Slf4j
public class BaseController {
    /**
     * 将前台传递过来的日期格式的字符串，自动转化为Date类型
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Date 类型转换
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalDate.parse(text, DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN)));
            }
        });
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalDateTime.parse(text, DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)));
            }
        });
        binder.registerCustomEditor(LocalTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalTime.parse(text, DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
            }
        });
    }

    public long getTenancyId() {
        Long tenancyId;
        try {
            tenancyId = AuthUtil.getCurrTenancyId();
        } catch (Exception e) {
            return BaseConstant.INT_STATUS_1;
        }
        if (tenancyId == null) {
            return BaseConstant.INT_STATUS_1;
        }
        return tenancyId;
    }

    public String getDataPermissionType() {
        try {
            AuthUser currUser = AuthUtil.getCurrUser();
            if (currUser == null) {
                return null;
            }
            if (!DataPermissionContext.isEnableDataPermission()) {
                return null;
            }
            List<String> roleCodes = currUser.getRoleCodes();
            //管理员直接放行
            if (roleCodes.contains(AuthConstant.ROLE_SUPER_ADMIN)
                    || roleCodes.contains(AuthConstant.ROLE_TENANCY_ADMIN)) {
                return null;
            }
            return currUser.getDataPermissionType();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getRoleList() {
        return AuthUtil.getCurrUser().getRoleCodes();
    }

}
