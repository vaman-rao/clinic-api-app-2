package com.demo.clinic.events;

import org.springframework.context.ApplicationEvent;

/** Fired when a doctor record is removed. */
public class DoctorDeletedEvent extends ApplicationEvent {
    private final Long doctorId;

    public DoctorDeletedEvent(Object source, Long doctorId) {
        super(source);
        this.doctorId = doctorId;
    }

    public Long getDoctorId() { return doctorId; }
}
