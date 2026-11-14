#!/usr/bin/env bash
set -euo pipefail

base="${COLDLATCH_API_URL:-http://127.0.0.1:8080}"
command -v jq >/dev/null || { echo 'jq is required for the REST seed.' >&2; exit 2; }
seed_now="$(node -e 'process.stdout.write(new Date().toISOString())')"
at_offset() { node -e 'const t=Date.parse(process.argv[1]); process.stdout.write(new Date(t+Number(process.argv[2])*60000).toISOString())' -- "$seed_now" "$1"; }
request() {
  local method="$1" path="$2" token="$3" body="${4:-}" response status jar csrf args
  jar="${TMPDIR:-/tmp}/coldlatch-${token}.cookies"
  args=(-sS -o)
  if [[ "$method" != GET ]]; then
    curl -sS -o /dev/null -b "$jar" -c "$jar" "$base/api/auth/csrf"
    csrf="$(awk '$6=="XSRF-TOKEN" {v=$7} END {print v}' "$jar")"
  fi
  response="$(mktemp)"
  if [[ -n "$body" ]]; then status="$(curl -sS -o "$response" -w '%{http_code}' -b "$jar" -c "$jar" -X "$method" "$base$path" -H "X-XSRF-TOKEN: $csrf" -H 'Content-Type: application/json' --data "$body")";
  else status="$(curl -sS -o "$response" -w '%{http_code}' -b "$jar" -c "$jar" -X "$method" "$base$path")"; fi
  if [[ "$status" -lt 200 || "$status" -ge 300 ]]; then cat "$response" >&2; rm -f "$response"; echo "HTTP $status $method $path" >&2; exit 1; fi
  cat "$response"; rm -f "$response"
}
login() {
  local role="$1" password jar csrf response status
  local role_lower; role_lower="$(printf '%s' "$role" | tr '[:upper:]' '[:lower:]')"
  jar="${TMPDIR:-/tmp}/coldlatch-${role_lower}.cookies"; rm -f "$jar"
  password="${COLDLATCH_OPERATIONS_PASSWORD:?Set COLDLATCH_OPERATIONS_PASSWORD}"; [[ "$role" != ANALYST ]] || password="${COLDLATCH_ANALYST_PASSWORD:?Set COLDLATCH_ANALYST_PASSWORD}"
  curl -sS -o /dev/null -c "$jar" "$base/api/auth/csrf"
  csrf="$(awk '$6=="XSRF-TOKEN" {v=$7} END {print v}' "$jar")"
  response="$(mktemp)"; status="$(curl -sS -o "$response" -w '%{http_code}' -b "$jar" -c "$jar" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $csrf" --data "$(jq -cn --arg r "$role" --arg p "$password" '{role:$r,companyName:"ColdLatch",password:$p}')" "$base/api/auth/login")"
  [[ "$status" == 200 ]] || { cat "$response" >&2; echo "Login failed: HTTP $status" >&2; exit 1; }; rm -f "$response"
  printf '%s' "$role_lower"
}
ops="$(login OPERATIONS)"
analyst="$(login ANALYST)"
names=("Berlin to Munich, Insulin" "Mumbai to Delhi, Vaccines" "Chennai to Hyderabad, Biologics" "Delhi to Bengaluru, Plasma" "Pune to Mumbai, Insulin" "Hyderabad to Chennai, Reagents" "Kolkata to Pune, Supplies" "Delhi to Jaipur, Vaccines" "Mumbai to Goa, Biologics" "Chennai to Bengaluru, Samples")
origins=(Berlin Mumbai Chennai Delhi Pune Hyderabad Kolkata Delhi Mumbai Chennai)
destinations=(Munich Delhi Hyderabad Bengaluru Mumbai Chennai Pune Jaipur Goa Bengaluru)
products=(Insulin Vaccines Biologics Plasma Insulin Reagents Supplies Vaccines Biologics Samples)
profiles=(PROFILE-REF PROFILE-REF PROFILE-REF PROFILE-REF PROFILE-REF PROFILE-CRT PROFILE-CRT PROFILE-REF PROFILE-REF PROFILE-CRT)
shipments=(); lots=(); sensors=()

echo "seedNow=$seed_now"
for i in $(seq 0 9); do
  n=$((i+1)); expected="SHP-$(printf '%05d' $((10000+n)))"
  body="$(jq -cn --arg name "${names[$i]}" --arg trk "TRK-BER-$(date -u +%y%m%d)-$(printf '%02d' "$n")" --arg o "${origins[$i]}" --arg d "${destinations[$i]}" --arg p "${profiles[$i]}" --arg status "$([[ $i == 9 ]] && echo PLANNED || echo IN_TRANSIT)" '{trackingNumber:$trk,shipmentReference:$name,origin:$o,destination:$d,storageProfileId:$p,status:$status,expectedCadenceMinutes:5,allowedLatenessMinutes:120}')"
  created="$(request POST /api/shipments "$ops" "$body")"
  id="$(jq -r .id <<<"$created")"; [[ "$id" == "$expected" ]] || { echo "Unexpected shipment ID: $id expected $expected" >&2; exit 1; }
  lot_body="$(jq -cn --arg sid "$id" --arg product "${products[$i]}" --arg batch "BATCH-26-$(printf '%04d' "$n")" '{shipmentId:$sid,productName:$product,batchNumber:$batch,quantity:250}')"
  lot="$(request POST /api/lots "$ops" "$lot_body")"; lot_id="$(jq -r .id <<<"$lot")"
  sensor_list="$(request GET "/api/shipments/$id/sensors" "$ops")"; sensor_id="$(jq -r '.[0].id' <<<"$sensor_list")"
  shipments+=("$id"); lots+=("$lot_id"); sensors+=("$sensor_id")
  if [[ "$i" == 1 ]]; then
    earlier_lot="$(request POST /api/lots "$ops" "$(jq -cn --arg sid "$id" '{shipmentId:$sid,productName:"Vaccine reserve cases",batchNumber:"BATCH-26-RESERVE",quantity:40}')")"
    earlier_lot_id="$(jq -r .id <<<"$earlier_lot")"
    request PATCH "/api/lots/$earlier_lot_id" "$ops" "$(jq -cn --arg t "$(at_offset -12)" '{unloadedAt:$t}')" >/dev/null
    echo "PASS shipment $id has lot $earlier_lot_id unloaded before its excursion"
  fi
  echo "PASS registered $id / $lot_id ($sensor_id)"
