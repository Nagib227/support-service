package ru.ithub.support.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Категории")
public class CategoryController {

  private final CategoryRepository categories;

  public CategoryController(CategoryRepository categories) {
    this.categories = categories;
  }

  @GetMapping
  @Operation(summary = "Справочник категорий")
  public List<CategoryResponse> list() {
    return categories.findAllByOrderByNameAsc().stream().map(CategoryResponse::of).toList();
  }
}
