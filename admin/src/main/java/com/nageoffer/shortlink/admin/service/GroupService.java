package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;

public interface GroupService extends IService<GroupDO> {
    void saveGroup(ShortLinkGroupSaveReqDTO requestParam);
}