done

reading() {
  local index="$1" temp="$2" offset="$3" event="${4:-}" sensor="${5:-${sensors[$1]}}"
  [[ -n "$event" ]] || event="$(at_offset "$offset")"
  local body
  body="$(jq -cn --argjson c "$temp" --arg t "$event" --arg s "$sensor" '{temperatureC:$c,eventTime:$t,sensorId:$s}')"
  request POST "/api/shipments/${shipments[$index]}/lots/${lots[$index]}/temperature" "$ops" "$body"
}

# Normal, minor excursion, critical excursion, missing interval.
reading 0 5.4 -20 >/dev/null; reading 0 5.7 -10 >/dev/null; reading 0 5.5 0 >/dev/null
reading 1 5.2 -20 >/dev/null; reading 1 9.4 -10 >/dev/null; reading 1 5.8 -5 >/dev/null
reading 2 5.0 -20 >/dev/null; reading 2 30.0 -10 >/dev/null
reading 3 5.0 -240 >/dev/null; reading 3 5.1 -40 >/dev/null
# Late within 72h is accepted and re-evaluated alongside a newer normal value.
reading 4 5.0 -15 >/dev/null; reading 4 5.5 -90 >/dev/null; reading 4 5.2 0 >/dev/null
reading 5 20.0 0 >/dev/null
reading 6 15.0 -30 >/dev/null; reading 6 18.0 -20 >/dev/null; reading 6 22.0 -10 >/dev/null; reading 6 27.0 0 >/dev/null
# Duplicate replay uses the same sensor, event time, and temperature key.
duplicate_time="$(at_offset -10)"; reading 7 5.5 -20 >/dev/null; reading 7 10.0 -10 "$duplicate_time" >/dev/null; reading 7 10.0 -10 "$duplicate_time" >/dev/null; reading 7 10.0 -10 "$duplicate_time" >/dev/null; reading 7 10.0 -10 "$duplicate_time" >/dev/null
# A second active sensor forces explicit sensor selection.
second_sensor="$(request POST /api/sensors "$ops" "$(jq -cn --arg id SEN-DEMO-10008 --arg sid "${shipments[7]}" '{id:$id,model:"ColdLatch logger",batteryLevel:95,shipmentId:$sid}')" | jq -r .id)"
request POST "/api/shipments/${shipments[7]}/sensors" "$ops" "$(jq -cn --arg id "$second_sensor" '{sensorId:$id}')" >/dev/null
reading 7 5.4 -1 "" "$second_sensor" >/dev/null
# Excursion plus a quarantined event outside the late window and compliant inspection.
reading 8 5.0 -30 >/dev/null; reading 8 11.0 -10 >/dev/null
reading 8 12.0 -150000 "$(at_offset -150000)" >/dev/null

investigate() {
  local i="$1" decision="$2" evidence_temp="$3" excursion investigation id
  excursion="$(request GET "/api/shipments/${shipments[$i]}/excursions" "$ops" | jq -r '.[0].id')"
  id="$(request POST /api/investigations "$ops" "$(jq -cn --arg s "${shipments[$i]}" --arg l "${lots[$i]}" --arg e "$excursion" '{shipmentId:$s,lotId:$l,excursionId:$e}')" | jq -r .id)"
  request PATCH "/api/investigations/$id/assignee" "$ops" '{"investigator":"Analyst"}' >/dev/null
  request POST "/api/investigations/$id/manual-inspection" "$ops" "$(jq -cn --argjson temp "$evidence_temp" --arg time "$seed_now" '{inspectionTime:$time,observedTemperatureC:$temp,packageIntegrityStatus:"INTACT",visualCondition:"Packaging intact",notes:"Temperature evidence reviewed"}')" >/dev/null
  request POST "/api/investigations/$id/decision" "$analyst" "$(jq -cn --arg d "$decision" --arg l "${lots[$i]}" '{decision:$d,reason:"Disposition supported by reviewed shipment evidence.",affectedLotIds:[$l]}')" >/dev/null
  echo "PASS investigation $id disposition $decision"
}
investigate 2 HOLD 30
investigate 6 RELEASE 20
investigate 7 REJECT 10
# Contradictory manual inspection on a recorded excursion, intentionally open.
excursion9="$(request GET "/api/shipments/${shipments[8]}/excursions" "$ops" | jq -r '.[0].id')"
inv9="$(request POST /api/investigations "$ops" "$(jq -cn --arg s "${shipments[8]}" --arg l "${lots[8]}" --arg e "$excursion9" '{shipmentId:$s,lotId:$l,excursionId:$e}')" | jq -r .id)"
request POST "/api/investigations/$inv9/manual-inspection" "$ops" "$(jq -cn --arg time "$seed_now" '{inspectionTime:$time,observedTemperatureC:5.0,packageIntegrityStatus:"INTACT",visualCondition:"Product and packaging compliant",notes:"Manual cold-chain inspection"}')" >/dev/null
echo "PASS seed created 10 shipments, 10 lots, readings, duplicate replay, late/quarantined evidence, investigations and all decisions via REST"
