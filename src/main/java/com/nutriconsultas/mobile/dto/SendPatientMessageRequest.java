package com.nutriconsultas.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendPatientMessageRequest(@NotBlank @Size(max = 2000) String body) {
}
