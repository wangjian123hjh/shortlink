package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant;
import com.nageoffer.shortlink.admin.common.convention.Result;
import com.nageoffer.shortlink.admin.common.convention.Results;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupUpdateDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkCountQueryRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.util.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    private final RedissonClient redissonClient;
    private static ShortLinkRemoteService service = new ShortLinkRemoteService() {
    };
    @Override
    public void saveGroup(ShortLinkGroupSaveReqDTO requestParam) {
        RLock lock = redissonClient.getLock(RedisCacheConstant.LOCK_GROUP_CREATE_KEY+UserContext.getUsername());
        lock.lock();
        try {
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, UserContext.getUsername());
            List<GroupDO> groupDOS = baseMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(groupDOS) && groupDOS.size() == 20){
                throw new ClientException("已超出最大分组数");
            }
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
        } finally {
            lock.unlock();
        }

    }

    @Override
    public Result<List<ShortLinkGroupRespDTO>> listGroup() {
        String username = UserContext.getUsername();
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag,0)
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOS = baseMapper.selectList(queryWrapper);
        Result<List<ShortLinkCountQueryRespDTO>> listResult = service.listGroupShortLinkCount(groupDOS.stream().map(each -> each.getGid()).collect(Collectors.toList()));
        List<ShortLinkGroupRespDTO> respDTOS = BeanUtil.copyToList(groupDOS, ShortLinkGroupRespDTO.class);
        respDTOS.forEach(each ->{
            Optional<ShortLinkCountQueryRespDTO> first = listResult.getData().stream().filter(item -> ObjectUtil.equal(item.getGid(), each.getGid())).findFirst();
            first.ifPresent(item -> each.setShortLinkCount(item.getShortLinkCount()));
        });
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

    @Override
    public void deleteGroup(String gid) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername());
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO,queryWrapper);
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        requestParam.forEach(each ->{
            GroupDO groupDO = GroupDO.builder()
                    .sortOrder(each.getSortOrder())
                    .build();
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getGid, each.getGid())
                    .eq(GroupDO::getDelFlag, 0);
            baseMapper.update(groupDO,updateWrapper);
        });
    }

    private boolean hasGid(String gid){
        LambdaQueryWrapper<GroupDO> lambdaQuery = Wrappers.lambdaQuery(GroupDO.class);
        lambdaQuery.eq(GroupDO::getGid,gid)
                .eq(GroupDO::getUsername,UserContext.getUsername());
        GroupDO groupDO = baseMapper.selectOne(lambdaQuery);
        return groupDO==null;
    }
}
