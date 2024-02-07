Feature: upload report check

  Background:
    Given GPD-Upload running
    And zip file of 50000 payment-position
    And the client send file through POST to /brokers/brokertest/organizations/ectest/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header

  Scenario: Upload report check OK
    When the upload of brokertest and ectest related to UID is completed
    And the client send GET to /brokers/brokertest/organizations/ectest/debtpositions/file/UID/report
    Then check statusCode is 200
    And body contains the following fields:
      | uploadID        |
      | processedItem   |
      | submittedItem   |
      | responses       |
      | endTime         |
      | startTime       |
