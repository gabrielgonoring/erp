package company.aura.erp.entity;

import company.aura.erp.enums.CompanyEmailConfigStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_email_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CompanyEmailConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "notification_type", nullable = false, length = 60)
    private String notificationType;

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Column(name = "from_name", nullable = false)
    private String fromName;

    @Column(name = "reply_to")
    private String replyTo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 45)
    private CompanyEmailConfigStatus status = CompanyEmailConfigStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
