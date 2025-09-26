package it.gov.pagopa.gpd.upload.model.v2;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.gov.pagopa.gpd.upload.model.UploadStatus;
import it.gov.pagopa.gpd.upload.model.v2.enumeration.OperationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize
@ToString
public class UploadStatusV2 extends UploadStatus {
    public OperationStatus operationStatus;

    @Builder
    public UploadStatusV2(String uploadID, int processedItem, int submittedItem, LocalDateTime startTime, OperationStatus operationStatus) {
        super(uploadID, processedItem, submittedItem, startTime);
        this.operationStatus = operationStatus;
    }
}
