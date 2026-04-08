package com.relyon.parkhere.exception;

public class ReportCooldownException extends DomainException {

    public ReportCooldownException() {
        super("report.cooldown");
    }
}
