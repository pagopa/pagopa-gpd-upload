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
// organizations pool: spreads the load across multiple Cosmos DB partitions
// If the environment does not define "organizations", the single organization in basePath is used.
const organizations = (vars.organizations && vars.organizations.length > 0) ? vars.organizations : [null];
const buildUploadUrl = (org) => `${vars.host}/${org ? vars.basePath.replace('{organization}', org) : vars.basePath}`;
let zipFiles = {};
for (let i = 0; i < __ENV.FILE_NUMBER; i++) {
    let fileName = `./files/test${i}.json.zip`;
    zipFiles[i] = open(fileName, 'b');
}

export function setup() {
  // one upload per organization, only needed to obtain the status URLs
  // "/brokers/{broker}/organizations/{ec}/debtpositions/file"
  const statusUrls = [];
  organizations.forEach((org, i) => {
    const data = {
      field: 'file',
      file: http.file(zipFiles[i % __ENV.FILE_NUMBER], 'file.zip'),
    };
    const r = http.post(buildUploadUrl(org), data, uploadParams);
    const uploadOk = check(r, { 'check file upload response is 202': (_r) => _r.status === 202, }, { api: 'upload' });
    if (!uploadOk) {
      console.log(`organization: ${org}, status: ${r.status},\nbody: ${r.body}\n`);
      fail(`Upload failed with HTTP ${r.status} for organization ${org}; cannot run status test.`);
    }
    const statusURL = r.headers['Location'];
    if (!statusURL) {
      console.log(`organization: ${org}, status: ${r.status},\nbody: ${r.body}\n`);
      fail(`Upload response has no Location for organization ${org}; cannot run status test.`);
    }
    statusUrls.push(`${vars.host}/${statusURL}`);
  });
  return {statusUrls};
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
const statusParams = {
  headers: params.headers,
  tags: {api: 'status', name: 'Status check API'},
};

export default function (data) {
  // random organization at each iteration to avoid hitting a single partition
  const statusUrl = data.statusUrls[Math.floor(Math.random() * data.statusUrls.length)];
  const r = http.get(statusUrl, statusParams)

  const checkOutput = check(r, { 'check status check is 200': (_r) => _r.status === 200, }, { api: 'status' });
  if (!checkOutput) {
    console.log(`status: ${r.status},\nbody: ${r.body}\n`);
  }

  postcondition();
}

export function teardown(data) {
  // single report retrieve on the first uploaded file, excluded from thresholds (api tag "report")
  const reportUrl = data.statusUrls[0].replace("/status", "/report");
  const r = http.get(reportUrl, {headers: params.headers, tags: {api: 'report', name: 'Report retrieve API'}});

  const checkOutput = check(r, { 'check report retrieve is 200': (_r) => r.status === 200, }, { api: 'report' });
  if (!checkOutput) {
    console.log(`status: ${r.status},\nbody: ${r.body}\n`);
  }
}
