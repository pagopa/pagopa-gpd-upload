package it.gov.pagopa.gpd.upload.model.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import it.gov.pagopa.gpd.upload.model.v2.enumeration.OperationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize
@ToString
@Builder(toBuilder = true)
public class UploadStatusDTO {
    private String fileId;
    private int processedItem;
    private int submittedItem;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(example = "2024-10-08T14:55:16.302Z")
    private LocalDateTime startTime;

    private OperationStatus operationStatus;
}
