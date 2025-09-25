package it.gov.pagopa.gpd.upload.model.v2;

import io.micronaut.core.convert.format.Format;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Serdeable
@Builder(toBuilder = true)
//@Getter
//@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UploadReportDTO {
    private String fileId;
    private int processedItem;
    private int submittedItem;
//    private List<ResponseEntryDTO> responses;

//    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
//    @Format("yyyy-MM-dd'T'HH:mm:ss.SSS")
//    @Schema(example = "2024-10-08T14:55:16.302Z")
//    @JsonSerialize(as = LocalDateTimeSerializer.class)
//    private LocalDateTime startTime;

//    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
//    @Format("yyyy-MM-dd'T'HH:mm:ss.SSS")
//    @Schema(example = "2024-10-08T14:55:16.302Z")
//    @JsonSerialize(as = LocalDateTimeSerializer.class)
//    private LocalDateTime endTime;

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public int getProcessedItem() { return processedItem; }
    public void setProcessedItem(int processedItem) { this.processedItem = processedItem; }

    public int getSubmittedItem() { return submittedItem; }
    public void setSubmittedItem(int submittedItem) { this.submittedItem = submittedItem; }

}
