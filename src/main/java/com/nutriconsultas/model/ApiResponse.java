package com.nutriconsultas.model;

import com.nutriconsultas.platillos.Ingrediente;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
    public ApiResponse(T data) {
        this.status = 200;
        this.message = "OK";
        this.data = data;
    }
}
