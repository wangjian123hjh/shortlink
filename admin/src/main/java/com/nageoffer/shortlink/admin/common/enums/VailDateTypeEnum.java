package com.nageoffer.shortlink.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum VailDateTypeEnum {
    PERMANENT(0),
    CUSTOM(1);
    @Getter
    private final int type;
}
