package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.DoctorProfile;
import com.intelligenthealthcare.knowledge.domain.repository.DoctorProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisDoctorProfileRepository implements DoctorProfileRepository {

    private final DoctorProfileMapper doctorProfileMapper;

    @Override
    public List<DoctorProfile> findHotDoctors(int limit) {
        LambdaQueryWrapper<DoctorProfile> query = new LambdaQueryWrapper<>();
        query.eq(DoctorProfile::getActiveStatus, 1);
        query.ge(DoctorProfile::getAuthorityScore, new BigDecimal("3.0"));
        query.orderByDesc(DoctorProfile::getAuthorityScore);
        query.last("LIMIT " + limit);
        return doctorProfileMapper.selectList(query);
    }

    @Override
    public Optional<DoctorProfile> findById(Long id) {
        DoctorProfile doctor = doctorProfileMapper.selectById(id);
        if (doctor == null || !Integer.valueOf(1).equals(doctor.getActiveStatus())) {
            return Optional.empty();
        }
        return Optional.of(doctor);
    }

    @Override
    public List<DoctorProfile> findByDepartmentId(Long departmentId) {
        LambdaQueryWrapper<DoctorProfile> query = new LambdaQueryWrapper<>();
        query.eq(DoctorProfile::getDepartmentId, departmentId);
        query.eq(DoctorProfile::getActiveStatus, 1);
        query.orderByDesc(DoctorProfile::getAuthorityScore);
        return doctorProfileMapper.selectList(query);
    }
}
