const {Given, When, Then, setDefaultTimeout} = require('@cucumber/cucumber')
const assert = require("assert");
const {zipFile, preparePayload} = require('./data')
const {call, get, sleep} = require('./common')
const {uploadFile} = require('./service')
const fs = require("fs");

const app_host = process.env.APP_HOST

setDefaultTimeout(3600 * 1000);

let filePath;
let responseToCheck;
let UID;


Given(/^zip file of (.*) payment-positions$/, async function (n) {
    filePath = await zipFile(n);
});

/**
 * This method allows the creation of the VALID or INVALID file zip payload
 * to be sent via POST, PUT and DELETE operations depending on the contents
 * of the file and the operation required by the test.
 * @param {string} zip_type - The type of zip file {VALID, INVALID_ENTRIES, INVALID_FORMAT}
 * @param {int} N - The number of payment positions or IUPD
 * @param {string} pp_type - The type of payment positions JSON payload {VALID, INVALID}
 * @param {string} method - The method under test {POST, PUT, DELETE}
 */
Given(/^(.*) zip file of (.*) (.*) payment-positions to be (.*)$/, async function (zip_type, n, pp_type, method) {
    filePath = await preparePayload(zip_type, n, pp_type, method.substring(0, method.length - 1));
});

Given(/^GPD-Upload is running$/, async function () {
    await sleep(10000) // prevent rate-limit response
    let r = await get(app_host + '/info')
    assert.strictEqual(r.status, 200, r);
});

Given(/^upload UID is been extracted from (.*) header$/, async function (headerKey) {
    const header_value = responseToCheck.headers[headerKey];
    const match = header_value.match(/file\/(.*?)\/status/);
    UID = match ? match[1] : null;
});


When(/^the client send file through (GET|POST|PUT|DELETE) to (.*)$/,
    async function (method, url) {
        await sleep(10000) // prevent rate-limit response
        responseToCheck = await uploadFile(app_host + url, filePath, method)
    }
);

When(/^the client send (GET|POST|PUT|DELETE) to (.*)$/,
    async function (method, url) {
        if (url.includes("UID"))
            url = url.replace("UID", UID);
        else console.log("URL does not contain 'UID'");

        console.log("call " + method + " to " + url);
        await sleep(10000) // prevent rate-limit response
        responseToCheck = await call(method, app_host + url, null);
    }
);

When(/^the upload of (.*) and (.*) related to UID is completed$/,
    async function (broker, ec) {
        await sleep(10000) // prevent rate-limit response
        let status_url = "/brokers/"+broker+"/organizations/"+ec+"/debtpositions/file/"+UID+"/status"
        let status_response = await call('GET', app_host + status_url, null);
        completed = false;
        const start_time = Date.now();

        while(status_response.data.processedItem - status_response.data.submittedItem !== 0) {
            await sleep(10000) // prevent rate-limit response
            status_response = await call('GET', app_host + status_url, null);
        }

        console.log("Upload duration: " + (Date.now() - start_time))
    }
);

Then(/^check statusCode is (\d+)$/, function (status) {
    assert.strictEqual(responseToCheck.status, status, responseToCheck);
});

Then(/^body contains the following fields:$/, function (dataTable) {
    const data = dataTable.rows();
    data.forEach(row => {
        row.forEach(cell => {
            assert.ok(responseToCheck.data.cell !== null, "Value " + cell + " should not be null");
        });
    });
});

Then(/^body contains the field (.*) valued (\d+)$/, function (field, value) {
    assert.strictEqual(responseToCheck.data[field], value, responseToCheck);
});

Then(/^body contains the path field (.*) 0 (.*) valued (\d+)$/, function (field1, field2, value) {
    assert.strictEqual(responseToCheck.data[field1][0][field2], value, responseToCheck);
});

Then(/^check (.*) header regex (.*)$/, function (headerKey, expectedPattern) {
  const header = responseToCheck.headers[headerKey];
  const patternRegex = new RegExp(expectedPattern);
  assert.ok(patternRegex.test(header), 'Location header does not match the expected pattern.');
});
