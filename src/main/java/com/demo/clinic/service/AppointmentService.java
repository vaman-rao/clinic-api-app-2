package com.demo.clinic.service;

import com.demo.clinic.dto.request.AppointmentRequest;
import com.demo.clinic.dto.response.AppointmentResponse;
import com.demo.clinic.events.AppointmentCancelledEvent;
import com.demo.clinic.events.AppointmentCreatedEvent;
import com.demo.clinic.exception.BusinessRuleException;
import com.demo.clinic.exception.DuplicateResourceException;
import com.demo.clinic.exception.ResourceNotFoundException;
import com.demo.clinic.model.Appointment;
import com.demo.clinic.model.Doctor;
import com.demo.clinic.model.Patient;
import com.demo.clinic.model.enums.AppointmentStatus;
import com.demo.clinic.repository.AppointmentRepository;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pure business logic. Zero Micrometer imports.
 *
 * Observability is achieved two ways, both non-invasive:
 *   1. @Timed on each method  → AOP aspect records latency histogram automatically
 *   2. publishEvent(...)      → ClinicMetricsListener increments the right counter
 *
 * This class does not know who listens to its events or what they do with them.
 */
@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorService doctorService,
                              PatientService patientService,
                              ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.doctorService  = doctorService;
        this.patientService = patientService;
        this.eventPublisher = eventPublisher;
    }

    @Timed("clinic.service.appointments.getAll")
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.appointments.getById")
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
    }

    @Timed("clinic.service.appointments.getByPatient")
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatientId(Long patientId) {
        patientService.getPatientById(patientId);
        return appointmentRepository.findByPatientPatientId(patientId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.appointments.getByDoctor")
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctorId(Long doctorId) {
        doctorService.getDoctorById(doctorId);
        return appointmentRepository.findByDoctorDoctorId(doctorId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.appointments.getByDate")
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDate(LocalDate date) {
        return appointmentRepository.findByAppointmentDate(date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.appointments.getByStatus")
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatus(status).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.appointments.getDoctorSchedule")
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorScheduleForDate(Long doctorId, LocalDate date) {
        doctorService.getDoctorById(doctorId);
        return appointmentRepository.findByDoctorDoctorIdAndAppointmentDate(doctorId, date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.appointments.create")
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        Doctor doctor = doctorService.getDoctorEntityById(request.getDoctorId());
        if (!doctor.isAvailable()) {
            throw new BusinessRuleException(
                    "Doctor with id " + request.getDoctorId() + " is currently not available for appointments");
        }

        Patient patient = patientService.getPatientEntityById(request.getPatientId());

        if (appointmentRepository.existsByDoctorDoctorIdAndAppointmentDateAndSlot(
                request.getDoctorId(), request.getAppointmentDate(), request.getSlot())) {
            throw new DuplicateResourceException(
                    "Doctor already has an appointment on " + request.getAppointmentDate()
                            + " at " + request.getSlot() + ". Please choose a different slot.");
        }

        boolean patientHasConflict = appointmentRepository
                .findByPatientPatientId(request.getPatientId()).stream()
                .anyMatch(a -> a.getAppointmentDate().equals(request.getAppointmentDate())
                        && a.getSlot().equals(request.getSlot())
                        && a.getStatus() != AppointmentStatus.CANCELLED);
        if (patientHasConflict) {
            throw new BusinessRuleException(
                    "Patient already has an appointment on " + request.getAppointmentDate()
                            + " at " + request.getSlot());
        }

        Appointment appointment = new Appointment(doctor, patient,
                request.getAppointmentDate(), request.getSlot(), request.getReason());
        appointment.setNotes(request.getNotes());
        Appointment saved = appointmentRepository.save(appointment);

        // Signal to the metrics layer — no Micrometer code here
        eventPublisher.publishEvent(
                new AppointmentCreatedEvent(this, saved.getAppointmentId(),
                        doctor.getDoctorId(), patient.getPatientId()));

        return toResponse(saved);
    }

    @Timed("clinic.service.appointments.updateStatus")
    public AppointmentResponse updateAppointmentStatus(Long id, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        validateStatusTransition(appointment.getStatus(), newStatus);
        appointment.setStatus(newStatus);
        Appointment saved = appointmentRepository.save(appointment);

        if (newStatus == AppointmentStatus.CANCELLED) {
            eventPublisher.publishEvent(new AppointmentCancelledEvent(this, saved.getAppointmentId()));
        }

        return toResponse(saved);
    }

    @Timed("clinic.service.appointments.update")
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {
        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        if (existing.getStatus() == AppointmentStatus.CANCELLED
                || existing.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "Cannot update a " + existing.getStatus().name().toLowerCase() + " appointment");
        }

        Doctor doctor = doctorService.getDoctorEntityById(request.getDoctorId());
        if (!doctor.isAvailable()) {
            throw new BusinessRuleException("Doctor is not available for appointments");
        }

        Patient patient = patientService.getPatientEntityById(request.getPatientId());

        boolean slotTaken = appointmentRepository
                .findByDoctorDoctorIdAndAppointmentDateAndSlot(
                        request.getDoctorId(), request.getAppointmentDate(), request.getSlot())
                .map(a -> !a.getAppointmentId().equals(id))
                .orElse(false);
        if (slotTaken) {
            throw new DuplicateResourceException("Doctor already has an appointment at that date and slot");
        }

        existing.setDoctor(doctor);
        existing.setPatient(patient);
        existing.setAppointmentDate(request.getAppointmentDate());
        existing.setSlot(request.getSlot());
        existing.setReason(request.getReason());
        existing.setNotes(request.getNotes());
        return toResponse(appointmentRepository.save(existing));
    }

    @Timed("clinic.service.appointments.delete")
    public void deleteAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot delete a completed appointment");
        }
        appointmentRepository.deleteById(id);
    }

    private void validateStatusTransition(AppointmentStatus current, AppointmentStatus next) {
        if (current == AppointmentStatus.CANCELLED)
            throw new BusinessRuleException("Cannot change status of a cancelled appointment");
        if (current == AppointmentStatus.COMPLETED)
            throw new BusinessRuleException("Cannot change status of a completed appointment");
        if (current == AppointmentStatus.NO_SHOW && next == AppointmentStatus.SCHEDULED)
            throw new BusinessRuleException("Cannot revert a no-show appointment back to scheduled");
    }

    private AppointmentResponse toResponse(Appointment a) {
        AppointmentResponse r = new AppointmentResponse();
        r.setAppointmentId(a.getAppointmentId());
        r.setDoctorId(a.getDoctor().getDoctorId());
        r.setDoctorName(a.getDoctor().getName());
        r.setDoctorSpecialization(a.getDoctor().getSpecialization().name());
        r.setPatientId(a.getPatient().getPatientId());
        r.setPatientName(a.getPatient().getName());
        r.setAppointmentDate(a.getAppointmentDate());
        r.setSlot(a.getSlot());
        r.setReason(a.getReason());
        r.setStatus(a.getStatus());
        r.setNotes(a.getNotes());
        return r;
    }
}
