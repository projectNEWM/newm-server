---
name: newm-chain-grpc
description: Use when calling, inspecting, or troubleshooting any newm-chain gRPC endpoint with grpcurl, including UTXO queries, monitoring streams, transaction building, and transaction submission.
---

# NEWM Chain gRPC

Use `grpcurl` and the repository's protobuf definition to communicate with any
`newm-chain` gRPC endpoint. Discover the current API from the proto instead of relying on a
copied endpoint catalog.

## Source Of Truth

- Proto: `newm-chain-grpc/src/main/proto/newm_chain.proto`
- Service: `newmchain.NewmChain`
- Default local target: `localhost:3737`
- Default local transport: plaintext HTTP/2 (`-plaintext`)
- Authentication: `Authorization: Bearer <JWT>`

Run commands from the repository root so the proto path resolves. The server does not expose
the gRPC reflection API, so always pass `-import-path` and `-proto`.

## Required Workflow

1. Identify the method, target, TLS mode, request data, and whether the response is unary or
   streaming.
2. If the method or request shape is unfamiliar, inspect it from the local proto before calling
   it.
3. Classify the method using the safety rules below. Treat unknown future methods as mutating
   until their implementation or documentation proves otherwise.
4. Resolve authentication without printing or persisting the token.
5. Construct protobuf JSON carefully, invoke the method, and report a redacted command summary
   with the result.

If the user does not specify a target, use `localhost:3737`. Use plaintext only for a target
known to have TLS disabled, such as the default local service. Default remote targets to TLS.
Never use `-insecure` unless the user explicitly authorizes disabling certificate verification.

## Discover The API

List all services defined by the proto:

```bash
grpcurl \
  -import-path newm-chain-grpc/src/main/proto \
  -proto newm_chain.proto \
  list
```

List every method on `newmchain.NewmChain`:

```bash
grpcurl \
  -import-path newm-chain-grpc/src/main/proto \
  -proto newm_chain.proto \
  list newmchain.NewmChain
```

Describe an endpoint:

```bash
grpcurl \
  -import-path newm-chain-grpc/src/main/proto \
  -proto newm_chain.proto \
  describe newmchain.NewmChain.QueryLiveUtxos
```

Describe a request and show a JSON template:

```bash
grpcurl \
  -msg-template \
  -import-path newm-chain-grpc/src/main/proto \
  -proto newm_chain.proto \
  describe newmchain.QueryUtxosRequest
```

For a future method, derive its request and response types from these commands. Do not guess
field names or copy an old schema from documentation.

## Authentication

Prefer a JWT supplied in `NEWM_CHAIN_JWT`. Use header expansion so the token is not placed in the
`grpcurl` argument list:

```bash
grpcurl \
  -expand-headers \
  -H 'Authorization: Bearer ${NEWM_CHAIN_JWT}' \
  ...
```

If `NEWM_CHAIN_JWT` is absent and the user authorizes local test credentials, a local test token
can be found in
`newm-server/src/test/kotlin/io/newm/server/grpc/GrpcTests.kt`. Read or extract it only for the
current process. Do not copy it into this skill, another tracked file, a progress update, or the
final response. Never print a token while diagnosing authentication.

Report only which credential source was used and whether authentication succeeded. If no
authorized credential source exists, ask the user for one rather than attempting to generate or
discover secrets broadly.

## Invoke Unary Methods

Plaintext template:

```bash
grpcurl \
  -plaintext \
  -expand-headers \
  -import-path newm-chain-grpc/src/main/proto \
  -proto newm_chain.proto \
  -H 'Authorization: Bearer ${NEWM_CHAIN_JWT}' \
  -d '<JSON_REQUEST>' \
  localhost:3737 \
  newmchain.NewmChain/<Method>
```

TLS template (omit `-plaintext`):

```bash
grpcurl \
  -expand-headers \
  -import-path newm-chain-grpc/src/main/proto \
  -proto newm_chain.proto \
  -H 'Authorization: Bearer ${NEWM_CHAIN_JWT}' \
  -d '<JSON_REQUEST>' \
  newm-chain.example.com:3737 \
  newmchain.NewmChain/<Method>
```

Use `{}` for empty requests such as `QueryTip`, `QueryCurrentEpoch`, `IsMainnet`, and
`QueryCardanoEra`.

Example read-only live UTXO query:

```bash
grpcurl \
  -plaintext \
  -expand-headers \
  -import-path newm-chain-grpc/src/main/proto \
  -proto newm_chain.proto \
  -H 'Authorization: Bearer ${NEWM_CHAIN_JWT}' \
  -d '{"address":"addr1..."}' \
  localhost:3737 \
  newmchain.NewmChain/QueryLiveUtxos
```

## Protobuf JSON Rules

- Prefer protobuf JSON lower-camel-case field names, such as `datumHash` and `startAfterId`.
- Encode `bytes` fields as base64, not hexadecimal.
- For a hex CBOR value supplied to a `bytes` field such as `SubmitTransactionRequest.cbor`,
  convert the hex to raw bytes and then base64. Confirm the conversion before use.
