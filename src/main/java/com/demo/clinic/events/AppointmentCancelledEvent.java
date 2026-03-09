package com.demo.clinic.events;

import org.springframework.context.ApplicationEvent;

/** Fired when an appointment is moved to CANCELLED status. */
public class AppointmentCancelledEvent extends ApplicationEvent {
    private final Long appointmentId;

    public AppointmentCancelledEvent(Object source, Long appointmentId) {
        super(source);
        this.appointmentId = appointmentId;
    }

    public Long getAppointmentId() { return appointmentId; }
}
