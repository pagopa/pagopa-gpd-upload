package it.gov.pagopa.gpd.upload.model.pd;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Introspected
public class PaymentOptionMetadataModel implements Serializable {

    /**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 4575041445781686511L;

	@NotBlank(message = "key is required")
    private String key;

    private String value;
}
