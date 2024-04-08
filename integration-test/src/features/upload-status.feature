Feature: upload status check

  Background:
    Given GPD-Upload running
    And zip file of 10 payment-positions
    And the client send file through POST to /brokers/brokertest/organizations/ectest/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header

  Scenario: Upload status check OK
    When the client send GET to /brokers/brokertest/organizations/ectest/debtpositions/file/UID/status
    Then check statusCode is 200
    And body contains the following fields:
      | uploadID        |
      | processedItem   |
      | submittedItem   |
      | startTime       |


