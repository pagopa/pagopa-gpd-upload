import http from 'k6/http';
import {check, fail, sleep} from 'k6';
import {SharedArray} from 'k6/data';

export let options = JSON.parse(open(__ENV.TEST_TYPE));

// read configuration
// note: SharedArray can currently only be constructed inside init code
// according to https://k6.io/docs/javascript-api/k6-data/sharedarray
const varsArray = new SharedArray('vars', function () {
  return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});
// workaround to use shared array (only array should be used)
const vars = varsArray[0];
const rootUrl = `${vars.host}/${vars.basePath}`;

let zipFiles = {};
for (let i = 0; i < __ENV.FILE_NUMBER; i++) {
    let fileName = `./files/test${i}.json.zip`;
    zipFiles[i] = open(fileName, 'b');
}

export function setup() {
  // single upload, only needed to obtain the status URL
  // "/brokers/{broker}/organizations/{ec}/debtpositions/file"
  const data = {
    field: 'file',
    file: http.file(zipFiles[0], 'file.zip'),
  };
  const r = http.post(rootUrl, data, uploadParams);

  const uploadOk = check(r, { 'check file upload response is 202': (_r) => r.status === 202, }, { api: 'upload' });
  if (!uploadOk) {
    console.log(`status: ${r.status},\nbody: ${r.body}\n`);
    fail(`Upload failed with HTTP ${r.status}; cannot run status test.`);
  }

  const statusURL = r.headers['Location'];
  if (!statusURL) {
    console.log(`status: ${r.status},\nbody: ${r.body}\n`);
    fail('Upload response has no Location; cannot run status test.');
  }
  const statusUrl = `${vars.host}/${statusURL}`;

  return {statusUrl};
}

function precondition() {
  // no pre conditions
}

function postcondition() {
  // Delete the new entity created
}

const params = {
  headers: {
    'Ocp-Apim-Subscription-Key': __ENV.API_SUBSCRIPTION_KEY,
  },
};

// the "api" tag lets thresholds evaluate only the status API
const uploadParams = {headers: params.headers, tags: {api: 'upload', name: 'File Upload API'}};
const statusParams = {headers: params.headers, tags: {api: 'status', name: 'Status check API'}};

export default function (data) {
  const r = http.get(data.statusUrl, statusParams)

  const checkOutput = check(r, { 'check status check is 200': (_r) => r.status === 200, }, { api: 'status' });
  if (!checkOutput) {
    console.log(`status: ${r.status},\nbody: ${r.body}\n`);
  }

  postcondition();
}

export function teardown(data) {
  // single report retrieve on the uploaded file, excluded from thresholds (api tag "report")
  const reportUrl = data.statusUrl.replace("/status", "/report");
  const r = http.get(reportUrl, {headers: params.headers, tags: {api: 'report', name: 'Report retrieve API'}});

  const checkOutput = check(r, { 'check report retrieve is 200': (_r) => r.status === 200, }, { api: 'report' });
  if (!checkOutput) {
    console.log(`status: ${r.status},\nbody: ${r.body}\n`);
  }
}
