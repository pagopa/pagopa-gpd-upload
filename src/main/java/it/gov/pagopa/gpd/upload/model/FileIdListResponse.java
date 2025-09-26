package it.gov.pagopa.gpd.upload.model;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
public class FileIdListResponse {
    @Schema(description = "List of file identifiers")
    private List<String> fileIds;

    @Schema(description = "Number of items in this page")
    private int size;

    @Schema(description = "True if another page is available (x-continuation-token present)")
    private boolean hasMore;

    @Schema(hidden = true) // do not put it in the body; it is put in the output header
    @JsonIgnore
    private String continuationToken;
}
