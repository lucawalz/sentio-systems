package dev.syslabs.sentio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentQuery {
    @NotBlank(message = "Query must not be blank")
    @Size(max = 2000, message = "Query must be at most 2000 characters")
    private String query;
}
