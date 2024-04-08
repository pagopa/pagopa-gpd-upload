Feature: UPDATE by file upload

  Background:
    Given GPD-Upload running

  Scenario: Update Debt Positions by file upload OK
    Given zip file of 10 payment-positions to be updated
    When the client send file through PUT to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 202
    And check location header regex ^brokers/(.+)/organizations/(.+)/debtpositions/file/(.+)/status$

  Scenario: Create Debt Positions by file upload KO: invalid zip with 2 file entries
    Given invalid zip file of 10 payment-positions to be created
    When the client send file through PUT to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 400

  Scenario: Create Debt Positions by file upload KO: invalid zip containing a file with a format different from JSON
    Given invalid zip file of 10 payment-positions to be created
    When the client send file through PUT to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 400

  Scenario: Update Debt Positions by file upload KO: invalid content, one test for all invalid and required Debt Position fields
    Given zip file of 10 invalid payment-positions to be updated
    When the client send file through PUT to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 400
