package it.gov.pagopa.gpd.upload.model.pd;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Introspected
public class PaymentOptionModel implements Serializable {

    /**
     * generated serialVersionUID
     */
    private static final long serialVersionUID = -8328320637402363721L;

    private String nav;
    @NotBlank(message = "iuv is required")
    private String iuv;
    @NotNull(message = "amount is required")
    @Min(value= 1L, message = "minimum amount is 1 eurocent")
    private Long amount;
    @NotBlank(message = "payment option description is required")
    @Size(max = 140) // compliant to paForNode.xsd
    private String description;
    @NotNull(message = "is partial payment is required")
    private Boolean isPartialPayment;
    @NotNull(message = "due date is required")
    private LocalDateTime dueDate;
    private LocalDateTime retentionDate;
    private long fee;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private long notificationFee;

    @Valid
    private List<TransferModel> transfer = new ArrayList<>();

    @Valid
    @Size(min=0, max=10)
    @Schema(description = "it can added a maximum of 10 key-value pairs for metadata")
    private List<PaymentOptionMetadataModel> paymentOptionMetadata = new ArrayList<>();

    public void addTransfers(TransferModel trans) {
        transfer.add(trans);
    }

    public void removeTransfers(TransferModel trans) {
        transfer.remove(trans);
    }

    public void addPaymentOptionMetadata(PaymentOptionMetadataModel paymentOptMetadata) {
        paymentOptionMetadata.add(paymentOptMetadata);
    }

    public void removePaymentOptionMetadata(PaymentOptionMetadataModel paymentOptMetadata) {
        paymentOptionMetadata.remove(paymentOptMetadata);
    }
}
