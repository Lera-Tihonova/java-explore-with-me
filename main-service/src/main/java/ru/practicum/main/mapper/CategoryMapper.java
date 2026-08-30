package ru.practicum.main.mapper;

import ru.practicum.main.dto.CategoryDto;
import ru.practicum.main.dto.NewCategoryDto;
import ru.practicum.main.model.Category;

public class CategoryMapper {

    public static Category toEntity(NewCategoryDto request) {
        return Category.builder()
                .name(request.getName())
                .build();
    }

    public static CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}