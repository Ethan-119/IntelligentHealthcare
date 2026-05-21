package com.intelligenthealthcare.auth.infrastructure.security;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 密码哈希服务：统一封装 BCrypt，加盐与校验逻辑集中管理。
 */
@Component
public class PasswordHashService {

    private static final int WORK_FACTOR = 12;

    public String hash(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(hashedPassword)) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, normalizeHashPrefix(hashedPassword));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 兼容历史测试数据中的 BCrypt 前缀：
     * jBCrypt 对 "$2a$" 兼容最好，旧库里常见 "$2b$" / "$2y$"。
     */
    private static String normalizeHashPrefix(String hash) {
        if (!StringUtils.hasText(hash)) {
            return hash;
        }
        if (hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }
}
