package com.techora.api.rest;

import com.techora.app.dto.response.ResponseDto;
import com.techora.app.service.ResponseFactory;
import com.techora.domain.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;
    private final ResponseFactory responseFactory;

    @GetMapping
    public ResponseDto getAll() {
        return responseFactory.success(categoryService.getActiveCategories());
    }
}
