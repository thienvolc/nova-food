package com.nova.food.domain.common.service;

import com.nova.food.app.aop.BusinessException;
import com.nova.food.domain.common.constant.ResponseCode;
import org.springframework.stereotype.Service;

@Service
public class PageValidator {

    public void validate(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessException(ResponseCode.INVALID_PAGE_REQUEST);
        }
    }
}
