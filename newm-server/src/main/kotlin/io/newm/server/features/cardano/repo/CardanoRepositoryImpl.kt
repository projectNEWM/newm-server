package io.newm.server.features.cardano.repo

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.util.DefaultByteBufferPool
import io.newm.chain.grpc.CardanoEra
import io.newm.chain.grpc.CardanoEraRequest
import io.newm.chain.grpc.IsMainnetRequest
import io.newm.chain.grpc.LedgerAssetMetadataItem
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.chain.grpc.MonitorPaymentAddressRequest
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.NewmChainGrpcKt.NewmChainCoroutineStub
import io.newm.chain.grpc.OutputUtxo
import io.newm.chain.grpc.Signature
import io.newm.chain.grpc.SnapshotNativeAssetsResponse
import io.newm.chain.grpc.SubmitTransactionResponse
import io.newm.chain.grpc.TransactionBuilderRequestKt
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.VerifySignDataResponse
import io.newm.chain.grpc.acquireMutexRequest
import io.newm.chain.grpc.datumOrNull
import io.newm.chain.grpc.listOrNull
import io.newm.chain.grpc.mapItemValueOrNull
import io.newm.chain.grpc.mapOrNull
import io.newm.chain.grpc.monitorAddressRequest
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.grpc.queryByNativeAssetRequest
import io.newm.chain.grpc.queryPaymentAddressForStakeAddressRequest
import io.newm.chain.grpc.queryUtxosOutputRefRequest
import io.newm.chain.grpc.queryUtxosRequest
import io.newm.chain.grpc.releaseMutexRequest
import io.newm.chain.grpc.signature
import io.newm.chain.grpc.snapshotNativeAssetsRequest
import io.newm.chain.grpc.submitTransactionRequest
import io.newm.chain.grpc.verifySignDataRequest
import io.newm.chain.util.Bech32
import io.newm.chain.util.Constants
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_TESTNET
import io.newm.chain.util.b64ToByteArray
import io.newm.chain.util.toB64String
import io.newm.chain.util.toHexString
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_ENCRYPTION_PASSWORD
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_ENCRYPTION_SALT
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_ALL_POLICY_IDS
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_NFTCDN_ENABLED
import io.newm.server.features.cardano.database.KeyEntity
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.cardano.database.ScriptAddressWhitelistEntity
import io.newm.server.features.cardano.model.EncryptionRequest
import io.newm.server.features.cardano.model.GetWalletSongsResponse
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.model.CardanoNftSong
import io.newm.server.features.cardano.model.WalletSong
import io.newm.server.features.cardano.parser.toNFTSongs
import io.newm.server.features.cardano.parser.toResourceUrl
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.CHARLI3_ADA_USD_NAME
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.CHARLI3_ADA_USD_NAME_PREPROD
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.CHARLI3_ADA_USD_POLICY
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.CHARLI3_ADA_USD_POLICY_PREPROD
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.CHARLI3_NEWM_USD_NAME
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.CHARLI3_NEWM_USD_NAME_PREPROD
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.CHARLI3_NEWM_USD_POLICY
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.CHARLI3_NEWM_USD_POLICY_PREPROD
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.MUTEX_NAME
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.NEWM_TOKEN_NAME
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.NEWM_TOKEN_NAME_PREPROD
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.NEWM_TOKEN_POLICY
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.NEWM_TOKEN_POLICY_PREPROD
import io.newm.server.features.dripdropz.repo.DripDropzRepository
import io.newm.server.features.nftcdn.repo.NftCdnRepository
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilters
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.walletconnection.database.WalletConnectionEntity
import io.newm.server.ktx.cborHexToUtxo
import io.newm.server.ktx.sign
import io.newm.server.model.FilterCriteria
import io.newm.server.typealiases.UserId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.isValidHex
import io.newm.shared.ktx.isValidPassword
import io.newm.txbuilder.ktx.fingerprint
import io.newm.txbuilder.ktx.mergeAmounts
import io.newm.txbuilder.ktx.toCborObject
import io.newm.txbuilder.ktx.toNativeAssetMap
import java.time.Duration
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.encrypt.BytesEncryptor
import org.springframework.security.crypto.encrypt.Encryptors
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest

