package it.gov.pagopa.gpd.upload.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize
@ToString
public class UploadStatus {
    private String uploadID;
    private int processedItem;
    private int submittedItem;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(example = "2024-10-08T14:55:16.302Z")
    @JsonSerialize(as = LocalDateTimeSerializer.class)
    public LocalDateTime startTime;
}
