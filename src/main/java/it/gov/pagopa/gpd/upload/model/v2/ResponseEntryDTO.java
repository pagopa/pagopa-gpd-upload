package it.gov.pagopa.gpd.upload.model.v2;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonSerialize
public class ResponseEntryDTO {
    @Schema(example = "400")
    public Integer statusCode;
    @Schema(example = "Bad request caused by invalid email address")
    public String statusMessage;
    public List<String> iupds; // IUPDs
}
