package it.gov.pagopa.gpd.upload.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import lombok.*;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize
@ToString
public class Status {
    private String id;
    private String brokerID;
    public String fiscalCode;
    public Upload upload;
    private ServiceType serviceType;
}
