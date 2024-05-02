const fs = require('fs');
const FormData = require('form-data');
const axios = require("axios");

async function uploadFile(hostUrl, filePath, method) {
    let data = new FormData();
    data.append('file', fs.createReadStream(filePath));

    let config = {
      method: method,
      maxBodyLength: Infinity,
      url: hostUrl,
      headers: {
        'Content-Type': 'multipart/form-data',
        'Ocp-Apim-Subscription-Key': process.env.SUBKEY,
        ...data.getHeaders()
      },
      data : data
    };

    try {
      const response = await axios.request(config);
      console.error("Upload file response:", response.status);

      return response;
  } catch (error) {
      // Handle errors
      console.error("Error in upload file request: ", error.response.status);
      return error.response;
  }
}

module.exports = {uploadFile}
