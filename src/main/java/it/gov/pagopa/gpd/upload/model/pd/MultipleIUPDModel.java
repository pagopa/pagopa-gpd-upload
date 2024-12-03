package it.gov.pagopa.gpd.upload.model.pd;

import io.micronaut.core.annotation.Introspected;
import it.gov.pagopa.gpd.upload.config.duplicate.NoDuplicate;
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

    private static final int MAX_IUPD = 100000;

    @NotEmpty
    @Size(min = 1, max = 100000, message = "The list of payment positions IUPD must contain at least one element and at the most " + MAX_IUPD)
    @NotNull
    @NoDuplicate
    private List<String> paymentPositionIUPDs;
}
