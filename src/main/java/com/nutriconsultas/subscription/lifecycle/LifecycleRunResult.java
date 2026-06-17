package com.nutriconsultas.subscription.lifecycle;

public record LifecycleRunResult(int graceTransitions, int suspendedTransitions, int remindersSent) {

}