internal class CardanoRepositoryImpl(
    private val client: NewmChainCoroutineStub,
    private val kms: KmsAsyncClient,
    private val kmsKeyId: String,
    private val configRepository: ConfigRepository,
    private val nftCdnRepository: NftCdnRepository,
    private val dripDropzRepository: DripDropzRepository
) : CardanoRepository {
    private val logger = KotlinLogging.logger {}

    private val songRepository: SongRepository by inject()

    @VisibleForTesting
    internal var isMainnet: Boolean? = null

    private var bytesEncryptor: BytesEncryptor? = null

    private val oracleMutex = Mutex()

    @VisibleForTesting
    internal val oracleUtxoCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5L))
            .build<String, Utxo>()

    // It's better to have SOME data than none at all, so use the failover cache even if the data is stale.
    private val failoverOracleUtxoCache =
        Caffeine
            .newBuilder()
            .build<String, Utxo>()

    private val cardanoEraCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5L))
            .build<String, CardanoEra>()

    private val scriptAddressWhitelistCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5L))
            .build<Unit, List<String>> {
                transaction {
                    ScriptAddressWhitelistEntity.all().map { it.scriptAddress }
                }
            }

    override suspend fun saveKey(
        key: Key,
        name: String?
    ): UUID {
        logger.debug { "add: key = $key, name: $name" }
        val eSkey = encryptSkey(key.skey)
        return transaction {
            KeyEntity
                .new {
                    address = key.address
                    skey = eSkey
                    vkey = key.vkey.toHexString()
                    script = key.script
                    scriptAddress = key.scriptAddress
                    this.name = name
                }.id.value
        }
    }

    override suspend fun getKey(keyId: UUID): Key {
        logger.debug { "get: keyId = $keyId" }
        val keyEntity = transaction {
            KeyEntity[keyId]
        }
        return keyEntity.toModel(decryptSkey(keyEntity.skey))
    }

    override suspend fun getKeyByName(name: String): Key? {
        logger.debug { "getByName: name = $name" }
        return transaction {
            KeyEntity.find { KeyTable.name eq name }.firstOrNull()
        }?.let {
            it.toModel(decryptSkey(it.skey))
        }
    }

    override suspend fun isMainnet(): Boolean {
        if (isMainnet == null) {
            // Check with newm-chain to see whether we're connected to a mainnet or a testnet
            isMainnet = client.isMainnet(IsMainnetRequest.getDefaultInstance()).isMainnet
        }

        return isMainnet!!
    }

    override suspend fun cardanoEra(): CardanoEra {
        val era = cardanoEraCache.getIfPresent("era")
        if (era != null) {
            return era
        }
        val response = client.queryCardanoEra(CardanoEraRequest.getDefaultInstance()).era
        cardanoEraCache.put("era", response)
        return response
    }

    override suspend fun queryLiveUtxos(address: String): List<Utxo> {
        val response = client.queryLiveUtxos(
            queryUtxosRequest {
                this.address = address
            }
        )
        return response.utxosList
    }

    override fun signTransaction(
        transactionIdBytes: ByteArray,
        signingKeys: List<Key>
    ): List<Signature> =
        signingKeys.map { key ->
            signature {
                vkey = key.vkey.toByteString()
                sig = key.sign(transactionIdBytes).toByteString()
            }
        }

    override fun signTransactionDummy(signingKeys: List<Key>) =
        signingKeys.map { key ->
            signature {
                vkey = key.vkey.toByteString()
                sig = ByteArray(64).toByteString()
            }
        }

    override suspend fun buildTransaction(block: TransactionBuilderRequestKt.Dsl.() -> Unit): TransactionBuilderResponse {
        val request = io.newm.chain.grpc.transactionBuilderRequest {
            block.invoke(this)
        }
        return client.transactionBuilder(request)
    }

    override suspend fun submitTransaction(cborBytes: ByteString): SubmitTransactionResponse {
        val request = submitTransactionRequest {
            cbor = cborBytes
        }
        return client.submitTransaction(request)
    }

    override suspend fun awaitPayment(request: MonitorPaymentAddressRequest) = client.monitorPaymentAddress(request)

    override suspend fun saveEncryptionParams(encryptionRequest: EncryptionRequest) {
        require(encryptionRequest.s.isValidHex()) { "Salt value is not a hex string!" }
        require(encryptionRequest.s.length >= 16) { "Salt value is not long enough!" }
        require(encryptionRequest.password.length > 30) { "Password is not long enough!" }
        require(encryptionRequest.password.isValidPassword()) { "Password must have upper,lower,number characters!" }
        require(!configRepository.exists(CONFIG_KEY_ENCRYPTION_SALT)) { "Salt already exists in config table!" }
        require(!configRepository.exists(CONFIG_KEY_ENCRYPTION_PASSWORD)) { "Password already exists in config table!" }

        val cipherSalt = encryptKmsBytes(encryptionRequest.s.toByteArray())
        val cipherPassword = encryptKmsBytes(encryptionRequest.password.toByteArray())

        configRepository.putString(CONFIG_KEY_ENCRYPTION_SALT, cipherSalt)
        configRepository.putString(CONFIG_KEY_ENCRYPTION_PASSWORD, cipherPassword)
    }

    override suspend fun queryPublicKeyHashByOutputRef(
        hash: String,
        ix: Long
    ): String {
        val response = client.queryPublicKeyHashByOutputRef(
            queryUtxosOutputRefRequest {
                this.hash = hash
                this.ix = ix
            }
        )
        require(response.hasPublicKeyHash()) { "Response does not contain a public key hash!" }
        return response.publicKeyHash
    }

    override suspend fun queryStreamTokenMinUtxo(): Long {
        val outputUtxo = client.calculateMinUtxoForOutput(
            outputUtxo {
                address = Constants.DUMMY_STAKE_ADDRESS
                // lovelace = "0" // auto-calculated
                nativeAssets.add(
                    nativeAsset {
                        policy = Constants.DUMMY_TOKEN_POLICY_ID
                        name = Constants.DUMMY_MAX_TOKEN_NAME
                        amount = "100000000"
                    }
                )
            }
        )
        return outputUtxo.lovelace.toLong()
    }

    override suspend fun calculateMinUtxoForOutput(outputUtxo: OutputUtxo): Long {
        val response = client.calculateMinUtxoForOutput(outputUtxo)
        return response.lovelace.toLong()
    }

    /**
     * Read the Charli3 Oracle token datum value and return the price as a Long.
     */
    private suspend fun queryCharli3OracleFeed(
        cacheKey: String,
        policy: String,
        name: String,
        default: Long = 250000L
    ): Long {
        // We need to lock this operation to prevent too many requests from hitting the newm-chain at the same time
        oracleMutex.withLock {
            val now = System.currentTimeMillis()
            var cachedOracleUtxo = oracleUtxoCache.getIfPresent(cacheKey)
            val failoverOracleUtxo = failoverOracleUtxoCache.getIfPresent(cacheKey)
            try {
                val oracleUtxoStartTimestamp =
                    cachedOracleUtxo
                        ?.datumOrNull
                        ?.listOrNull
                        ?.getListItem(0)
                        ?.listOrNull
                        ?.getListItem(0)
                        ?.mapOrNull
                        ?.getMapItem(1)
                        ?.mapItemValueOrNull
                        ?.int
                        ?: Long.MAX_VALUE
                val oracleUtxoEndTimestamp =
                    cachedOracleUtxo
                        ?.datumOrNull
                        ?.listOrNull
                        ?.getListItem(0)
                        ?.listOrNull
                        ?.getListItem(0)
                        ?.mapOrNull
                        ?.getMapItem(2)
                        ?.mapItemValueOrNull
                        ?.int
                        ?: Long.MIN_VALUE
                if (now < oracleUtxoStartTimestamp || now > oracleUtxoEndTimestamp) {
                    // Outside of range. Get a new oracle utxo
                    cachedOracleUtxo = client.queryUtxoByNativeAsset(
                        queryByNativeAssetRequest {
                            this.policy = policy
                            this.name = name
                        }
                    )
                    oracleUtxoCache.put(cacheKey, cachedOracleUtxo)
                    failoverOracleUtxoCache.put(cacheKey, cachedOracleUtxo)
                }
                val usdPrice = (cachedOracleUtxo ?: failoverOracleUtxo)
                    ?.datumOrNull
                    ?.listOrNull
                    ?.getListItem(0)
                    ?.listOrNull
                    ?.getListItem(0)
                    ?.mapOrNull
                    ?.getMapItem(0)
                    ?.mapItemValueOrNull
                    ?.int
                return usdPrice ?: run {
                    if (isMainnet()) {
                        throw IllegalStateException(
                            "Oracle Utxo does not contain a USD price! - ${(cachedOracleUtxo ?: failoverOracleUtxo)}"
                        )
                    } else {
                        // On testnet, we don't have an oracle utxo, so we just return a default price
                        logger.warn {
                            "Oracle Utxo does not contain a USD price! Using default value: $default - ${cachedOracleUtxo ?: failoverOracleUtxo}"
                        }
                        default
                    }
                }
            } catch (e: Throwable) {
                throw IllegalStateException(
                    "Error parsing oracle feed! for $cacheKey - $policy.$name: ${cachedOracleUtxo ?: failoverOracleUtxo} ${
                        (cachedOracleUtxo ?: failoverOracleUtxo)?.datumOrNull?.toCborObject()?.toCborByteArray()
                            ?.toHexString()
                    }",
                    e
                )
            }
        }
    }

    /**
     * Returns the current ada price in USD as a Long assuming 6 decimal places.
     */
    override suspend fun queryAdaUSDPrice(): Long =
        if (isMainnet()) {
            queryCharli3OracleFeed(
                "ada",
                CHARLI3_ADA_USD_POLICY,
                CHARLI3_ADA_USD_NAME,
            )
        } else {
            queryCharli3OracleFeed(
                "ada",
                CHARLI3_ADA_USD_POLICY_PREPROD,
                CHARLI3_ADA_USD_NAME_PREPROD,
            )
        }

    /**
     * Returns the current NEWM price in USD as a Long assuming 6 decimal places.
     */
    override suspend fun queryNEWMUSDPrice(): Long =
        if (isMainnet()) {
            queryCharli3OracleFeed(
                "newm",
                CHARLI3_NEWM_USD_POLICY,
                CHARLI3_NEWM_USD_NAME,
                5000L,
            )
        } else {
            queryCharli3OracleFeed(
                "newm",
                CHARLI3_NEWM_USD_POLICY_PREPROD,
                CHARLI3_NEWM_USD_NAME_PREPROD,
                5000L,
            )
        }

    override suspend fun queryNativeTokenUSDPrice(
        policyId: String,
        assetName: String
    ): Long {
        if (isNewmToken(policyId, assetName)) {
            return queryNEWMUSDPrice()
        }
        throw IllegalArgumentException("Unsupported token for price API - policyId: $policyId, assetName: $assetName")
    }

    override suspend fun isNewmToken(
        policyId: String,
        assetName: String
    ): Boolean =
        if (isMainnet()) {
            policyId == NEWM_TOKEN_POLICY && assetName == NEWM_TOKEN_NAME
        } else {
            policyId == NEWM_TOKEN_POLICY_PREPROD && assetName == NEWM_TOKEN_NAME_PREPROD
        }

    override suspend fun snapshotToken(
        policyId: String,
        name: String,
    ): SnapshotNativeAssetsResponse =
        client.snapshotNativeAssets(
            snapshotNativeAssetsRequest {
                this.policy = policyId
                this.name = name
            }
        )

    override suspend fun <T> withLock(block: suspend () -> T): T {
        try {
            client.acquireMutex(
                acquireMutexRequest {
                    mutexName = MUTEX_NAME
                    acquireWaitTimeoutMs = 30.minutes.inWholeMilliseconds
                    lockExpiryMs = 1.minutes.inWholeMilliseconds
                }
            )
            return block()
        } finally {
            client.releaseMutex(
                releaseMutexRequest {
                    mutexName = MUTEX_NAME
                }
            )
        }
    }

    override suspend fun verifySignData(
        signatureHex: String,
        publicKeyHex: String
    ): VerifySignDataResponse =
        client.verifySignData(
            verifySignDataRequest {
                this.signatureHex = signatureHex
                this.publicKeyHex = publicKeyHex
            }
        )

    override suspend fun verifySignTransaction(cborHex: String): VerifySignDataResponse =
        client.verifySignTransaction(
            submitTransactionRequest {
                cbor = ByteString.fromHex(cborHex)
            }
        )

    override suspend fun monitorAddress(
        address: String,
        startAfterTxId: String?
    ): Flow<MonitorAddressResponse> =
        client.monitorAddress(
            monitorAddressRequest {
                this.address = address
                startAfterTxId?.let { this.startAfterTxId = it }
            }
        )

    override suspend fun saveScriptAddressToWhitelist(scriptAddress: String) {
        transaction {
            ScriptAddressWhitelistEntity.new {
                this.scriptAddress = scriptAddress
            }
        }
        scriptAddressWhitelistCache.invalidate(Unit)
    }

    override suspend fun getWalletSongs(
        request: List<String>,
        filters: SongFilters,
        offset: Int,
        limit: Int
    ): GetWalletSongsResponse {
        logger.debug { "getWalletSongs: request = $request, filters = $filters, offset = $offset, limit = $limit" }
        val utxos = request.map { it.cborHexToUtxo() }
        val nativeAssetMap = utxos.map { it.nativeAssetsList }.flatten().toNativeAssetMap()
        val streamTokenNames = nativeAssetMap.values.flatten().map { it.name }
        val updatedFilters = filters.copy(nftNames = FilterCriteria(includes = streamTokenNames))
        val count = songRepository.getAllCount(updatedFilters)
        val songs = songRepository
            .getAll(
                filters = updatedFilters,
                offset = offset,
                limit = limit,
            ).map { song: Song ->
                WalletSong(
                    song = song,
                    tokenAmount = nativeAssetMap[song.nftPolicyId]!!.find { it.name == song.nftName }!!.amount!!.toLong(),
                )
            }

        return GetWalletSongsResponse(
            songs = songs,
            total = count,
            offset = offset,
            limit = limit,
        )
    }

    override suspend fun getReceiveAddressForStakeAddress(stakeAddress: String): String? {
        val response = client.queryPaymentAddressForStakeAddress(
            queryPaymentAddressForStakeAddressRequest { this.stakeAddress = stakeAddress }
        )
        if (response.hasPaymentAddress()) {
            // check to see if this is a script receiving address. If so, verify it's in the whitelist
            val decodedPaymentStakeAddress = Bech32.decode(response.paymentAddress)
            return when (decodedPaymentStakeAddress.bytes[0]) {
                PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_MAINNET,
                PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_TESTNET,
                -> response.paymentAddress

                else -> {
                    if (response.paymentAddress in scriptAddressWhitelistCache.get(Unit) ||
                        stakeAddress in scriptAddressWhitelistCache.get(Unit)
                    ) {
                        response.paymentAddress
                    } else {
                        logger.error { "PaymentStakeAddress lookup for $stakeAddress not in whitelist: ${response.paymentAddress}" }
                        null
                    }
                }
            }
        }
        return null
    }

    override suspend fun getWalletNftSongs(
        userId: UserId,
        includeLegacy: Boolean,
        useDripDropz: Boolean
    ): List<CardanoNftSong> {
        val assets = if (useDripDropz) getDripDropzAssets(userId) else getWalletAssets(userId)
        val nftSongs = mutableListOf<CardanoNftSong>()
        val streamTokenPolicyIds = configRepository.getStrings(CONFIG_KEY_MINT_ALL_POLICY_IDS)
        val isNftCdnEnabled = configRepository.getBoolean(CONFIG_KEY_NFTCDN_ENABLED)
        for (asset in assets) {
            val metadata = getAssetMetadata(asset)
            if (includeLegacy || metadata.any { it.key.equals("music_metadata_version", true) }) {
                val isStreamToken = asset.policy in streamTokenPolicyIds
                nftSongs += metadata.toNFTSongs(asset, isStreamToken, isNftCdnEnabled)
            }
        }
        return nftSongs
    }

    override suspend fun getWalletImages(userId: UserId): List<String> {
        val assets = getWalletAssets(userId)
        return if (configRepository.getBoolean(CONFIG_KEY_NFTCDN_ENABLED)) {
            assets.map { nftCdnRepository.generateImageUrl(it.fingerprint()) }
        } else {
            assets.mapNotNull { asset ->
                getAssetMetadata(asset).firstOrNull { it.key == "image" }?.value?.let(String::toResourceUrl)
            }
        }
    }

    private fun getStakeAddressesByUserId(userId: UserId): List<String> =
        transaction {
            WalletConnectionEntity.getAllByUserId(userId).map { it.stakeAddress }
        }

    private suspend fun getWalletAssets(userId: UserId): List<NativeAsset> =
        getStakeAddressesByUserId(userId)
            .flatMap {
                client
                    .queryUtxosByStakeAddress(
                        queryUtxosRequest {
                            address = it
                        }
                    ).utxosList
                    .flatMap { it.nativeAssetsList }
            }.mergeAmounts()

    private suspend fun getDripDropzAssets(userId: UserId): List<NativeAsset> =
        getStakeAddressesByUserId(userId)
            .flatMap { dripDropzRepository.checkAvailableTokens(it) }
            .map {
                nativeAsset {
                    policy = it.tokenPolicy
                    name = it.tokenAssetId
                    amount = it.availableQuantity
                        .movePointRight(6)
                        .toBigInteger()
                        .toString()
                }
            }.mergeAmounts()

    private suspend fun getAssetMetadata(asset: NativeAsset): List<LedgerAssetMetadataItem> =
        client
            .queryLedgerAssetMetadataListByNativeAsset(
                queryByNativeAssetRequest {
                    policy = asset.policy
                    name = asset.name
                }
            ).ledgerAssetMetadataList

    private suspend fun encryptKmsBytes(bytes: ByteArray): String {
        val plaintextBuffer = DefaultByteBufferPool.borrow()
        try {
            plaintextBuffer.put(bytes)
            plaintextBuffer.flip()
            return suspendCoroutine { continuation ->
                val request =
                    EncryptRequest
                        .builder()
                        .keyId(kmsKeyId)
                        .plaintext(SdkBytes.fromByteBuffer(plaintextBuffer))
                        .build()
                kms.encrypt(request).whenComplete { result, throwable ->
                    throwable?.let {
                        continuation.resumeWithException(it)
                    } ?: continuation.resume(result.ciphertextBlob().asByteArray().toB64String())
                }
            }
        } finally {
            DefaultByteBufferPool.recycle(plaintextBuffer)
        }
    }

    val kmsCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).build<String, ByteArray>()

    private suspend fun decryptKmsString(cipherText: String): ByteArray {
        val cachedPlaintext = kmsCache.getIfPresent(cipherText)
        if (cachedPlaintext != null) {
            return cachedPlaintext
        }

        val ciphertextBuffer = DefaultByteBufferPool.borrow()
        try {
            ciphertextBuffer.put(cipherText.b64ToByteArray())
            ciphertextBuffer.flip()

            return suspendCoroutine { continuation ->
                val request =
                    DecryptRequest
                        .builder()
                        .keyId(kmsKeyId)
                        .ciphertextBlob(SdkBytes.fromByteBuffer(ciphertextBuffer))
                        .build()
                kms.decrypt(request).whenComplete { result, throwable ->
                    throwable?.let {
                        continuation.resumeWithException(it)
                    } ?: run {
                        val plaintextBuffer = result.plaintext().asByteBuffer()
                        val plaintext = ByteArray(plaintextBuffer.remaining())
                        plaintextBuffer.get(plaintext)
                        kmsCache.put(cipherText, plaintext)
                        continuation.resume(plaintext)
                    }
                }
            }
        } finally {
            DefaultByteBufferPool.recycle(ciphertextBuffer)
        }
    }

    private suspend fun encryptSkey(bytes: ByteArray): String = getEncryptor().encrypt(bytes).toB64String()

    private suspend fun decryptSkey(ciphertext: String): ByteArray = getEncryptor().decrypt(ciphertext.b64ToByteArray())

    private suspend fun getEncryptor(): BytesEncryptor =
        bytesEncryptor ?: run {
            require(configRepository.exists(CONFIG_KEY_ENCRYPTION_SALT)) { "$CONFIG_KEY_ENCRYPTION_SALT Not found in db!" }
            require(configRepository.exists(CONFIG_KEY_ENCRYPTION_PASSWORD)) { "$CONFIG_KEY_ENCRYPTION_PASSWORD Not found in db!" }
            val cipherTextSalt = configRepository.getString(CONFIG_KEY_ENCRYPTION_SALT)
            val cipherTextPassword = configRepository.getString(CONFIG_KEY_ENCRYPTION_PASSWORD)
            val salt = String(decryptKmsString(cipherTextSalt), Charsets.UTF_8)
            val password = String(decryptKmsString(cipherTextPassword), Charsets.UTF_8)
            bytesEncryptor = Encryptors.stronger(password, salt)
            bytesEncryptor!!
        }
}
