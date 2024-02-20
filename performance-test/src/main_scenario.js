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
let zipFile = open('./files/test1.json.zip', 'b');

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

  let idx = exec.scenario.iterationInInstance;

  console.log("idx: " + idx)

  let tag = {
  };

  // "/brokers/{broker}/organizations/{ec}/debtpositions/file"
  let url = `${rootUrl}`;

  const data = {
    field: 'file',
    file: http.file(zipFile, 'file.zip'),
  };

  let r = http.post(url, data, params);

  // check(r, { 'check status is 202': (_r) => r.status === 202, }, tag);

  let statusURL = r.headers['Location']
  console.log(statusURL)
  r = http.get(`${vars.host}/${statusURL}`)

  sleep(5)

  while(r.json().processedItem !== r.json().submittedItem) {
    r = http.get(`${vars.host}/${statusURL}`)
    sleep(30)
  }

  let reportURL = statusURL.replace("/status", "/report")
  r = http.get(`${vars.host}/${reportURL}`)

  check(r, {
    'check report is created': (_r) => r.status === 200,
  }, tag);

  postcondition();
}

export function teardown(data) {
  // After All
  // teardown code
}
