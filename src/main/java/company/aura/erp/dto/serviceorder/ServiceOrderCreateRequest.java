package company.aura.erp.dto.serviceorder;

import company.aura.erp.enums.ServiceOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderCreateRequest {

    @NotNull(message = "O parceiro (cliente) é obrigatório.")
    private Long partnerId;

    private Long technicianId;

    private Long termId;

    private ServiceOrderStatus status;

    @NotBlank(message = "O título do objeto é obrigatório.")
    @Size(max = 150, message = "O título do objeto deve ter no máximo 150 caracteres.")
    private String objectTitle;

    @Size(max = 100, message = "O identificador do objeto deve ter no máximo 100 caracteres.")
    private String objectIdentifier;

    private String entryDetails;

    @NotBlank(message = "O problema relatado é obrigatório.")
    private String reportedIssue;

    private String technicalReport;

    private String internalNotes;

    private LocalDateTime entryAt;

    private LocalDateTime expectedDeliveryAt;

    @NotNull(message = "É preciso adicionar itens a order de serviço")
    @NotEmpty(message = "É preciso adicionar itens a order de serviço")
    private List<@Valid ServiceOrderItemCreateRequest> items;
}
