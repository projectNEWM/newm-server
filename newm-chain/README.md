# NEWM-CHAIN

## Overview
NEWM-CHAIN is a gRPC interface for communicating with the Cardano Blockchain. Because it uses gRPC, it is possible to use any language that supports gRPC to communicate with the Cardano Blockchain. While some apis are specific to the NEWM project, most of the apis are useful to any project.

## Building
NEWM-CHAIN uses [Gradle](https://gradle.org/) as its build system. Java JDK 17 is required. To build the project, run the following command:
```
./gradlew shadowJar
```
This will create a jar file in the `newm-chain/build/libs` directory.

## Installation
NEWM-CHAIN is still in beta, so you'll have to build the jar file manually for now. Once the project is more stable, it will be published on the releases page.

### Files
NEWM-CHAIN requires a few files to run. These files are:
- `newm-chain.jar` - The jar file created by the build process.
- `.env` - A file containing environment variables. See the [Configuration](configuration) section for more information.

### Ogmios
You'll need to install Ogmios to run NEWM-CHAIN. You can install Ogmios by following their [installation guide](https://ogmios.dev). You will need to edit the `.env` file to point to your installation of Ogmios.

### Postgres
NEWM-CHAIN uses a postgres database to store data. You can install postgres on Ubuntu with the following commands:
```
sudo apt update
sudo apt install postgresql postgresql-contrib
```
Once postgres is installed, you'll need to create a database and a user for NEWM-CHAIN. You can do this with the following commands:
```
sudo -i -u postgres
createuser --interactive
```
You'll be prompted to enter a name for the user. Enter `newmchain`. You'll then be prompted to answer a few questions. Answer `n` to all of them. Then, run the following psql commands:
```
Enter name of role to add: newmchain
Shall the new role be a superuser? (y/n) n
Shall the new role be allowed to create databases? (y/n) n
Shall the new role be allowed to create more new roles? (y/n) n
psql
postgres=# CREATE DATABASE newmchain;
CREATE DATABASE
postgres=# ALTER USER newmchain WITH PASSWORD '<pick_strong_password_here>';
ALTER ROLE
postgres=# ALTER USER newmchain VALID UNTIL 'infinity';
ALTER ROLE
postgres=# ALTER DATABASE newmchain OWNER TO newmchain;
ALTER DATABASE
postgres=# \q
exit
```

### systemd service
NEWM-CHAIN can be run as a systemd service. To do this, use `sudo nano /etc/systemd/system/newmchain.service` to create a file with the following contents:
```
[Unit]
Description=NewmChain - ChainSync and APIs for Cardano
After=syslog.target
StartLimitIntervalSec=0

[Service]
Type=simple
Restart=always
RestartSec=5
User=<your_linux_username_here>
LimitNOFILE=131072
WorkingDirectory=/home/<your_linux_username_here>/newmchain
EnvironmentFile=/home/<your_linux_username_here>/newmchain/.env
ExecStart=/usr/bin/java -XX:+DisableAttachMechanism -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xmx8192m -jar newm-chain.jar
SuccessExitStatus=143
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=newmchain
                
[Install]
WantedBy=multi-user.target
```
Then, run the following commands to enable automatic startup:
```
sudo systemctl daemon-reload
sudo systemctl enable newmchain.service
```

## Configuration
NEWM-CHAIN uses environment variables for configuration. These variables are stored in a file called `.env`. This file should be located in the same directory as the jar file. An example `.env` can be found here: [systemd/.env.example](.env.example).

The JWT_SECRET value for your environment should be a random string of characters. You can generate a random string with the following command:
```
openssl rand -base64 258 | tr -d '\n'
```

## Usage
NEWM-CHAIN is a gRPC server. It can be accessed using any language that supports gRPC. The gRPC server runs on port 3737 by default. You can change this by editing the `.env` file and adding a `PORT` option.

### JWT Authentication
NEWM-CHAIN uses JWT authentication. To authenticate, you must send a JWT token in the `Authorization` header of your request. The JWT token must be signed with the JWT_SECRET value in your `.env` file. To generate a JWT token, create a bash script called `generateJWT.sh` with the following contents:
```
#!/bin/bash
set -a
source <(sed -e "s/\r//" -e '/^#/d;/^\s*$/d' -e "s/'/'\\\''/g" -e "s/=\(.*\)/=\"\1\"/g" ".env")
set +a

/usr/bin/java -XX:+DisableAttachMechanism -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xmx16384m -jar newm-chain.jar JWT $1 2>&1 | sed 's/^.*\]: //g; s#WARN.*$#\x1b[33m&\x1b[0m#; s#ERROR.*$#\x1b[31m&\x1b[0m#; s#INFO#\x1b[32m&\x1b[0m#'
```
Then, run the following command:
```
./generateJWT.sh <your_jwt_username_here>
```
A warning log entry will be generated with the JWT token for this user. You can use this token to authenticate with the gRPC server.

### gRPC
A gRPC client can be generated using the [NEWM-CHAIN proto file](src/main/proto/newm_chain.proto). You can find an example of how to do this for a number of languages:
- [gRPC Languages](https://grpc.io/docs/languages/)

You will need to authenticate using the JWT token created above. Here's an example usage from a Kotlin client:
```
val channel = ManagedChannelBuilder.forAddress("localhost", 3737).usePlaintext().build()
val client =
    NewmChainGrpcKt.NewmChainCoroutineStub(channel).withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(
            Metadata().apply {
                put(
                    Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                    "Bearer <JWT_TOKEN_HERE_DO_NOT_COMMIT>"
                )
            }
        )
    )
val request = walletRequest {
    accountXpubKey = "xpub10yq2v72lq0h7lnhkw308uy23fjq384zufvyesh6mlklnpmv048xs8arze4nws0xfp8h87d7jdxwgm5dsr7l0qruedrtcdudjlnxls3sm0qlln"
}
val response = client.queryWalletControlledLiveUtxos(request)
println("response: $response")
````

#### gRPC SSL
You can enable SSL for the gRPC server by setting the `SSL_CERT_CHAIN_PATH` and `SSL_PRIVATE_KEY_PATH` options in the `.env` file. Depending on where you got your certificate, you may need to ensure that the newm-chain user your service runs under has access to these files. Also ensure that access is correct whenever the certificate rotates.
```
SSL_CERT_CHAIN_PATH=/etc/letsencrypt/live/newm-chain.server.com/fullchain.pem
SSL_PRIVATE_KEY_PATH=/etc/letsencrypt/live/newm-chain.server.com/privkey.pem
```

Another option for enabling SSL is to put a reverse-proxy like NGINX in front of it. This advanced configuration won't be covered in this guide.

Here is an example usage from a Kotlin client:
```
val channel =
    ManagedChannelBuilder.forAddress("newm-chain.cardanostakehouse.com", 3737).useTransportSecurity().build()
val client =
    NewmChainGrpcKt.NewmChainCoroutineStub(channel).withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(
            Metadata().apply {
                put(
                    Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                    "Bearer <JWT_TOKEN_HERE_DO_NOT_COMMIT>"
                )
            }
        )
    )
val request = walletRequest {
    accountXpubKey = "xpub10yq2v72lq0h7lnhkw308uy23fjq384zufvyesh6mlklnpmv048xs8arze4nws0xfp8h87d7jdxwgm5dsr7l0qruedrtcdudjlnxls3sm0qlln"
}
val response = client.queryWalletControlledLiveUtxos(request)
println("response: $response")
```