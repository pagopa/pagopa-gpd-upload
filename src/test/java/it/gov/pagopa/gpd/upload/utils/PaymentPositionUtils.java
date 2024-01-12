package it.gov.pagopa.gpd.upload.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.gpd.upload.model.pd.PaymentOptionModel;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionsModel;
import it.gov.pagopa.gpd.upload.model.pd.TransferModel;
import it.gov.pagopa.gpd.upload.model.pd.enumeration.Type;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaymentPositionUtils {

    public static String createPaymentPositionsJSON(String fiscalCode, int n) throws JsonProcessingException {
        List<PaymentPositionModel> paymentPositionList = new ArrayList<>();
        for(int i = 0; i < n; i++) {
            String ID = fiscalCode + "_" + UUID.randomUUID().toString().substring(0,5);
            TransferModel tf = TransferModel.builder()
                    .idTransfer("1")
                    .amount(100L)
                    .remittanceInformation("remittance information")
                    .category("categoryXZ")
                    .iban("IT0000000000000000000000000")
                    .transferMetadata(new ArrayList<>())
                    .build();
            List<TransferModel> transferList = new ArrayList<TransferModel>();
            transferList.add(tf);
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.now().plus(1, ChronoUnit.DAYS), ZoneId.of("UTC"));
            PaymentOptionModel po = PaymentOptionModel.builder()
                    .iuv("IUV_" + ID)
                    .amount(100L)
                    .isPartialPayment(false)
                    .dueDate(zonedDateTime.toLocalDateTime())
                    .transfer(transferList)
                    .paymentOptionMetadata(new ArrayList<>())
                    .build();
            List<PaymentOptionModel> paymentOptionList = new ArrayList<PaymentOptionModel>();
            paymentOptionList.add(po);
            PaymentPositionModel pp = PaymentPositionModel.builder()
                    .iupd("IUPD_" + ID)
                    .type(Type.F)
                    .fiscalCode(fiscalCode)
                    .email("malformed_email")
                    .fullName("Mario Rossi")
                    .companyName("Company Name")
                    .paymentOption(paymentOptionList)
                    .switchToExpired(false)
                    .build();
            paymentPositionList.add(pp);
        }

        PaymentPositionsModel paymentPositions = PaymentPositionsModel.builder()
                .paymentPositions(paymentPositionList)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);

        return objectMapper.writeValueAsString(paymentPositions);
    }
}
