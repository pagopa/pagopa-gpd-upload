Feature: file handling: { POST }

  Background:
    Given GPD-Upload running

  Scenario: File upload OK
    Given zip file of 10 payment-position
    When the client send file through POST to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 202
    And check location header regex ^brokers/(.+)/organizations/(.+)/debtpositions/file/(.+)/status$
