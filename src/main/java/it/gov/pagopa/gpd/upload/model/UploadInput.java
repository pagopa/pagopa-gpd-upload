package it.gov.pagopa.gpd.upload.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UploadInput {
    @JsonProperty("operation")
    private UploadOperation uploadOperation;
    @Valid
    private List<@Valid PaymentPositionModel> paymentPositions;

    public @Valid List<@Valid PaymentPositionModel> getPaymentPositions() {
        return this.paymentPositions;
    }
}