- Represent 64-bit integer values as quoted decimal strings when precision could otherwise be
  lost. Responses commonly encode them as JSON strings.
- Encode enums with symbolic names such as `"MAINNET"` and `"CONWAY"`.
- Encode repeated fields as arrays and maps as JSON objects.
- Set at most one member of a protobuf `oneof`.
- Omit absent optional fields rather than inventing defaults.
- Use `-allow-unknown-fields` only for a deliberate compatibility reason, never to hide an
  invalid request.

## Streams And Waiting Calls

`MonitorAddress` and `MonitorNativeAssets` are server-streaming methods.
`MonitorPaymentAddress` is unary but may wait for its request timeout.

- Add `-max-time` unless the user explicitly requests an unbounded stream.
- Set the command execution timeout longer than the `grpcurl` maximum time.
- Preserve cursor values such as `startAfterTxId` and `startAfterId` when resuming.
- Do not retry a stream automatically; a retry can duplicate observed events.
- A `DeadlineExceeded` status after receiving requested events can be the expected end of a
  deliberately bounded stream. Explain this rather than reporting it as an unconditional
  service failure.

Example bounded stream:

```bash
grpcurl \
  -plaintext \
  -max-time 30 \
  -expand-headers \
  -import-path newm-chain-grpc/src/main/proto \
  -proto newm_chain.proto \
  -H 'Authorization: Bearer ${NEWM_CHAIN_JWT}' \
  -d '{"address":"addr1..."}' \
  localhost:3737 \
  newmchain.NewmChain/MonitorAddress
```

## Safety Classification

The following methods change external or coordination state:

- `SubmitTransaction`
- `AcquireMutex`
- `ReleaseMutex`

Before invoking any of them:

1. Require explicit user authorization for that exact method and target. Do not infer approval
   from a general request to test gRPC access.
2. Show the endpoint, target, network, and a redacted request summary immediately before the
   call.
3. For `SubmitTransaction`, call `IsMainnet` first and explicitly state whether the transaction
   would be submitted to mainnet or a test network.
4. Never submit a transaction merely to test connectivity.
5. Never retry a failed, timed-out, or ambiguous submission automatically. Query the transaction
   or investigate the returned status first.
6. Confirm the exact mutex name before acquiring or releasing a mutex.

Do not invoke these state-changing methods in read-only or plan mode. All other currently defined
methods are non-mutating, but they can still be expensive, long-running, or sensitive. In
particular, wildcard or regex `SnapshotNativeAssets` requests may be expensive.

## Sensitive Request Data

- `TransactionBuilder` may contain private signing keys and signatures. Building does not submit
  the transaction, but its inputs can be secret.
- `DeriveWalletAddresses` receives an account extended public key. It is not a private key, but
  treat it as sensitive wallet data.
- Transaction CBOR, signatures, JWTs, and keys must be redacted from progress updates and final
  responses unless the user explicitly needs a non-secret value reproduced.
- Avoid putting private material directly in command arguments when an environment- or
  file-based mechanism is available.
- Never confuse `TransactionBuilder` with `SubmitTransaction`; only the latter broadcasts a
  transaction.

## Result Reporting

After a call:

- State the target and fully qualified method.
- Summarize the request without credentials, keys, signatures, or full transaction CBOR.
- Return the response verbatim when it is reasonably sized; otherwise summarize it and retain
  identifiers needed by the user.
- Preserve transaction hashes, output indexes, addresses, lovelace amounts, policies, and asset
  names exactly.
- Mention that protobuf 64-bit values are often rendered as JSON strings when that affects
  interpretation.
- State whether the call was read-only or state-changing.
- Never include the JWT in the response.

## Troubleshooting

- `server does not support the reflection API`: provide the repository `-import-path` and
  `-proto`; do not depend on reflection.
- `permission denied` opening the proto with a Snap-installed `grpcurl`: inspect
  `snap connections grpcurl`. Report the confinement issue and request approval before changing
  system permissions or copying files.
- `Unauthenticated`: verify that the bearer header exists and that the token is valid for the
  configured issuer and audience. Do not display the token.
- `Unavailable`: verify the process, host, port, and plaintext/TLS selection.
- `unknown service` or `unknown method`: list methods from the current local proto and use the
  fully qualified `newmchain.NewmChain/<Method>` name.
- JSON parse or unknown-field errors: describe the request with `-msg-template` and correct the
  JSON instead of enabling permissive parsing by default.
- Incorrect `bytes` values: verify that protobuf JSON contains base64, not source hex.
- Deadline errors: distinguish connection failures from intentionally bounded streams or waiting
  methods.
- Oversized responses: increase `-max-msg-sz` only when the requested method justifies it.

## Completion Checklist

- The method and request were discovered from the current proto.
- The target and transport mode are correct.
- Authentication succeeded without exposing the JWT.
- The request obeys protobuf JSON encoding rules.
- Any state-changing call received explicit confirmation and passed its additional safeguards.
- Streaming calls have an intentional time bound or explicit approval to remain open.
- The reported result is accurate and redacts sensitive values.
