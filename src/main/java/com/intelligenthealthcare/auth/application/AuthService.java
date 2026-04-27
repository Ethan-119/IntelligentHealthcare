package com.intelligenthealthcare.auth.application;

import com.intelligenthealthcare.auth.api.dto.CurrentPatientResponse;
import com.intelligenthealthcare.auth.api.dto.LoginRequest;
import com.intelligenthealthcare.auth.api.dto.RegisterRequest;
import com.intelligenthealthcare.auth.api.dto.TokenResponse;
import com.intelligenthealthcare.auth.config.JwtProperties;
import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.auth.infrastructure.jwt.JwtService;
import com.intelligenthealthcare.patient.domain.model.Gender;
import com.intelligenthealthcare.patient.domain.model.Patient;
import com.intelligenthealthcare.patient.domain.model.TriagePrefer;
import com.intelligenthealthcare.patient.domain.repository.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * 患者注册/登录与当前用户信息；密码仅经 {@link org.springframework.security.crypto.password.PasswordEncoder} 单向存储。
 */
@Service
public class AuthService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(
            PatientRepository patientRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    /** 注册新用户：校验手机号唯一，初始化导诊上下文并签发 JWT。 */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String phone = request.getPhone().trim();
        if (phone.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号无效");
        }
        if (patientRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该手机号已注册");
        }
        String password = request.getPassword();
        Gender patientGender = request.getPatientGender() != null ? request.getPatientGender() : Gender.UNKNOWN;
        TriagePrefer triagePrefer =
                request.getTriagePrefer() != null ? request.getTriagePrefer() : TriagePrefer.NEARBY;

        Patient toSave = Patient.builder()
                .phone(phone)
                .username(trimToNull(request.getUsername()))
                .password(passwordEncoder.encode(password))
                .status(1)
                .deleted(0)
                .patientAge(request.getPatientAge())
                .patientGender(patientGender)
                .residentCity(trimToNull(request.getResidentCity()))
                .area(trimToNull(request.getArea()))
                .triagePrefer(triagePrefer)
                .build();
        patientRepository.save(toSave);
        return buildTokenResponse(toSave);
    }

    /**
     * 登录：根据手机号定位患者并校验密码，成功则签发 JWT。账号异常与密码错误均返回 401、同一提示，减少枚举有效账号。
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String phone = request.getPhone().trim();
        if (phone.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号无效");
        }

        Patient matchedByPhone = patientRepository.findByPhone(phone).orElse(null);
        if (matchedByPhone == null || matchedByPhone.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        Long patientId = matchedByPhone.getId();
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        if (!Integer.valueOf(1).equals(patient.getStatus()) || Integer.valueOf(1).equals(patient.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已禁用");
        }
        if (patient.getPassword() == null || !passwordEncoder.matches(request.getPassword(), patient.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        return buildTokenResponse(patient);
    }

    /** 按主键重载患者档案，避免只信任 JWT 中缓存的敏感字段（若将来扩展可在此脱敏）。 */
    @Transactional(readOnly = true)
    public CurrentPatientResponse me(PatientAuthPrincipal principal) {
        Patient patient = patientRepository.findById(principal.getId()).orElse(null);
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或已删除");
        }
        return CurrentPatientResponse.fromEntity(patient);
    }

    private TokenResponse buildTokenResponse(Patient patient) {
        // expiresInMs 与 app.jwt.expiration-ms 一致，供前端与 token 内 exp 同步展示
        String token = jwtService.createToken(patient.getId());
        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.expirationMs())
                .user(CurrentPatientResponse.fromEntity(patient))
                .build();
    }

    /** 空串与纯空白按「未提供」处理。 */
    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return StringUtils.hasText(t) ? t : null;
    }
}
