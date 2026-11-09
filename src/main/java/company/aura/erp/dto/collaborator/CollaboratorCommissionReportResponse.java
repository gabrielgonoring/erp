package company.aura.erp.dto.collaborator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorCommissionReportResponse {

    private Long collaboratorId;

    private String collaboratorName;

    private CollaboratorCommissionReportHeader header;

    private List<CollaboratorSoldItemResponse> items;

}
