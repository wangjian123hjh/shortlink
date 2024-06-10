package com.nageoffer.shortlink.admin.common.convention.exception;

import com.nageoffer.shortlink.admin.common.convention.errorcode.IErrorCode;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Optional;
@Data
public class AbstractException extends RuntimeException{
    public final String errorCode;

    public final String errorMessage;

    public AbstractException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable);
        this.errorCode = errorCode.code();
        this.errorMessage = Optional.ofNullable(StringUtils.hasLength(message) ? message : null).orElse(errorCode.message());
    }
}
