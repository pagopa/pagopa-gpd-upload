const axios = require("axios");
const fs = require('fs');
const FormData = require('form-data');


axios.defaults.headers.common['Ocp-Apim-Subscription-Key'] = process.env.SUBKEY // for all requests
if (process.env.CANARY) {
  axios.defaults.headers.common['X-Canary'] = 'canary' // for all requests
}

async function uploadFile(hostUrl, filePath) {
    let data = new FormData();
    data.append('file', fs.createReadStream(filePath));

    let config = {
      method: 'post',
      maxBodyLength: Infinity,
      url: hostUrl,
      headers: {
        'Content-Type': 'multipart/form-data',
        'Ocp-Apim-Subscription-Key': process.env.SUBKEY,
        ...data.getHeaders()
      },
      data : data
    };

    let response = await axios.request(config)
    console.log(JSON.stringify(response.data));

    return response;
}

function get(url) {
  return axios.get(url)
  .then(res => {
    return res;
  })
  .catch(error => {
    return error.response;
  });
}

function post(url, body) {
  return axios.post(url, body)
  .then(res => {
    return res;
  })
  .catch(error => {
    return error.response;
  });
}

function put(url, body) {
  return axios.put(url, body)
  .then(res => {
    return res;
  })
  .catch(error => {
    return error.response;
  });
}

function del(url) {
  return axios.delete(url)
  .then(res => {
    return res;
  })
  .catch(error => {
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

module.exports = {get, post, put, del, call, uploadFile}
