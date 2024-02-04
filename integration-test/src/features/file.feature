Feature: file handling: { POST }

  Background:
    """Given info ok"""

  Scenario: File upload
    Given zip file of 100 payment-position
    When the client send POST to /brokers/btest/organizations/ectest/debtpositions/file
    Then check statusCode is 202
    And check location header regex ^brokers/(.+)/organizations/(.+)/debtpositions/file/(.+)/status$
