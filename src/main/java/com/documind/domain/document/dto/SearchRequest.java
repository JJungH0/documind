package com.documind.domain.document.dto;

import jakarta.validation.constraints.*;

public record SearchRequest (
        @NotBlank
        @Size(max = 1000)
        String query,

        @NotNull
        @Min(1)
        @Max(20)
        Integer topK
){
}
