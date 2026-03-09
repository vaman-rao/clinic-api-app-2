package com.demo.clinic.events;

import org.springframework.context.ApplicationEvent;

/**
 * Fired when a new appointment is successfully booked.
 * Business code publishes this; the metrics layer listens.
 * Neither side knows about the other.
 */
public class AppointmentCreatedEvent extends ApplicationEvent {
    private final Long appointmentId;
    private final Long doctorId;
    private final Long patientId;

    public AppointmentCreatedEvent(Object source, Long appointmentId, Long doctorId, Long patientId) {
        super(source);
        this.appointmentId = appointmentId;
        this.doctorId      = doctorId;
        this.patientId     = patientId;
    }

    public Long getAppointmentId() { return appointmentId; }
    public Long getDoctorId()      { return doctorId; }
    public Long getPatientId()     { return patientId; }
}
