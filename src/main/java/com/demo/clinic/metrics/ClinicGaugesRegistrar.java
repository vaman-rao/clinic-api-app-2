package com.demo.clinic.metrics;

import com.demo.clinic.model.enums.AppointmentStatus;
import com.demo.clinic.repository.AppointmentRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Registers Prometheus Gauges that reflect live DB state.
 *
 * Gauges are pull-based: Micrometer calls the lambda on every scrape,
 * hitting the DB to return the current count. No manual increment needed.
 *
 * Intentionally separate from ClinicMetricsListener:
 *   Listener  → push model (reacts to events)   → counters
 *   Registrar → pull model (queried each scrape) → gauges
 */
@Component
public class ClinicGaugesRegistrar {

    private final AppointmentRepository appointmentRepository;
    private final MeterRegistry registry;

    public ClinicGaugesRegistrar(AppointmentRepository appointmentRepository,
                                  MeterRegistry registry) {
        this.appointmentRepository = appointmentRepository;
        this.registry              = registry;
    }

    @PostConstruct
    public void register() {
        Gauge.builder("clinic.appointments.scheduled.total", appointmentRepository,
                        repo -> repo.countByStatus(AppointmentStatus.SCHEDULED))
                .description("Current appointments in SCHEDULED state")
                .register(registry);

        Gauge.builder("clinic.appointments.today.total", appointmentRepository,
                        repo -> repo.countByDate(LocalDate.now()))
                .description("Appointments scheduled for today")
                .register(registry);
    }
}
