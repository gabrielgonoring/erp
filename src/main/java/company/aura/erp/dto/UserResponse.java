package company.aura.erp.dto;

import company.aura.erp.entity.UserEntity;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        Long companyId,
        String name,
        String email,
        String status,
        Boolean temporaryPassword,
        LocalDateTime lastLogin,
        LocalDateTime createdAt
) {
    public UserResponse(Long id, Long companyId, String name, String email, String status, LocalDateTime lastLogin, LocalDateTime createdAt) {
        this(id, companyId, name, email, status, false, lastLogin, createdAt);
    }

    public static UserResponse fromEntity(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getTemporaryPassword() != null ? entity.getTemporaryPassword() : false,
                entity.getLastLogin(),
                entity.getCreatedAt()
        );
    }
}
