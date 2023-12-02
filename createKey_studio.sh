#!/bin/bash

#get a JWT token
EMAIL=$1
PASSWORD=$2
SKEY_FILE=$3
VKEY_FILE=$4
NAME=$5

#'GTjEwdaJVR3K$FGsV*5%LE2mQ^j%8@h'

ACCESS_TOKEN=`curl -X POST -H "Content-Type: application/json" -H "Accept: application/json" -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}" https://studio.newm.io/v1/auth/login 2>/dev/null | jq -r .accessToken`
echo "accessToken: $ACCESS_TOKEN"

SKEY=`cat $SKEY_FILE | jq -r tostring`
VKEY=`cat $VKEY_FILE | jq -r tostring`


cat > /tmp/create_key.json << EOF
{
	"name": "$NAME",
	"skey": $SKEY,
	"vkey": $VKEY
}
EOF

cat /tmp/create_key.json | jq .

curl -X POST -H "Content-Type: application/json" -H "Accept: application/json" -H "Authorization: Bearer $ACCESS_TOKEN" -d "@/tmp/create_key.json" https://studio.newm.io/v1/cardano/key 2>/dev/null | jq .
