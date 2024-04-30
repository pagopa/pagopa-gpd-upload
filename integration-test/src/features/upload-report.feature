Feature: upload report check

  Background:
    Given GPD-Upload is running
    And zip file of 10 payment-positions
    And the client send file through POST to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header

  Scenario: Upload report check OK
    When the upload of brokertest and 77777777777 related to UID is completed
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    Then check statusCode is 200
    And body contains the following fields:
      | uploadID        |
      | processedItem   |
      | submittedItem   |
      | responses       |
      | endTime         |
      | startTime       |
