package com.demo.clinic.service;

import com.demo.clinic.dto.request.DoctorRequest;
import com.demo.clinic.dto.response.DoctorResponse;
import com.demo.clinic.events.DoctorCreatedEvent;
import com.demo.clinic.events.DoctorDeletedEvent;
import com.demo.clinic.exception.DuplicateResourceException;
import com.demo.clinic.exception.ResourceNotFoundException;
import com.demo.clinic.model.Doctor;
import com.demo.clinic.model.enums.Specialization;
import com.demo.clinic.repository.DoctorRepository;
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
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DoctorService(DoctorRepository doctorRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.doctorRepository = doctorRepository;
        this.eventPublisher   = eventPublisher;
    }

    @Timed("clinic.service.doctors.getAll")
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.doctors.getById")
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    @Timed("clinic.service.doctors.getByEmail")
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorByEmail(String email) {
        return doctorRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "email", email));
    }

    @Timed("clinic.service.doctors.getBySpecialization")
    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsBySpecialization(Specialization specialization) {
        return doctorRepository.findBySpecialization(specialization).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.doctors.getAvailable")
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAvailableDoctors() {
        return doctorRepository.findByAvailableTrue().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.doctors.searchByName")
    @Transactional(readOnly = true)
    public List<DoctorResponse> searchDoctorsByName(String name) {
        return doctorRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Timed("clinic.service.doctors.create")
    public DoctorResponse createDoctor(DoctorRequest request) {
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Doctor", "email", request.getEmail());
        }
        Doctor saved = doctorRepository.save(toEntity(request));
        eventPublisher.publishEvent(new DoctorCreatedEvent(this, saved.getDoctorId()));
        return toResponse(saved);
    }

    @Timed("clinic.service.doctors.update")
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor existing = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));

        if (!existing.getEmail().equals(request.getEmail())
                && doctorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Doctor", "email", request.getEmail());
        }

        existing.setName(request.getName());
        existing.setGender(request.getGender());
        existing.setSpecialization(request.getSpecialization());
        existing.setContact(request.getContact());
        existing.setEmail(request.getEmail());
        existing.setPassword(request.getPassword());
        return toResponse(doctorRepository.save(existing));
    }

    @Timed("clinic.service.doctors.delete")
    public void deleteDoctor(Long id) {
        if (!doctorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Doctor", "id", id);
        }
        doctorRepository.deleteById(id);
        eventPublisher.publishEvent(new DoctorDeletedEvent(this, id));
    }

    // Internal — used by AppointmentService
    public Doctor getDoctorEntityById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    private Doctor toEntity(DoctorRequest request) {
        Doctor doctor = new Doctor();
        doctor.setName(request.getName());
        doctor.setGender(request.getGender());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setContact(request.getContact());
        doctor.setEmail(request.getEmail());
        doctor.setPassword(request.getPassword());
        return doctor;
    }

    public DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getDoctorId(), doctor.getName(), doctor.getGender(),
                doctor.getSpecialization(), doctor.getContact(),
                doctor.getEmail(), doctor.isAvailable());
    }
}
