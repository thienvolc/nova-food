package com.nova.food.app.service;

import com.nova.food.app.dto.response.Meta;
import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.domain.common.constant.ResponseCode;
import org.springframework.stereotype.Service;

@Service
public class ResponseFactory {

    private final String appName = "nova-food";

    public ResponseDto success(ResponseCode responseCode) {
        var meta = Meta.builder()
                .serviceId(appName)
                .status(responseCode.getCode())
                .message(responseCode.getDefaultMessage())
                .build();
        return new ResponseDto(meta, null);
    }

    public ResponseDto success(Object data) {
        var meta = Meta.builder()
                .serviceId(appName)
                .status(ResponseCode.SUCCESS.getCode())
                .build();
        return new ResponseDto(meta, data);
    }
}
