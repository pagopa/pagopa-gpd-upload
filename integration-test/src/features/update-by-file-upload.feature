Feature: UPDATE by file upload

  Background:
    Given GPD-Upload running

  Scenario: Update Debt Position by file upload OK
    Given zip file of 10 payment-position to be updated
    When the client send file through PUT to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 202
    And check location header regex ^brokers/(.+)/organizations/(.+)/debtpositions/file/(.+)/status$

  Scenario: Update Debt Position by file upload KO: invalid zip
    Given invalid zip file of 10 payment-position to be updated
    When the client send file through PUT to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 400

  Scenario: Update Debt Position by file upload KO: invalid content
    Given zip file of 10 invalid payment-position to be updated
    When the client send file through PUT to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 400
