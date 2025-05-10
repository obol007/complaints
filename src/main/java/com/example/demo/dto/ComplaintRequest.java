package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplaintRequest(@NotBlank @NotNull String productId,
                               @NotBlank @NotNull String content,
                               @NotBlank @NotNull String reporter
) {
}