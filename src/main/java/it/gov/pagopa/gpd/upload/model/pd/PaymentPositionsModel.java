package it.gov.pagopa.gpd.upload.model.pd;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.Valid;
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
public class PaymentPositionsModel {
    @Valid
    @Size(min = 1, message = "The list of payment positions must contain at least one element")
    private List<@Valid PaymentPositionModel> paymentPositions;

    public @Valid List<@Valid PaymentPositionModel> getPaymentPositions() {
        return this.paymentPositions;
    }
}
