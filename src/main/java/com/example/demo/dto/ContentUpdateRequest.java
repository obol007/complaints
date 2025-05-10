package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContentUpdateRequest(@NotBlank @NotNull String content) {
}