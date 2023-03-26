#!/bin/bash

# This script will start the service from the cmdline on systems that don't run
# systemd. It looks for the .env file in the same folder to configure newm-chain

set -a
source <(sed -e "s/\r//" -e '/^#/d;/^\s*$/d' -e "s/'/'\\\''/g" -e "s/=\(.*\)/=\"\1\"/g" ".env")
set +a

/usr/bin/java -XX:+DisableAttachMechanism -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xmx16384m -jar newm-chain.jar 2>&1 | sed 's/^.*\]: //g; s#WARN.*$#\x1b[33m&\x1b[0m#; s#ERROR.*$#\x1b[31m&\x1b[0m#; s#INFO#\x1b[32m&\x1b[0m#'
