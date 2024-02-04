const fs = require('fs');
const { v4: uuidv4 } = require('uuid');
const archiver = require('archiver');

class Transfer {
    constructor() {
        this.idTransfer = "1";
        this.amount = 100;
        this.remittanceInformation = uuidv4().substring(0, 10);
        this.category = "categoryXZ";
        this.iban = "IT0000000000000000000000000";
        this.transferMetadata = [];
    }
}

class PaymentOption {
    constructor() {
        this.iuv = "IUV_" + uuidv4().substring(0, 11);
        this.amount = 100;
        this.isPartialPayment = false;
        this.dueDate = new Date(Date.now() + 24 * 60 * 60 * 1000); // Plus 1 day
        this.transfer = [new Transfer()];
        this.paymentOptionMetadata = [];
    }
}

class PaymentPosition {
    constructor(fiscalCode) {
        this.iupd = "IUPD_" + uuidv4().substring(0, 11);
        this.type = "F";
        this.fiscalCode = fiscalCode;
        this.fullName = uuidv4().substring(0, 4);
        this.companyName = uuidv4().substring(0, 4);
        this.paymentOption = [new PaymentOption()];
        this.switchToExpired = false;
    }
}

class PaymentPositions {
    constructor(fiscalCode, N) {
        this.paymentPositions = Array.from({ length: N }, () => new PaymentPosition(fiscalCode));
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

module.exports = { zipFile }
