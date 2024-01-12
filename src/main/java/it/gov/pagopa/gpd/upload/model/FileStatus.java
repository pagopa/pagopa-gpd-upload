package it.gov.pagopa.gpd.upload.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FileStatus {
    public String fileId;
    public int processed;
    public int uploaded;
    private ArrayList<String> successIUPD;
    private ArrayList<String> failedIUPD;
    private LocalDateTime uploadTime;
}
