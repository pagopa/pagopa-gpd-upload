Feature: DELETE by file upload

  Background:
    Given GPD-Upload is running

  Scenario: Delete Debt Positions by file upload OK
    Given VALID zip file of 2 VALID payment-positions to be deleted
    When the client send file through DELETE to /brokers/brokertest/organizations/77777777777/debtpositions/file
    Then check statusCode is 202
    And check location header regex ^brokers/(.+)/organizations/(.+)/debtpositions/file/(.+)/status$

  Scenario: Delete Debt Positions by file upload KO: invalid zip with 2 file entries
    Given INVALID_ENTRIES zip file of 2 VALID payment-positions to be deleted
    When the client send file through DELETE to /brokers/brokertest/organizations/77777777777/debtpositions/file
    Then check statusCode is 400

  Scenario: Delete Debt Positions by file upload KO: invalid zip containing a file with a format different from JSON
    Given INVALID_FORMAT zip file of 2 VALID payment-positions to be deleted
    When the client send file through DELETE to /brokers/brokertest/organizations/77777777777/debtpositions/file
    Then check statusCode is 400

  Scenario: Delete Debt Positions by file upload KO: zip file is missing
    Given EMPTY zip file of 2 VALID payment-positions to be deleted
    When the client send file through DELETE to /brokers/brokertest/organizations/77777777777/debtpositions/file
    Then check statusCode is 400

  Scenario: Delete Debt Positions by file upload KO: invalid content, one test for all invalid and required Debt Position fields
    Given VALID zip file of 2 INVALID payment-positions to be deleted
    When the client send file through DELETE to /brokers/brokertest/organizations/77777777777/debtpositions/file
    Then check statusCode is 400
