package it.gov.pagopa.gpd.upload.model.pd;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class TransferMetadataModel implements Serializable {

    
    /**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = -1509450417943158597L;

	@NotBlank(message = "key is required")
    private String key;

    private String value;

}
