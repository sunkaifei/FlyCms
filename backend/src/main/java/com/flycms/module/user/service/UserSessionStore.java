package com.flycms.module.user.service;

import com.flycms.module.user.dao.UserDao;
import com.flycms.module.user.model.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户会话存储（阶段 K5 / G5）。
 *
 * <p><b>存在的唯一理由</b>：拆掉 {@code UserService ↔ UserSessionUtils} 的循环依赖。
 * <ul>
 *   <li>改造前：{@code UserService} → {@code UserSessionUtils} → {@code UserService}（互相注入），
 *       项目不得不在 {@code application.yml} 打开 {@code spring.main.allow-circular-references}；</li>
 *   <li>改造后：会话的"读-写"由本类（**只依赖 {@link UserDao}**）承担，形成单向链
 *       {@code UserService → UserSessionUtils → UserSessionStore → UserDao}，
 *       循环消失，那个开关得以删除。</li>
 * </ul>
 *
 * <p>职责边界：只做 {@code fly_user_session} 的判存/新增/更新，不含登录态写入（那是
 * {@code UserSessionUtils} 的事），也不含用户主体逻辑（那是 {@code UserService} 的事）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class UserSessionStore {

    @Autowired
    private UserDao userDao;

    /**
     * 该用户是否已有会话记录
     */
    public boolean existsByUserId(Long userId) {
        return userDao.checkUserSessionByUserId(userId) > 0;
    }

    /**
     * 新增会话记录
     */
    public int save(UserSession userSession) {
        return userDao.addUserSession(userSession);
    }

    /**
     * 更新会话记录
     */
    public int update(UserSession userSession) {
        return userDao.updateUserSession(userSession);
    }
}
