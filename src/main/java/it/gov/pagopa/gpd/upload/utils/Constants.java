package it.gov.pagopa.gpd.upload.utils;

public class Constants {
    private Constants(){}

    public static final String SERVICE_TYPE_METADATA = "serviceType";
    public static final String INPUT_DIRECTORY = "input";
    public static final String OUTPUT_DIRECTORY = "output";

    private static final String TAB = "&nbsp;&nbsp;&nbsp;&nbsp;";

    public static final String CREATE_UPDATE_FILE_DESCRIPTION = "ZIP File to be uploaded containing a JSON according to <pre><br>{<br>" +
            TAB + "\"paymentPositions\": [<br>" +
            TAB + "  {<br>" +
            TAB + TAB + "\"iupd\": \"string\",<br>" +
            TAB + TAB + "\"aca\": false,<br>" +
            TAB + TAB + "\"payStandIn\": false,<br>" +
            TAB + TAB + "\"type\": \"F\",<br>" +
            TAB + TAB + "\"fiscalCode\": \"string\",<br>" +
            TAB + TAB + "\"fullName\": \"string\",<br>" +
            TAB + TAB + "\"streetName\": \"string\",<br>" +
            TAB + TAB + "\"civicNumber\": \"string\",<br>" +
            TAB + TAB + "\"postalCode\": \"string\",<br>" +
            TAB + TAB + "\"city\": \"string\",<br>" +
            TAB + TAB + "\"province\": \"string\",<br>" +
            TAB + TAB + "\"region\": \"string\",<br>" +
            TAB + TAB + "\"country\": \"IT\",<br>" +
            TAB + TAB + "\"email\": \"string\",<br>" +
            TAB + TAB + "\"phone\": \"string\",<br>" +
            TAB + TAB + "\"switchToExpired\": false,<br>" +
            TAB + TAB + "\"companyName\": \"string\",<br>" +
            TAB + TAB + "\"officeName\": \"string\",<br>" +
            TAB + TAB + "\"validityDate\": \"YYYY-MM-DDThh:mm:ss.SSSZ\",<br>" +
            TAB + TAB + "\"paymentOption\": [<br>" +
            TAB + TAB + "  {<br>" +
            TAB + TAB + TAB + "\"iuv\": \"string\",<br>" +
            TAB + TAB + TAB + "\"amount\": 0,<br>" +
            TAB + TAB + TAB + "\"description\": \"string\",<br>" +
            TAB + TAB + TAB + "\"isPartialPayment\": true,<br>" +
            TAB + TAB + TAB + "\"dueDate\": \"YYYY-MM-DDThh:mm:ss.SSSZ\",<br>" +
            TAB + TAB + TAB + "\"retentionDate\": \"YYYY-MM-DDThh:mm:ss.SSSZ\",<br>" +
            TAB + TAB + TAB + "\"fee\": 0,<br>" +
            TAB + TAB + TAB + "\"transfer\": [<br>" +
            TAB + TAB + TAB + "  {<br>" +
            TAB + TAB + TAB + TAB + "\"idTransfer\": \"1\",<br>" +
            TAB + TAB + TAB + TAB + "\"amount\": 0,<br>" +
            TAB + TAB + TAB + TAB + "\"organizationFiscalCode\": \"00000000000\",<br>" +
            TAB + TAB + TAB + TAB + "\"remittanceInformation\": \"string\",<br>" +
            TAB + TAB + TAB + TAB + "\"category\": \"string\",<br>" +
            TAB + TAB + TAB + TAB + "\"iban\": \"IT0000000000000000000000000\",<br>" +
            TAB + TAB + TAB + TAB + "\"postalIban\": \"IT0000000000000000000000000\",<br>" +
            TAB + TAB + TAB + TAB + "\"stamp\": {<br>" +
            TAB + TAB + TAB + TAB + "  \"hashDocument\": \"string\",<br>" +
            TAB + TAB + TAB + TAB + "  \"stampType\": \"st\",<br>" +
            TAB + TAB + TAB + TAB + "  \"provincialResidence\": \"RM\"<br>" +
            TAB + TAB + TAB + TAB + "},<br>" +
            TAB + TAB + TAB + TAB + "\"transferMetadata\": [<br>" +
            TAB + TAB + TAB + TAB + "  {<br>" +
            TAB + TAB + TAB + TAB + TAB + "\"key\": \"string\",<br>" +
            TAB + TAB + TAB + TAB + TAB + "\"value\": \"string\"<br>" +
            TAB + TAB + TAB + TAB + "  }<br>" +
            TAB + TAB + TAB + TAB + "]<br>" +
            TAB + TAB + TAB + "  }<br>" +
            TAB + TAB + TAB + "],<br>" +
            TAB + TAB + TAB + "\"paymentOptionMetadata\": [<br>" +
            TAB + TAB + TAB + "  {<br>" +
            TAB + TAB + TAB + TAB + "\"key\": \"string\",<br>" +
            TAB + TAB + TAB + TAB + "\"value\": \"string\"<br>" +
            TAB + TAB + TAB + "  }<br>" +
            TAB + TAB + TAB + "]<br>" +
            TAB + TAB + "  }<br>" +
            TAB + TAB + "]<br>" +
            TAB + "  }<br>" +
            TAB + "]<br>" +
            "  }<br></pre>";

    public static final String DELETE_FILE_DESCRIPTION = "ZIP File to be uploaded containing a JSON according to <pre><br>" +
            "{<br>" +
            TAB + "\"paymentPositionIUPDs\": [<br>" +
            TAB + TAB +"\"IUPD-string\"<br>" +
            TAB + "]<br>" +
            "}<br>" +
            "<br>";
}
