package company.aura.erp.dto;

import company.aura.erp.entity.UserEntity;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        Long companyId,
        String name,
        String email,
        String status,
        LocalDateTime lastLogin,
        LocalDateTime createdAt
) {
    public static UserResponse fromEntity(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getCompany() != null ? entity.getCompany().getId() : null,
                entity.getName(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getLastLogin(),
                entity.getCreatedAt()
        );
    }
}
