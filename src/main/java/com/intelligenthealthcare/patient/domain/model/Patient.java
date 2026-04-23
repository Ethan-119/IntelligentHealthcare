package com.intelligenthealthcare.patient.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 互联网场景下可视为“应用用户 + 健康档案”的聚合；后续若出现医护等多角色，可再拆出独立 User/Account 实体并做关联。
 * <p>
 * 密码仅存 {@code passwordHash}，明文凭 {@link org.springframework.security.crypto.password.PasswordEncoder} 校验。
 */
@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String medicalRecordNo;

    /* --- 用户中心：注册/登录与身份（密码仅存哈希） --- */
    @Column(unique = true, length = 20)
    private String phone;

    @Column(unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", length = 200)
    private String passwordHash;

    /* --- 健康档案：个性化推荐与问诊上下文 --- */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @Builder.Default
    private Gender gender = Gender.UNKNOWN;

    private LocalDate birthDate;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String medicalHistory;
}
