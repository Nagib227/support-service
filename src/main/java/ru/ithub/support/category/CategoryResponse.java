package ru.ithub.support.category;

import java.util.UUID;

public record CategoryResponse(UUID id, String code, String name) {

  public static CategoryResponse of(Category category) {
    return new CategoryResponse(category.getId(), category.getCode(), category.getName());
  }
}
