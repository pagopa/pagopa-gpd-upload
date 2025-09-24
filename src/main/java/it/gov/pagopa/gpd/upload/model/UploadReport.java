package it.gov.pagopa.gpd.upload.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonSerialize
public class UploadReport {
    public String uploadID;
    public int processedItem;
    public int submittedItem;
    public List<ResponseEntry> responses;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @Schema(example = "2024-10-08T14:55:16.302Z")
    @JsonSerialize(as = LocalDateTimeSerializer.class)
    public LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @Schema(example = "2024-10-08T14:55:16.302Z")
    @JsonSerialize(as = LocalDateTimeSerializer.class)
    public LocalDateTime endTime;
    private ServiceType serviceType;
}
