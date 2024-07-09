package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.convention.Result;
import com.nageoffer.shortlink.admin.common.convention.Results;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
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
                .build();
        baseMapper.insert(build);
    }

    @Override
    public Result<List<ShortLinkGroupRespDTO>> listGroup() {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, "1")
                .eq(GroupDO::getDelFlag,0)
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOS = baseMapper.selectList(queryWrapper);
        List<ShortLinkGroupRespDTO> respDTOS = BeanUtil.copyToList(groupDOS, ShortLinkGroupRespDTO.class);
        return Results.success(respDTOS);
    }

    private boolean hasGid(String gid){
        LambdaQueryWrapper<GroupDO> lambdaQuery = Wrappers.lambdaQuery(GroupDO.class);
        lambdaQuery.eq(GroupDO::getGid,gid)
                .eq(GroupDO::getUsername,null);
        GroupDO groupDO = baseMapper.selectOne(lambdaQuery);
        return groupDO==null;
    }
}
