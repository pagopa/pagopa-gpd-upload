# Performance Tests with k6

[k6](https://k6.io/) is a load testing tool. 👀
See [here](https://k6.io/docs/get-started/installation/) to install it.

## How to Run 🚀

To run k6 tests use the command:

``` shell
k6 run --env VARS=environments/local.environment.json --env TEST_TYPE=./test-types/smoke.json main_scenario.js
```

where

- _VARS_ is a environment file
- _TEST_TYPE_ is a file in `/test-types` folder
- _API_SUBSCRIPTION_KEY_ is your sub-key

`<script-name>.js` is the scenario to run with k6

## Status API ramping test

The ramping test assesses status API performance under the offered load. Run
it with the APIM rate limit disabled when measuring backend capacity; HTTP 429
responses count as failed requests and contribute to the
`http_req_failed{api:status}` threshold. The threshold allows a failure rate
below 10%, so the test fails when the rate of 429 and other failed responses
reaches 10% or more.

The UAT capacity run ramped up to 100 requests/s and completed 33,629 status
requests with HTTP 200 responses and a p99 of approximately 699 ms. This result
was collected with the APIM rate limit disabled.

`main_scenario.js` uploads one ZIP in `setup()` (checking HTTP 202 and the
`Location` header), calls status during the ramp, and retrieves the report once
in `teardown()` (checking HTTP 200).

This ramping configuration is not a dedicated APIM throttling validation:
HTTP 429 responses are treated as failed status requests, just like other
non-successful status responses.
