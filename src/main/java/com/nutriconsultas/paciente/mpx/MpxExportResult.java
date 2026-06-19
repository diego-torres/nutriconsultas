package com.nutriconsultas.paciente.mpx;

/**
 * Binary MPX export payload and suggested download filename (#221).
 */
public record MpxExportResult(byte[] content, String filename) {

}
