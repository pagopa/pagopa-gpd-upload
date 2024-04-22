const fs = require('fs');
const { v4: uuidv4 } = require('uuid');
const archiver = require('archiver');

class Transfer {
    constructor() {
        this.idTransfer = "1";
        this.amount = 100;
        this.remittanceInformation = uuidv4().substring(0, 10);
        this.category = "category-integration-test-gpd-upload";
        this.iban = "IT0000000000000000000000000";
        this.transferMetadata = [];
    }
}

class PaymentOption {
    constructor() {
        this.iuv = "IUV_GPD_UPLOAD_TEST" + uuidv4().substring(0, 11);
        this.amount = 100;
        this.isPartialPayment = false;
        this.description = "An example description"
        this.dueDate = new Date(Date.now() + 24 * 60 * 60 * 1000) .toISOString().slice(0, 23);; // Plus 1 day
        this.transfer = [new Transfer()];
        this.paymentOptionMetadata = [];
    }
}

class PaymentPosition {
    constructor(fiscalCode) {
        this.iupd = "IUPD_GPD_UPLOAD_TEST" + uuidv4().substring(0, 11);
        this.type = "F";
        this.fiscalCode = fiscalCode;
        this.fullName = "Full Name";
        this.companyName = "Company Name"
        this.paymentOption = [new PaymentOption()];
        this.switchToExpired = false;
    }
}

class PaymentPositions {
    constructor(fiscalCode, N) {
        this.paymentPositions = Array.from({ length: N }, () => new PaymentPosition(fiscalCode));
    }
}

class MutlipleIUPD {
    constructor(N) {
        this.paymentPositionIUPDs = Array.from({ length: N }, () => uuidv4().substring(0, 11));
    }
}

function generateAndWritePaymentPositions(N) {
    const fiscalCode = uuidv4().substring(0, 11);
    const paymentPositions = new PaymentPositions(fiscalCode, N);

    const jsonPP = JSON.stringify(paymentPositions, null, 2);
    const extender = uuidv4().substring(0, 4);
    const filename = `test${extender}.json`;

    fs.writeFileSync(filename, jsonPP);

    return filename;
}

function generateAndWriteValidMultipleIUPD(N) {
    const mutlipleIUPD = new MutlipleIUPD(N);
    const jsonPP = JSON.stringify(mutlipleIUPD, null, 2);
    const extender = uuidv4().substring(0, 4);
    const filename = `test${extender}.json`;

    fs.writeFileSync(filename, jsonPP);

    return filename;
}

function generateAndWriteInvalidMultipleIUPD(N) {
    const jsonPP = JSON.stringify({"name": "Aname"});
    const extender = uuidv4().substring(0, 4);
    const filename = `test${extender}.json`;

    fs.writeFileSync(filename, jsonPP);

    return filename;
}

function generateAndWriteInvalidPaymentPositions(N) {
    const fiscalCode = uuidv4().substring(0, 11);
    const paymentPositions = new PaymentPositions(fiscalCode, N);
    paymentPositions.paymentPositions.at(0).companyName = null;
    const jsonPP = JSON.stringify(paymentPositions, null, 2);
    const extender = uuidv4().substring(0, 4);
    const filename = `test${extender}.json`;

    fs.writeFileSync(filename, jsonPP);

    return filename;
}

async function zipFile(N) {
    filePath = generateAndWritePaymentPositions(N)

    const archive = archiver('zip', { zlib: { level: 9 } });
    const output = fs.createWriteStream(`${filePath}.zip`);

    archive.pipe(output);

    // Explicitly filter out .DS_Store file
    const stats = fs.statSync(filePath);
    if (stats.isFile() && !filePath.endsWith('/.DS_Store')) {
        archive.file(filePath, { name: 'payment-positions.json' });
    }

    await archive.finalize();

    return `${filePath}.zip`;
}

async function preparePayload(zip_type, N, pp_type, method) {
    if (method === 'create' || method === 'update') {
        switch (pp_type) {
            case 'INVALID':
                filePath = generateAndWriteInvalidPaymentPositions(N);
                break;
            case 'VALID':
                filePath = generateAndWritePaymentPositions(N);
                break;
            default:
                // Handle other cases if needed
                break;
        }
    } else if(method === 'delete') {
        switch (pp_type) {
            case 'INVALID':
                filePath = generateAndWriteInvalidMultipleIUPD(N);
                break;
            case 'VALID':
                filePath = generateAndWriteValidMultipleIUPD(N);
                break;
            default:
                // Handle other cases if needed
                break;
        }
    }

    return zipFileOptions(zip_type, N, filePath)
}

async function zipFileOptions(zip_type, N, JSONpath) {
    const archive = archiver('zip', { zlib: { level: 9 } });
    const output = fs.createWriteStream(`${JSONpath}.zip`);

    archive.pipe(output);
    const stats = fs.statSync(JSONpath);
    switch (zip_type) {
        case 'VALID':
            // Explicitly filter out .DS_Store file
            if (stats.isFile() && !JSONpath.endsWith('/.DS_Store')) {
                archive.file(JSONpath, { name: 'payment-positions.json' });
            }
            break;
        case 'INVALID_ENTRIES':
            if (stats.isFile()) {
                archive.file(JSONpath, { name: 'payment-positions.json' });
                // Add the second JSON file to the archive
                archive.append(JSON.stringify({ "name": "Aname", "age": 99 }), { name: 'file2.json' });
            }
            break;
        case 'INVALID_FORMAT':
            // Write content to the mock file
            fs.writeFile('invalid_format_file.txt', 'mock', (err) => {
                if (err) {
                    console.error('Error writing to file:', err);
                } else {
                    console.log(`Mock file '${JSONpath}' created successfully.`);
                }
            });
            // Add the mock file to the archive
            archive.file(JSONpath, { name: 'invalid_format_file.txt' });
            break;
        case 'EMPTY':
            // empty
            break;
        default:
            // Handle other cases if needed
            break;
    }

    await archive.finalize();

    return `${JSONpath}.zip`;
}

module.exports = { preparePayload, zipFile }
