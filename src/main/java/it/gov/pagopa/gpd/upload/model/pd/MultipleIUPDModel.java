package it.gov.pagopa.gpd.upload.model.pd;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Introspected
public class MultipleIUPDModel {

    @NotEmpty
    @Size(max = 100000)
    @NotNull
    private List<String> paymentPositionIUPDs;
}
