package com.nutriconsultas.device;

/**
 * Outcome of a single-device push attempt (#575).
 */
public enum PushDeliveryResult {

	SUCCESS, TRANSIENT_FAILURE, INVALID_TOKEN, SKIPPED

}
