const axios = require("axios");

axios.defaults.headers.common['Ocp-Apim-Subscription-Key'] = process.env.SUBKEY // for all requests
if (process.env.CANARY) {
  axios.defaults.headers.common['X-Canary'] = 'canary' // for all requests
}

function get(url) {
  return axios.get(url)
  .then(res => {
    return res;
  })
  .catch(error => {
    console.log("Error while calling GET " + url)
    return error.response;
  });
}

function post(url, body) {
  return axios.post(url, body)
  .then(res => {
    return res;
  })
  .catch(error => {
    console.log("Error while calling POST " + url)
    return error.response;
  });
}

function put(url, body) {
  return axios.put(url, body)
  .then(res => {
    return res;
  })
  .catch(error => {
    console.log("Error while calling PUT " + url)
    return error.response;
  });
}

function del(url) {
  return axios.delete(url)
  .then(res => {
    return res;
  })
  .catch(error => {
    console.log("Error while calling DELETE " + url)
    return error.response;
  });
}

function call(method, url, body) {
    if (method === 'GET') {
        return get(url)
    }
    if (method === 'POST') {
        return post(url, body)
    }
    if (method === 'PUT') {
        return put(url, body)
    }
    if (method === 'DELETE') {
        return del(url)
    }
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}


module.exports = {get, post, put, del, call, sleep}
