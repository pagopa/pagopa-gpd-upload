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
        this.companyName = "Integration Test Company";
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

    return [filename, jsonPP]
}

function generateAndWriteInvalidPaymentPositions(N) {
    const fiscalCode = uuidv4().substring(0, 11);
    const paymentPositions = new PaymentPositions(fiscalCode, N);
    for(i in paymentPositions.paymentPositions) {
        paymentPositions.paymentPositions.at(i).companyName = null
    }
    const jsonPP = JSON.stringify(paymentPositions, null, 2);
    const extender = uuidv4().substring(0, 4);
    const filename = `test${extender}.json`;

    fs.writeFileSync(filename, jsonPP);

    return [filename, jsonPP];
}

function generateAndWriteValidMultipleIUPD(item_number, iupd_array) {
    let mutlipleIUPD = new MutlipleIUPD(item_number);

    if(iupd_array) { // !(undefined, null, empty string, 0, NaN, false)
        mutlipleIUPD.paymentPositionIUPDs = iupd_array;
    }
    const jsonPP = JSON.stringify(mutlipleIUPD, null, 2);
    const extender = uuidv4().substring(0, 4);
    const filename = `test${extender}.json`;

    fs.writeFileSync(filename, jsonPP);

    return [filename, jsonPP];
}

function generateAndWriteInvalidMultipleIUPD(N) {
    const jsonPP = JSON.stringify({"name": "John"});
    const extender = uuidv4().substring(0, 4);
    const filename = `test${extender}.json`;

    fs.writeFileSync(filename, jsonPP);

    return [filename, jsonPP];
}

async function extractIUPDs(filePath) {
    iupds = []

    try {
        const payload = fs.readFileSync(filePath, 'utf8');
        const data = JSON.parse(payload)
        pps = data.paymentPositions;
        for (key in pps) {
            pp = pps[key]
            iupds[key] = pp.iupd
        }
    } catch (err) {
        console.error('Error while parsing JSON data:', err)
    }

    return iupds;
}

async function generate_zip_with_type(item_number, zip_type, pp_type, method, iupds) {
    console.log(`Zip generation: {zip-type = ${zip_type}, pp-type = ${pp_type}, method = ${method}}`)
    if (method === 'create' || method === 'update') {
        switch (pp_type) {
            case 'INVALID':
                filePath = generateAndWriteInvalidPaymentPositions(item_number)[0];
                break;
            case 'VALID':
                filePath = generateAndWritePaymentPositions(item_number)[0];
                break;
            default:
                // Handle other cases if needed
                break;
        }
    } else if(method === 'delete') {
        switch (pp_type) {
            case 'INVALID':
                filePath = generateAndWriteInvalidMultipleIUPD(item_number)[0];
                break;
            case 'VALID':
                filePath = generateAndWriteValidMultipleIUPD(item_number, iupds)[0];
                break;
            default:
                // Handle other cases if needed
                break;
        }
    }

    return zip_with_options(zip_type, item_number, filePath)
}

async function zip_with_options(zip_type, N, JSONpath) {
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
                archive.append(JSON.stringify({ "name": "John", "age": 99 }), { name: 'file2.json' });
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

async function zip(filePath) {
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

async function zip_by_content(json_content) {
    const file_name = `test_${uuidv4().substring(0, 10)}.zip`;
    const archive = archiver('zip', { zlib: { level: 9 } });
    const output = fs.createWriteStream(file_name);

    archive.pipe(output);
    archive.append(json_content, { name: 'data.json' });
    archive.finalize();

    return file_name
}

async function mock_zip(sizeMB) {
    const file_name = "./file.zip"
    const archive = archiver('zip', { zlib: { level: 0 } });
    const output = fs.createWriteStream(file_name);
    const dummyContent = Buffer.alloc(1024 * 1024, 'MOCK');

    archive.pipe(output);

    const fileSize = sizeMB * 1024 * 1024;
    for (let i = 0; i < fileSize / dummyContent.length; i++) {
        archive.append(dummyContent, { name: `file${i}.txt` });
    }

    await archive.finalize();

    return file_name
}

module.exports = { zip, mock_zip, zip_by_content, extractIUPDs, generate_zip_with_type, generateAndWritePaymentPositions, generateAndWriteValidMultipleIUPD }
