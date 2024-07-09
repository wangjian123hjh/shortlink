package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.convention.Result;
import com.nageoffer.shortlink.admin.common.convention.Results;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupUpdateDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.util.RandomGenerator;
import org.springframework.stereotype.Service;

import java.util.List;

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
                .sortOrder(0)
                .username(UserContext.getUsername())
                .build();
        baseMapper.insert(build);
    }

    @Override
    public Result<List<ShortLinkGroupRespDTO>> listGroup() {
        String username = UserContext.getUsername();
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag,0)
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOS = baseMapper.selectList(queryWrapper);
        List<ShortLinkGroupRespDTO> respDTOS = BeanUtil.copyToList(groupDOS, ShortLinkGroupRespDTO.class);
        return Results.success(respDTOS);
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateDTO requestParam) {
        LambdaQueryWrapper<GroupDO> lambdaQuery = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername,UserContext.getUsername())
                .eq(GroupDO::getGid,requestParam.getGid())
                .eq(GroupDO::getDelFlag,0);
        GroupDO groupDO = new GroupDO();
        groupDO.setName(requestParam.getName());
        baseMapper.update(groupDO,lambdaQuery);
    }

    private boolean hasGid(String gid){
        LambdaQueryWrapper<GroupDO> lambdaQuery = Wrappers.lambdaQuery(GroupDO.class);
        lambdaQuery.eq(GroupDO::getGid,gid)
                .eq(GroupDO::getUsername,UserContext.getUsername());
        GroupDO groupDO = baseMapper.selectOne(lambdaQuery);
        return groupDO==null;
    }
}
