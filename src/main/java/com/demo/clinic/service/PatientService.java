package com.demo.clinic.service;

import com.demo.clinic.dto.request.PatientRequest;
import com.demo.clinic.dto.response.PatientResponse;
import com.demo.clinic.events.PatientCreatedEvent;
import com.demo.clinic.exception.DuplicateResourceException;
import com.demo.clinic.exception.ResourceNotFoundException;
import com.demo.clinic.model.Patient;
import com.demo.clinic.repository.PatientRepository;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pure business logic. Zero Micrometer imports.
 * Publishes events; ClinicMetricsListener handles the counters.
 */
@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PatientService(PatientRepository patientRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.patientRepository = patientRepository;
        this.eventPublisher    = eventPublisher;
    }

    @Timed("clinic.service.patients.getAll")
    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.patients.getById")
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        return patientRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }

    @Timed("clinic.service.patients.getByEmail")
    @Transactional(readOnly = true)
    public PatientResponse getPatientByEmail(String email) {
        return patientRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "email", email));
    }

    @Timed("clinic.service.patients.searchByName")
    @Transactional(readOnly = true)
    public List<PatientResponse> searchPatientsByName(String name) {
        return patientRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.patients.create")
    public PatientResponse createPatient(PatientRequest request) {
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Patient", "email", request.getEmail());
        }
        Patient saved = patientRepository.save(toEntity(request));
        eventPublisher.publishEvent(new PatientCreatedEvent(this, saved.getPatientId()));
        return toResponse(saved);
    }

    @Timed("clinic.service.patients.update")
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient existing = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));

        if (!existing.getEmail().equals(request.getEmail())
                && patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Patient", "email", request.getEmail());
        }

        existing.setName(request.getName());
        existing.setDateOfBirth(request.getDateOfBirth());
        existing.setGender(request.getGender());
        existing.setContact(request.getContact());
        existing.setEmail(request.getEmail());
        existing.setPassword(request.getPassword());
        existing.setBloodGroup(request.getBloodGroup());
        return toResponse(patientRepository.save(existing));
    }

    @Timed("clinic.service.patients.delete")
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Patient", "id", id);
        }
        patientRepository.deleteById(id);
    }

    // Internal — used by AppointmentService
    public Patient getPatientEntityById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }

    private Patient toEntity(PatientRequest request) {
        Patient p = new Patient();
        p.setName(request.getName());
        p.setDateOfBirth(request.getDateOfBirth());
        p.setGender(request.getGender());
        p.setContact(request.getContact());
        p.setEmail(request.getEmail());
        p.setPassword(request.getPassword());
        p.setBloodGroup(request.getBloodGroup());
        return p;
    }

    public PatientResponse toResponse(Patient p) {
        return new PatientResponse(p.getPatientId(), p.getName(), p.getDateOfBirth(),
                p.getGender(), p.getContact(), p.getEmail(), p.getBloodGroup());
    }
}
