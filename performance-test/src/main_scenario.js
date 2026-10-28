import http from 'k6/http';
import exec from 'k6/execution';
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
  console.log(JSON.stringify({
    environment: vars.env,
    uploadUrl: rootUrl,
    fileNumber: __ENV.FILE_NUMBER,
    subscriptionKeyPresent: Boolean(__ENV.API_SUBSCRIPTION_KEY),
  }));
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

function logFailure(stage, response) {
  const diagnosticHeaders = {};
  const headerNames = [
    'content-type', 'date', 'server', 'server-timing', 'via',
    'www-authenticate', 'retry-after', 'apim-request-id',
    'request-id', 'x-azure-ref', 'x-cache', 'x-correlation-id',
    'x-envoy-upstream-service-time', 'x-ms-correlation-request-id',
    'x-ms-request-id', 'x-powered-by', 'x-request-id', 'traceparent',
  ];
  for (const name of Object.keys(response.headers)) {
    if (headerNames.includes(name.toLowerCase())) {
      diagnosticHeaders[name] = response.headers[name];
    }
  }
  const requestHeaders = response.request.headers;
  const message = JSON.stringify({
    stage,
    method: response.request.method,
    requestUrl: response.request.url,
    requestHeaderNames: Object.keys(requestHeaders),
    subscriptionKeySent: Object.keys(requestHeaders).some(
      name => name.toLowerCase() === 'ocp-apim-subscription-key',
    ),
    url: response.url,
    status: response.status,
    error: response.error,
    errorCode: response.error_code,
    timings: response.timings,
    headers: diagnosticHeaders,
    body: response.body,
  });
  const key = __ENV.API_SUBSCRIPTION_KEY;
  console.error(key ? message.split(key).join('[REDACTED]') : message);
}


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
    logFailure('upload', r);
    fail(`Upload failed with HTTP ${r.status}; skipping status and report requests.`);
  }

  let statusURL = r.headers['Location']
  if (!check(r, { 'check upload response has Location': () => Boolean(statusURL) })) {
    logFailure('upload: missing Location', r);
    fail('Upload response has no Location; skipping status and report requests.');
  }
  r = callStatus(statusURL)

  while(r.status === 429 || r.json().processedItem !== r.json().submittedItem) {
    r = callStatus(statusURL)
  }

  sleep(1)
  let reportURL = statusURL.replace("/status", "/report")
  r = http.get(`${vars.host}/${reportURL}`, params)

  checkOutput = check(r, { 'check report retrieve is 200': (_r) => r.status === 200, }, { tag: "Report retrieve API" });
  if (!checkOutput) {
    logFailure('report', r);
  }

  postcondition();
}

function callStatus(statusURL) {
  sleep(1)
  let r = http.get(`${vars.host}/${statusURL}`, params)

  const checkOutput = check(r, { 'check status check is 200': (_r) => r.status === 200, }, { tag: "Status check API" });
  if (!checkOutput) {
    logFailure('status', r);
    if (r.status !== 429) {
      fail(`Status request failed with HTTP ${r.status}; skipping further polling and report.`);
    }
  }
  return r;
}

export function teardown(data) {
  // After All
  // teardown code
}
