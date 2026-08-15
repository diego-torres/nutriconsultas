package com.nutriconsultas.mobile.dto;

import org.springframework.http.MediaType;

/**
 * Binary platillo picture payload for {@code GET
 * /rest/mobile/patient/diet-plans/{assignmentId}/platillos/{platilloIngestaId}/image}
 * (#598).
 */
public record DietPlatilloImageResult(byte[] content, MediaType mediaType) {

}
