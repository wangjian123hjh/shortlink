package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.admin.dao.entity.UserDO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;

public interface UserService extends IService<UserDO> {
    // 根据用户名查询用户信息
    UserRespDTO getUserByUsername(String username);
}
