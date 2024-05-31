import http from 'k6/http';
import exec from 'k6/execution';
import {check, sleep} from 'k6';
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
  // The setup code runs, setting up the test environment (optional) and generating data
  // used to reuse code for the same VU
  // precondition is moved to default fn because in this stage
  // __VU is always 0 and cannot be used to create env properly
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


export default function () {

  let test_id = exec.scenario.iterationInInstance % __ENV.FILE_NUMBER;

  console.log("Run perfromance scenario for user: " + test_id)

  let tag = {
  };

  // "/brokers/{broker}/organizations/{ec}/debtpositions/file"
  let url = `${rootUrl}`;

  const data = {
    field: 'file',
    file: http.file(zipFiles[test_id], 'file.zip'),
  };

  let r = http.post(url, data, params);
  let checkOutput = check(r, { 'check file upload response is 202': (_r) => r.status === 202, }, { tag: "File Upload API" });

  if (!checkOutput) {
    console.log(`status: ${r.status},\nbody: ${r.json.body}\n`)
  }

  let statusURL = r.headers['Location']
  console.log(`url-upload-key: ${statusURL}`)
  r = callStatus(statusURL)

  while(r.json().processedItem !== r.json().submittedItem) {
    r = callStatus(statusURL)
  }

  sleep(1)
  let reportURL = statusURL.replace("/status", "/report")
  r = http.get(`${vars.host}/${reportURL}`, params)

  checkOutput =   check(r, { 'check report retrieve is 200': (_r) => r.status === 200, }, tag = "Report retrieve API");
  if (!checkOutput) {
    console.log(`status: ${r.status},\nbody: ${r.json.body}\n`)
  }

  postcondition();
}

function callStatus(statusURL) {
  sleep(1)
  let r = http.get(`${vars.host}/${statusURL}`, params)

  const checkOutput = check(r, { 'check status check is 200': (_r) => r.status === 200, }, { tag: "Status check API" });
  if (!checkOutput) {
    console.log(`status: ${r.status},\nbody: ${r.json.body}\n`)
  }
  return r;
}

export function teardown(data) {
  // After All
  // teardown code
}
