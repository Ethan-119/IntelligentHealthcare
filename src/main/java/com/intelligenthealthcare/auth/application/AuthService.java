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
import com.intelligenthealthcare.patient.infrastructure.persistence.PatientRepository;
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

    /**
     * 注册新患者：校验手机/邮箱唯一性，生成病历号，写入密码哈希后签发 JWT。
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String phone = request.getPhone().trim();
        if (phone.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号无效");
        }
        if (patientRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该手机号已注册");
        }
        String email = trimToNull(request.getEmail());
        if (email != null && patientRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册");
        }
        String password = request.getPassword();
        Gender gender = request.getGender() != null ? request.getGender() : Gender.UNKNOWN;

        Patient toSave = Patient.builder()
                .name(request.getName().trim())
                .medicalRecordNo(newMedicalRecordNo())
                .phone(phone)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .gender(gender)
                .birthDate(request.getBirthDate())
                .medicalHistory(request.getMedicalHistory())
                .build();
        Patient saved = patientRepository.save(toSave);
        return buildTokenResponse(saved);
    }

    /**
     * 登录：根据账号定位患者并校验密码，成功则签发 JWT。账号异常与密码错误均返回 401、同一提示，减少枚举有效账号。
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String account = request.getAccount().trim();
        if (account.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号无效");
        }
        Patient patient = resolveByAccount(account);
        if (patient.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), patient.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        return buildTokenResponse(patient);
    }

    /** 按主键重载患者档案，避免只信任 JWT 中缓存的敏感字段（若将来扩展可在此脱敏）。 */
    @Transactional(readOnly = true)
    public CurrentPatientResponse me(PatientAuthPrincipal principal) {
        return patientRepository
                .findById(principal.getId())
                .map(CurrentPatientResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或已删除"));
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

    /**
     * 含 {@code '@'} 视为邮箱，否则视为手机号，与 {@link com.intelligenthealthcare.auth.api.dto.LoginRequest#getAccount} 约定一致。
     */
    private Patient resolveByAccount(String account) {
        if (account.contains("@")) {
            return patientRepository
                    .findByEmail(account)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
        }
        return patientRepository
                .findByPhone(account)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
    }

    /**
     * 保证 {@link com.intelligenthealthcare.patient.domain.model.Patient#medicalRecordNo} 在库中唯一，极端碰撞时退回纳秒时间戳。
     */
    private String newMedicalRecordNo() {
        for (int i = 0; i < 20; i++) {
            String candidate = "MR" + System.currentTimeMillis() + (int) (Math.random() * 10000);
            if (!patientRepository.existsByMedicalRecordNo(candidate)) {
                return candidate;
            }
        }
        return "MR" + System.nanoTime();
    }

    /** 空串与纯空白按「未提供」处理，避免唯一索引与可空 email 的歧义。 */
    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return StringUtils.hasText(t) ? t : null;
    }
}
