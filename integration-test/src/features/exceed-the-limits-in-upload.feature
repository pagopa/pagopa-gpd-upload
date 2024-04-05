Feature: Exceed size limit in upload

  Background:
    Given GPD-Upload running

  Scenario: Create Debt Position by file upload KO: file size > limit size
    Given zip file with a size of 200 MB to be created
    When the client send file through POST to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 400

  Scenario: Update Debt Position by file upload KO: file size > limit size
    Given zip file with a size of 200 MB to be updated
    When the client send file through PUT to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 400

  Scenario: Delete Debt Position by file upload KO: file size > limit size
    Given zip file with a size of 200 MB to be deleted
    When the client send file through DELETE to /brokers/brokertest/organizations/ectest/debtpositions/file
    Then check statusCode is 400
