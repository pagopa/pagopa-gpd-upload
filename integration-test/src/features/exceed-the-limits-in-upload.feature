Feature: Request Entity Too Large: Exceed size limit in upload

  Background:
    Given GPD-Upload is running

  Scenario: Request Entity Too Large: Create Debt Positions by file upload KO: file size > limit size
    Given mock zip file with a size of 105 MB to be created
    When the client send file through POST to /brokers/brokertest/organizations/77777777777/debtpositions/file
    Then check statusCode is 413

  Scenario: Request Entity Too Large: Update Debt Positions by file upload KO: file size > limit size
    Given mock zip file with a size of 105 MB to be updated
    When the client send file through PUT to /brokers/brokertest/organizations/77777777777/debtpositions/file
    Then check statusCode is 413

  Scenario: Request Entity Too Large: Delete Debt Positions by file upload KO: file size > limit size
    Given mock zip file with a size of 105 MB to be deleted
    When the client send file through DELETE to /brokers/brokertest/organizations/77777777777/debtpositions/file
    Then check statusCode is 413
