const fs = require('fs');
const FormData = require('form-data');
const axios = require("axios");

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

    return response;
}

module.exports = {uploadFile}
