package it.gov.pagopa.gpd.upload.model.pd;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class PaymentOptionMetadataModel implements Serializable {

    /**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 4575041445781686511L;

	@NotBlank(message = "key is required")
    private String key;

    private String value;
}
