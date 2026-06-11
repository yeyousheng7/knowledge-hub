package com.yousheng.knowledgehub.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yousheng.knowledgehub.category.dto.*;
import com.yousheng.knowledgehub.category.entity.Category;
import com.yousheng.knowledgehub.category.mapper.CategoryMapper;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.note.entity.Note;
import com.yousheng.knowledgehub.note.mapper.NoteMapper;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryService {
    private final AppUserMapper appUserMapper;
    private final CategoryMapper categoryMapper;
    private static final int DELETED = 1;
    private static final int NOT_DELETED = 0;
    private final NoteMapper noteMapper;

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

    @Transactional
    public CategoryUpdateResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        Long userId = requireCurrentEnabledUserId();
        String newName = request.name().trim();

        LambdaUpdateWrapper<Category> updateWrapper = Wrappers.lambdaUpdate(Category.class)
                .eq(Category::getUserId, userId)
                .eq(Category::getId, categoryId)
                .eq(Category::getDeleted, NOT_DELETED)
                .set(Category::getName, newName);

        int affectedRows = 0;
        try {
            affectedRows = categoryMapper.update(new Category(), updateWrapper);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        if (affectedRows == 0) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        LambdaQueryWrapper<Category> queryByCategory = Wrappers.lambdaQuery(Category.class)
                .eq(Category::getUserId, userId)
                .eq(Category::getId, categoryId)
                .eq(Category::getDeleted, NOT_DELETED);
        Category category = categoryMapper.selectOne(queryByCategory);
        return new CategoryUpdateResponse(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Long userId = requireCurrentEnabledUserId();
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Category> deleteCategory = Wrappers.lambdaUpdate(Category.class)
                .eq(Category::getUserId, userId)
                .eq(Category::getId, categoryId)
                .eq(Category::getDeleted, NOT_DELETED)
                .set(Category::getDeleted, DELETED)
                .set(Category::getDeletedAt, now)
                .set(Category::getDeletedMarker, categoryId);

        int affectedRows = categoryMapper.update(new Category(), deleteCategory);
        if (affectedRows == 0) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 将已在 categoryId 下的 note 设置为未分类 - category = null 代表未分类
        LambdaUpdateWrapper<Note> updateNote = Wrappers.lambdaUpdate(Note.class)
                .eq(Note::getUserId, userId)
                .eq(Note::getCategoryId, categoryId)
                .eq(Note::getDeleted, NOT_DELETED)
                .set(Note::getCategoryId, null);

        noteMapper.update(new Note(), updateNote);
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
