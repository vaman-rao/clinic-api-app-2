package com.demo.clinic.metrics;

import com.demo.clinic.events.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * ─────────────────────────────────────────────────────────────
 *  THE ONLY CLASS IN THIS CODEBASE THAT IMPORTS io.micrometer
 *  for business counters.
 * ─────────────────────────────────────────────────────────────
 *
 * Business services publish plain Spring ApplicationEvents.
 * This listener intercepts them and records Prometheus counters.
 *
 * Services have zero knowledge of Micrometer. This class has
 * zero knowledge of business logic. They are fully decoupled.
 *
 * To add a new counter:
 *   1. Create a new event class in the events/ package.
 *   2. Publish it from the service with eventPublisher.publishEvent(...)
 *   3. Add a @EventListener method here.
 *   No other files need to change.
 */
@Component
public class ClinicMetricsListener {

    private final Counter appointmentCreated;
    private final Counter appointmentCancelled;
    private final Counter doctorCreated;
    private final Counter doctorDeleted;
    private final Counter patientCreated;

    public ClinicMetricsListener(MeterRegistry registry) {
        this.appointmentCreated   = Counter.builder("clinic.appointments.created")
                .description("Total appointments booked").register(registry);
        this.appointmentCancelled = Counter.builder("clinic.appointments.cancelled")
                .description("Total appointments cancelled").register(registry);
        this.doctorCreated        = Counter.builder("clinic.doctors.created")
                .description("Total doctors registered").register(registry);
        this.doctorDeleted        = Counter.builder("clinic.doctors.deleted")
                .description("Total doctors deleted").register(registry);
        this.patientCreated       = Counter.builder("clinic.patients.created")
                .description("Total patients registered").register(registry);
    }

    @EventListener
    public void on(AppointmentCreatedEvent e)   { appointmentCreated.increment(); }

    @EventListener
    public void on(AppointmentCancelledEvent e) { appointmentCancelled.increment(); }

    @EventListener
    public void on(DoctorCreatedEvent e)        { doctorCreated.increment(); }

    @EventListener
    public void on(DoctorDeletedEvent e)        { doctorDeleted.increment(); }

    @EventListener
    public void on(PatientCreatedEvent e)       { patientCreated.increment(); }
}
