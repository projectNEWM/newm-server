#!/bin/bash

#get a JWT token
EMAIL=$1
PASSWORD=$2
ESALT=$3
EPASSWORD=$4

ACCESS_TOKEN=`curl -X POST -H "Content-Type: application/json" -H "Accept: application/json" -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}" https://studio.newm.io/v1/auth/login 2>/dev/null | jq -r .accessToken`
#echo "accessToken: $ACCESS_TOKEN"

cat > /tmp/create_encryptor.json << EOF
{
	"s": "$ESALT",
	"password": "$EPASSWORD"
}
EOF

curl -X POST --verbose -H "Content-Type: application/json" -H "Accept: application/json" -H "Authorization: Bearer $ACCESS_TOKEN" -d "@/tmp/create_encryptor.json" https://studio.newm.io/v1/cardano/encryption 2>/dev/null | jq .
