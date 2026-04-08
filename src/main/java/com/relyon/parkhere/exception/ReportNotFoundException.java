package com.relyon.parkhere.exception;

public class ReportNotFoundException extends DomainException {

    public ReportNotFoundException(String reportId) {
        super("report.not.found", reportId);
    }
}
