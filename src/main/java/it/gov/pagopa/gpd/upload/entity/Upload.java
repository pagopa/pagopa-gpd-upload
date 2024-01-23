package it.gov.pagopa.gpd.upload.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize
@ToString
public class Upload {
    private int current;
    private int total;
    private ArrayList<ResponseEntry> responses;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Schema(example = "2024-10-08T14:55:16.302Z")
    private LocalDateTime start;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Schema(example = "2024-10-08T14:55:16.302Z")
    private LocalDateTime end;

    public void addResponse(ResponseEntry responseEntry) {
        for (ResponseEntry existingEntry : responses) {
            if (existingEntry.getStatusCode().equals(responseEntry.getStatusCode())
                        && existingEntry.getStatusMessage().equals(responseEntry.getStatusMessage())) {
                List<String> requestIDs = new ArrayList<>(existingEntry.getRequestIDs());
                requestIDs.addAll(responseEntry.getRequestIDs());
                existingEntry.setRequestIDs(requestIDs);
                return; // No need to continue checking once a match is found
            }
        }
        // If no match is found, add the new response entry to the list
        responses.add(responseEntry);
    }
}
