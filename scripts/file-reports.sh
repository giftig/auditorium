#!/bin/bash

DIR="$(readlink -f "$(dirname $0)")"

if [[ "$AUDITORIUM_HOST" == '' ]]; then
  AUDITORIUM_HOST=localhost
fi
if [[ "$AUDITORIUM_PORT" == '' ]]; then
  AUDITORIUM_PORT=8080
fi

file_report() {
  REPORT_ID=$1
  OBJECT_ID=$2
  if [[ "$3" != '' ]]; then
    TIMESTAMP="$3"
  else
    TIMESTAMP=$(date +%Y-%m-%dT%H:%M:%SZ)
  fi

  echo -ne "Filing report $REPORT_ID...\t"

  f="$(mktemp file-reports-XXXXXX.json)"
  cat "$DIR/../fixtures/report-template.json.template" |
    sed \
      -e "s|{{ report_id }}|$REPORT_ID|g" \
      -e "s|{{ object_id }}|$OBJECT_ID|g" \
      -e "s|{{ timestamp }}|$TIMESTAMP|g" > "$f"

  curl \
    -X POST \
    -H 'Content-type: application/json' \
    -d "@$f" \
    $AUDITORIUM_HOST:$AUDITORIUM_PORT/report/
  echo ''
  rm -f $f
}

# Insert 100 random documents from template, a second apart each time
for i in $(seq 100); do
  file_report report-$RANDOM$RANDOM object-$RANDOM
  sleep 1
done
