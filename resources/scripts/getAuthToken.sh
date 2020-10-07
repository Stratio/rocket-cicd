#!/bin/bash

function filter_jsessionid_cookie() {
    RET_VALUE=$(echo $1 \
        | grep -oE "JSESSIONID=[^;]+" \
        | sed "s/JSESSIONID=//g")
    echo $RET_VALUE
}

function filter_execution_value() {
    RET_VALUE=$(echo $1  \
        | grep -oE "\"execution\" value=\"[^<>]+\"" \
        | grep -oE "value=\"[^\"]+\"" \
        | sed "s/value=//g" \
        | sed "s/\"//g" \
        | sed "s/\//%2F/g" \
        | sed "s/+/%2B/g" \
        | sed "s/=/%3D/g")
     echo $RET_VALUE
}

function filter_lt_value() {
    RET_VALUE=$(echo $1 \
        | grep -oE "\"lt\" value=\"[^<>]+\"" \
        | grep -oE "value=\"[^\"]+\"" \
        | sed "s/value=//g" \
        | sed "s/\"//g")
    echo $RET_VALUE
}

function filter_ticket_value() {
    RET_VALUE=$(echo $1 \
        | grep -oE "ticket[^ ]+" \
        | sed "s/ticket=//g" \
        | grep -oE "[^ ]+" \
        | tr -d "\r")
    echo $RET_VALUE
}

function filter_location_value() {
  RET_VALUE=$(echo $1 \
        | grep -oE "Location: [^ ]+" \
        | sed "s/Location: //g" \
        | grep -oE "[^ ]+" \
        | tr -d "\r")
    echo $RET_VALUE
}

function filter_user_value() {
    RET_VALUE=$(echo $1 \
        | grep -oE "user=[a-z0-9\-]+" \
        | sed "s/user=//g")
   echo $RET_VALUE
}

############## Login
#
# login
#     INPUTS:
#           1. ROCKET_UI_URI -> https://sparta.stratio-ey-wavespace.com/sparta
#           2: User_id with permission Rocket API
#           3: User Password
#           4: Tenant -> NONE
#     OUTPUT:
#            a file located in $TICKET_FILE with the user ticket to access Tocket API

# Find sso login url from Sparta UI redirection url

if [ $# -eq 5 ]
then
  ROCKET_UI_URI=$1
  USERLOGIN=$2
  PASSWD=$3
  TENANT=$4
  TICKET_FILE=$5
else
  echo "[ERROR] Script takes exactly 5 parameters: ROCKET_UI_URI, User_id, Password, Tenant and FilePath"
  exit 1
fi


EFFECTIVE_URL=$(curl -I -k -s -L $ROCKET_UI_URI -w 'url_effective=%{url_effective}')
ROCKET_LOGIN_URI=$(echo $EFFECTIVE_URL \
                | grep -oE 'url_effective=https?://[^ ]+' \
                | grep -oE 'https?://[^ ]+' \
                | head -n 1)

if [ $ROCKET_UI_URI == $ROCKET_LOGIN_URI ]
then
  echo "[ERROR] Could not retrieve a valid sso login url: $ROCKET_LOGIN_URI"
  exit 1
fi

AUTHORIZE_RESPONSE=$(curl -X GET $ROCKET_LOGIN_URI -k -i -s -w 'http_status=(%{http_code})')

HTTP_STATUS_CODE=$(echo $AUTHORIZE_RESPONSE \
                | grep -oE "http_status=\([0-9]+\)" \
                | grep -oE "[0-9]+" \
                | head -n 1)

if [ $HTTP_STATUS_CODE -ne 200 ]
then
  exit 1
fi

JSESSIONID=$(filter_jsessionid_cookie "$EFFECTIVE_URL")
EXECUTION=$(filter_execution_value "$AUTHORIZE_RESPONSE")
LT=$(filter_lt_value "$AUTHORIZE_RESPONSE")

LOGIN_SERVICE=$(curl -X POST $ROCKET_LOGIN_URI -k -i -s -w 'http_status=(%{http_code})' -H "Cookie: JSESSIONID=$JSESSIONID" --data "lt=$LT&execution=$EXECUTION&_eventId=submit&username=$USERLOGIN&password=$PASSWD&tenant=$TENANT")
AUTHORIZE_URL=$(filter_location_value "$LOGIN_SERVICE")

AUTHORIZE_URL_RESPONSE=$(curl -X GET "$AUTHORIZE_URL" -H "Cookie: JSESSIONID=$JSESSIONID" -k -s -I --compressed)
TICKET_URL=$(filter_location_value "$AUTHORIZE_URL_RESPONSE")

TICKET_URL_RESPONSE=$(curl -X GET "$TICKET_URL" -k -s -I --compressed)
USER_TOKEN=$(filter_user_value "$TICKET_URL_RESPONSE")

echo $USER_TOKEN > $TICKET_FILE