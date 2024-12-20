package it.gov.pagopa.gpd.upload.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    private LocalDateTime start;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;

    public void addResponse(ResponseEntry responseEntry) {
        if(responses == null) responses = new ArrayList<>();
        // regardless of the condition increments the current counter because a list of IUPDs was processed
        current += responseEntry.requestIDs.size();
        for (ResponseEntry existingEntry : responses) {
            if (existingEntry.statusCode.equals(responseEntry.statusCode) && existingEntry.statusMessage.equals(responseEntry.statusMessage)) {
                List<String> requestIDs = new ArrayList<>(existingEntry.requestIDs);
                requestIDs.addAll(responseEntry.requestIDs);
                existingEntry.requestIDs = requestIDs;
                return; // No need to continue checking once a match is found
            }
        }
        // If no match is found, add the new response entry to the list
        responses.add(responseEntry);
    }
}
