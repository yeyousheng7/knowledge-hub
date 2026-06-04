package com.yousheng.knowledgehub.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.category.dto.CategoryCreateRequest;
import com.yousheng.knowledgehub.category.dto.CategoryCreateResponse;
import com.yousheng.knowledgehub.category.dto.CategoryListItemResponse;
import com.yousheng.knowledgehub.category.dto.CategoryListResponse;
import com.yousheng.knowledgehub.category.entity.Category;
import com.yousheng.knowledgehub.category.mapper.CategoryMapper;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryService {
    private final AppUserMapper appUserMapper;
    private final CategoryMapper categoryMapper;
    private final int NOT_DELETED = 0;

    @Transactional
    public CategoryCreateResponse createCategory(CategoryCreateRequest request) {
        Long userId = requireCurrentEnabledUserId();
        String categoryName = request.name().trim();

        Category category = new Category();
        category.setName(categoryName);
        category.setUserId(userId);

        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        return new CategoryCreateResponse(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public CategoryListResponse listMyCategories() {
        Long userId = requireCurrentEnabledUserId();

        LambdaQueryWrapper<Category> queryByCategory = Wrappers.lambdaQuery(Category.class)
                .eq(Category::getUserId, userId)
                .eq(Category::getDeleted, NOT_DELETED)
                .orderByDesc(Category::getUpdatedAt)
                .orderByDesc(Category::getId);

        List<Category> categories = categoryMapper.selectList(queryByCategory);

        List<CategoryListItemResponse> items = categories
                .stream()
                .map(category -> {
                    return new CategoryListItemResponse(
                            category.getId(),
                            category.getName(),
                            category.getCreatedAt(),
                            category.getUpdatedAt()
                    );
                })
                .toList();
        return new CategoryListResponse(items);
    }

    private Long requireCurrentEnabledUserId() {
        Long userId = CurrentUser.getUserId();
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        return userId;
    }
}
