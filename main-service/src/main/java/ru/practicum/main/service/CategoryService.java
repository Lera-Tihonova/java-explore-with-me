package ru.practicum.main.service;

import ru.practicum.main.dto.CategoryDto;
import ru.practicum.main.dto.NewCategoryDto;
import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto request);

    CategoryDto updateCategory(Long catId, CategoryDto request);

    void deleteCategory(Long catId);

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);
}