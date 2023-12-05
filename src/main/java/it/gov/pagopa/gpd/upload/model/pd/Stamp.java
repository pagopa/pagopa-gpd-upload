package it.gov.pagopa.gpd.upload.model.pd;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stamp implements Serializable {
    private static final long serialVersionUID = -5862140737726963810L;

    @NotBlank
    @Schema(required = true, description = "Document hash")
    private String hashDocument;

    @NotBlank
    @Size(min = 2, max = 2)
    @Schema(required = true, description = "The type of the stamp", minLength = 2, maxLength = 2)
    private String stampType;

    @NotBlank
    @Pattern(regexp = "[A-Z]{2}")
    @Schema(required = true, description = "The provincial of the residence", example = "RM", pattern = "[A-Z]{2,2}")
    private String provincialResidence;
}
