package company.aura.erp.dto.collaborator;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.mapper.CollaboratorMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorSummaryResponse {

    private Long id;

    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Long userId;

    @JsonProperty("nome")
    @JsonAlias("name")
    private String nome;

    private String cpf;

    private String email;

    private String status;

    @JsonProperty("name")
    public String getName() {
        return this.nome;
    }

    public static CollaboratorSummaryResponse fromEntity(CollaboratorEntity entity) {
        return CollaboratorMapper.toSummaryResponse(entity);
    }
}
