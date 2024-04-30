Feature: concatenate CRUD by file upload

  Background:
    Given GPD-Upload is running

  Scenario: Create and Update same Debt Positions by file upload OK
    Given zip file of 1 payment-positions
    # Create
    When the client send file through POST to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 201
    # Update
    When the client send file through PUT to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 200

  Scenario: Create and Delete same Debt Positions by file upload OK
    Given zip file of 1 payment-positions
    # Create
    When the client send file through POST to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 201
    # Extract multiple-IUPD model
    Given zip file of 1 IUPD from payment-positons zip file
    # Delete
    When the client send file through DELETE to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 200

  Scenario: Create, Update and Delete same Debt Positions by file upload OK
    Given zip file of 1 payment-positions
    # Create
    When the client send file through POST to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 201
    # Update
    When the client send file through PUT to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 200
    # Extract multiple-IUPD model
    Given zip file of 1 IUPD from payment-positons zip file
    # Delete
    When the client send file through DELETE to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 200

  Scenario: Delete Debt Positions that doesn't exist
    Given zip file of 1 payment-positions
    # Extract multiple-IUPD model
    Given zip file of 1 IUPD from payment-positons zip file
    # Delete
    When the client send file through DELETE to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 404

  Scenario: Update Debt Positions that doesn't exist
    Given zip file of 1 payment-positions
    # Update
    When the client send file through PUT to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 404

  Scenario: Create Debt Positions that already exist
    Given zip file of 1 payment-positions
    # Create
    When the client send file through POST to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 201
    # Create
    When the client send file through POST to /brokers/brokertest/organizations/77777777777/debtpositions/file
    And check statusCode is 202
    And upload UID is been extracted from location header
    And the upload of brokertest and 77777777777 related to UID is completed
    # GET report
    And the client send GET to /brokers/brokertest/organizations/77777777777/debtpositions/file/UID/report
    And check statusCode is 200
    Then body contains the field processedItem valued 1
    And body contains the path field responses statusCode valued 409
    