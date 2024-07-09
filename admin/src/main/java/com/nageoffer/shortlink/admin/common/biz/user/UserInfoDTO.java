package com.nageoffer.shortlink.admin.common.biz.user;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {
    @Alias("id")
    private String userId;
    private String username;
    private String realName;
}
