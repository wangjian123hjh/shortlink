package com.nageoffer.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.util.RandomGenerator;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {
    @Override
    public void saveGroup(ShortLinkGroupSaveReqDTO requestParam) {
        String gid ;
        while (true){
            gid = RandomGenerator.generateRandomString();
            if (hasGid(gid)){
                break;
            }
        }
        GroupDO build = GroupDO.builder()
                .gid(gid)
                .name(requestParam.getName())
                .delFlag(0)
                .build();
        baseMapper.insert(build);
    }
    private boolean hasGid(String gid){
        LambdaQueryWrapper<GroupDO> lambdaQuery = Wrappers.lambdaQuery(GroupDO.class);
        lambdaQuery.eq(GroupDO::getGid,gid)
                .eq(GroupDO::getUsername,null);
        GroupDO groupDO = baseMapper.selectOne(lambdaQuery);
        return groupDO==null;
    }
}
