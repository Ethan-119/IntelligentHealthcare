package com.intelligenthealthcare.patient.api;

import com.intelligenthealthcare.auth.api.dto.CurrentPatientResponse;
import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.patient.api.dto.UpdateMyProfileRequest;
import com.intelligenthealthcare.patient.application.PatientProfileApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patient/me")
public class PatientProfileController {

    private final PatientProfileApplicationService patientProfileApplicationService;

    public PatientProfileController(PatientProfileApplicationService patientProfileApplicationService) {
        this.patientProfileApplicationService = patientProfileApplicationService;
    }

    @GetMapping
    public CurrentPatientResponse me(@AuthenticationPrincipal PatientAuthPrincipal principal) {
        ensureLogin(principal);
        return patientProfileApplicationService.me(principal);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public CurrentPatientResponse updateMyProfile(
            @AuthenticationPrincipal PatientAuthPrincipal principal,
            @Valid @RequestBody UpdateMyProfileRequest request) {
        ensureLogin(principal);
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

    private static void ensureLogin(PatientAuthPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
    }
}
