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

## Status API: capacity and throttling

The tests have two different goals:

1. With the APIM rate limit disabled, assess application performance under the
   offered load. The UAT capacity run ramped up to 100 requests/s and completed
   33,629 status requests with HTTP 200 responses and a p99 of approximately
   699 ms.
2. With the 45 requests/s APIM rate limit re-enabled, assess throttling:
   admitted requests should return HTTP 200 and excess requests should return
   HTTP 429, according to the configured APIM counting key and window.

`main_scenario.js` uploads one ZIP in `setup()` (checking HTTP 202 and the
`Location` header), calls status during the ramp, and retrieves the report once
in `teardown()` (checking HTTP 200).

The throttling configuration in `test-types/ramping.json` keeps the ramp up to
100 requests/s. Status responses are evaluated separately:

- `status_responses_200`: admitted requests; at least one is required.
- `status_responses_429`: throttled requests; at least one is required.
- `status_unexpected_errors`: responses other than 200 or 429, including
  network failures; the required rate is zero.
- `http_req_duration{api:status,status:200}`: latency of admitted requests only;
  p99 must be below 2 seconds.

HTTP 429 is expected only for status requests, not for upload or report.
These thresholds are intended for the run with throttling enabled: a run
without throttling would fail the requirement for at least one HTTP 429.

Aggregate counts demonstrate that the filter intervenes, but do not prove
that it enforces exactly 45 requests/s. To evaluate that, compare timestamped
200 and 429 responses with the offered load and the APIM policy configuration.
