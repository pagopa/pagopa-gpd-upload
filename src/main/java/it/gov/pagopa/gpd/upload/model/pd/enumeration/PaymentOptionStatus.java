package it.gov.pagopa.gpd.upload.model.pd.enumeration;

import io.micronaut.core.annotation.Introspected;

@Introspected
public enum PaymentOptionStatus {
    PO_UNPAID, PO_PAID, PO_PARTIALLY_REPORTED, PO_REPORTED
}
