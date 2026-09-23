package com.documind.domain.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank
        @Size(max = 100)
        String question
) {
}
