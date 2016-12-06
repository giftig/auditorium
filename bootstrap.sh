#!/bin/bash

if [[ "$ES_HOST" == '' ]]; then
  ES_HOST=localhost
fi

if [[ "$ES_PORT" == '' ]]; then
  ES_PORT=9200
fi

if [[ "$ES_INDEX_PREFIX" == '' ]]; then
  ES_INDEX_PREFIX='auditorium-'
fi

INDEX_NAME=$ES_INDEX_PREFIX$(date +'%Y%m')

echo "Bootstrapping elasticsearch with auditorium index ($INDEX_NAME)..."
curl \
  -X PUT \
  -H 'Content-type: application/json' \
  -d '@src/main/resources/index-template.json' \
  http://$ES_HOST:$ES_PORT/$INDEX_NAME

RESULT=$?
echo ''

exit $RESULT
