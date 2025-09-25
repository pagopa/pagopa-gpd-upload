package it.gov.pagopa.gpd.upload.model.v2;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Serdeable
public class ResponseEntryDTO {
    @Schema(example = "400")
    private Integer statusCode;
    @Schema(example = "Bad request caused by invalid email address")
    private String statusMessage;
    private List<String> iupds; // IUPDs
}
