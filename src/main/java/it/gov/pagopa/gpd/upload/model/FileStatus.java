package it.gov.pagopa.gpd.upload.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileStatus {
    public String fileId;
    private ArrayList<String> successIUPD;
    private ArrayList<String> failedIUPD;
    private LocalDateTime uploadTime;
}
