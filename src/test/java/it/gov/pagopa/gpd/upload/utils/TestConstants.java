package it.gov.pagopa.gpd.upload.utils;

import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;

public class TestConstants {
    public static final String QUERY_PARAM_SERVICE_TYPE_GPD = String.format("?serviceType=%s", ServiceType.GPD.name());
    public static final String URI_V1 = "brokers/broker-ID/organizations/fiscal-code/debtpositions/file";
    public static final String URI_V2 = "v2/brokers/broker-ID/organizations/fiscal-code/debtpositions/file";

}
