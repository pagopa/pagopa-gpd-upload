package it.gov.pagopa.gpd.upload.model.pd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import it.gov.pagopa.gpd.upload.model.pd.enumeration.DebtPositionStatus;
import it.gov.pagopa.gpd.upload.model.pd.enumeration.Type;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Introspected
public class PaymentPositionModel implements Serializable {

    /**
     * generated serialVersionUID
     */
    private static final long serialVersionUID = 1509046053787358148L;


    @NotBlank(message = "iupd is required")
    private String iupd;
    @NotNull(message = "type is required")
    private Type type;
    @Schema(description = "feature flag to enable a debt position in stand-in mode", example = "true", defaultValue = "true")
    private boolean payStandIn;
    @NotBlank(message = "fiscal code is required")
    private String fiscalCode;
    @NotBlank(message = "full name is required")
    private String fullName;
    private String streetName;
    private String civicNumber;
    private String postalCode;
    private String city;
    private String province;
    private String region;
    @Pattern(regexp="[A-Z]{2}", message="The country must be reported with two capital letters (example: IT)")
    private String country;
    @Email(message = "Please provide a valid email address")
    private String email;
    private String phone;
    @Schema(description = "feature flag to enable the debt position to expire after the due date", example = "false", defaultValue = "false")
    @NotNull(message = "switch to expired value is required")
    private Boolean switchToExpired;

    // Payment Position properties
    @NotBlank(message = "company name is required")
    @Size(max = 140) // compliant to paForNode.xsd
    private String companyName; // es. Comune di Roma
    @Size(max = 140) // compliant to paForNode.xsd
    private String officeName; // es. Ufficio Tributi
    private LocalDateTime validityDate;
    @JsonProperty(access = Access.READ_ONLY)
    private LocalDateTime paymentDate;
    @JsonProperty(access = Access.READ_ONLY)
    private DebtPositionStatus status;

    @Valid
    private List<@Valid PaymentOptionModel> paymentOption = new ArrayList<>();

    public void addPaymentOptions(PaymentOptionModel paymentOpt) {
        paymentOption.add(paymentOpt);
    }

    public void removePaymentOptions(PaymentOptionModel paymentOpt) {
        paymentOption.remove(paymentOpt);
    }
}
