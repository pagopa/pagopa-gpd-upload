package it.gov.pagopa.gpd.upload.model.pd;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Introspected
public class TransferModel implements Serializable {

    /**
     * generated serialVersionUID
     */
    private static final long serialVersionUID = 5593063492841435180L;

    @NotBlank(message = "id transfer is required")
    @Schema(type = "string", allowableValues = {"1", "2", "3", "4", "5"})
    private String idTransfer;

    @NotNull(message = "amount is required")
    private Long amount;

    @Schema(description = "Fiscal code related to the organization targeted by this transfer.", example = "00000000000")
    private String organizationFiscalCode;

    @NotBlank(message = "remittance information is required")
    private String remittanceInformation; // causale

    @NotBlank(message = "category is required")
    private String category; // taxonomy

    @Schema(description = "mutual exclusive with postalIban and stamp", example = "IT0000000000000000000000000")
    private String iban;

    @Schema(description = "mutual exclusive with iban and stamp", example = "IT0000000000000000000000000")
    private String postalIban;

    @Schema(description = "mutual exclusive with iban and postalIban")
    private Stamp stamp;
    
    @Valid
    @Size(min=0, max=10)
    @Schema(description = "it can added a maximum of 10 key-value pairs for metadata")
    private List<TransferMetadataModel> transferMetadata = new ArrayList<>();

    public void addTransferMetadata(TransferMetadataModel trans) {
    	transferMetadata.add(trans);
    }

    public void removeTransferMetadata(TransferMetadataModel trans) {
    	transferMetadata.remove(trans);
    }

}
