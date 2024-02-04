const {Given, When, Then} = require('@cucumber/cucumber')
const assert = require("assert");
const {zipFile} = require('./data')
const {uploadFile} = require('./common')
const fs = require("fs");

const app_host = process.env.APP_HOST

let filePath;
let responseToCheck;

Given(/^zip file of (.*) payment-position$/, async function (n) {
    filePath = await zipFile(n);
});

When(/^the client send (GET|POST|PUT|DELETE) to (.*)$/,
    async function (method, url) {
        responseToCheck = await uploadFile(app_host + url, filePath)
    });

Then(/^check statusCode is (\d+)$/, function (status) {
    assert.strictEqual(responseToCheck.status, status, responseToCheck);
});

Then(/^check (.*) header regex (.*)$/, function (headerKey, expectedPattern) {
  const header = responseToCheck.headers[headerKey];
  const patternRegex = new RegExp(expectedPattern);
  assert.ok(patternRegex.test(header), 'Location header does not match the expected pattern.');
});
