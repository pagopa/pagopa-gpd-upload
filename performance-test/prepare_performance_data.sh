#!/bin/bash

FILE_NUMBER=$1
DEBT_POSITION_BY_FILE=$2


if [ -z "$FILE_NUMBER" ]
then
  echo "file-number not specified: sh prepare_performance_data.sh <file-number> <debt-positions-by-file>"
  exit 1
fi

if [ -z "$DEBT_POSITION_BY_FILE" ]
then
  echo "debt-positions-by-file not specified: sh prepare_performance_data.sh <file-number> <debt-positions-by-file>"
  exit 1
fi

# prepare zip files to upload
mkdir src/files
cd yarn
yarn install
yarn node data ${DEBT_POSITION_BY_FILE} ../src/files/ ${FILE_NUMBER} # create 10 zip file with 100 debt positions in JSON format
cd ..
