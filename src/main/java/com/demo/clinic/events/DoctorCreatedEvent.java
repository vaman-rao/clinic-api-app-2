package com.demo.clinic.events;

import org.springframework.context.ApplicationEvent;

/** Fired when a new doctor is registered. */
public class DoctorCreatedEvent extends ApplicationEvent {
    private final Long doctorId;

    public DoctorCreatedEvent(Object source, Long doctorId) {
        super(source);
        this.doctorId = doctorId;
    }

    public Long getDoctorId() { return doctorId; }
}
