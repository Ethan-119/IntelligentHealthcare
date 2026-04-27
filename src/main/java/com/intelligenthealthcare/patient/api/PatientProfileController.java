package com.intelligenthealthcare.patient.api;

import com.intelligenthealthcare.auth.api.dto.CurrentPatientResponse;
import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.patient.api.dto.UpdateMyProfileRequest;
import com.intelligenthealthcare.patient.application.PatientProfileApplicationService;
import com.intelligenthealthcare.shared.security.CurrentPatient;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient/me")
public class PatientProfileController {

    private final PatientProfileApplicationService patientProfileApplicationService;

    public PatientProfileController(PatientProfileApplicationService patientProfileApplicationService) {
        this.patientProfileApplicationService = patientProfileApplicationService;
    }

    @GetMapping
    public CurrentPatientResponse me(@CurrentPatient PatientAuthPrincipal principal) {
        return patientProfileApplicationService.me(principal);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public CurrentPatientResponse updateMyProfile(
            @CurrentPatient PatientAuthPrincipal principal,
            @Valid @RequestBody UpdateMyProfileRequest request) {
        return patientProfileApplicationService.updateMyProfile(
                principal,
                request.getUsername(),
                request.getPhone(),
                request.getPatientAge(),
                request.getPatientGender(),
                request.getResidentCity(),
                request.getArea(),
                request.getTriagePrefer());
    }
}
