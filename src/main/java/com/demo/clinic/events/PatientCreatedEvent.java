package com.demo.clinic.events;

import org.springframework.context.ApplicationEvent;

/** Fired when a new patient is registered. */
public class PatientCreatedEvent extends ApplicationEvent {
    private final Long patientId;

    public PatientCreatedEvent(Object source, Long patientId) {
        super(source);
        this.patientId = patientId;
    }

    public Long getPatientId() { return patientId; }
}
