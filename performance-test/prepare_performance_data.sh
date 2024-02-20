#!/bin/bash

# prepare zip files to upload
mkdir src/files
cd yarn
yarn install
yarn node data 100 ../src/files/ 10 # create 10 zip file with 100 debt positions in JSON format
cd ..
