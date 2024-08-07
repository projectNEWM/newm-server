package io.newm.server.config.repo

interface ConfigRepository {
    suspend fun exists(id: String): Boolean

    suspend fun getString(id: String): String

    suspend fun getStrings(id: String): List<String>

    suspend fun getLong(id: String): Long

    suspend fun getLongs(id: String): List<Long>

    suspend fun getInt(id: String): Int

    suspend fun getInts(id: String): List<Int>

    suspend fun getBoolean(id: String): Boolean

    suspend fun getBooleans(id: String): List<Boolean>

    suspend fun getDouble(id: String): Double

    suspend fun getDoubles(id: String): List<Double>

    suspend fun putString(
        id: String,
        value: String
    )

    fun invalidateCache()

    companion object {
        const val CONFIG_KEY_ENCRYPTION_SALT = "encryption.salt"
        const val CONFIG_KEY_ENCRYPTION_PASSWORD = "encryption.password"
        const val CONFIG_KEY_MINT_PRICE = "mint.price"
        const val CONFIG_KEY_MINT_CASH_REGISTER_MIN_AMOUNT = "mint.cashRegisterMinAmount"
        const val CONFIG_KEY_MINT_CASH_REGISTER_COLLECTION_AMOUNT = "mint.cashRegisterCollectionAmount"
        const val CONFIG_KEY_MINT_CIP68_POLICY = "mint.cip68Policy"
        const val CONFIG_KEY_MINT_CIP68_SCRIPT_ADDRESS = "mint.cip68ScriptAddress"
        const val CONFIG_KEY_MINT_SCRIPT_UTXO_REFERENCE = "mint.scriptUtxoReference"
        const val CONFIG_KEY_MINT_STARTER_TOKEN_UTXO_REFERENCE = "mint.starterTokenUtxoReference"
        const val CONFIG_KEY_MINT_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN = "mint.monitorPaymentAddressTimeoutMin"
        const val CONFIG_KEY_MINT_LEGACY_POLICY_IDS = "mint.legacyPolicyIds"
        const val CONFIG_KEY_MINT_ALL_POLICY_IDS = "mint.allPolicyIds"
        const val CONFIG_KEY_SCHEDULER_EVEARA_SYNC = "scheduler.evearaSync"
        const val CONFIG_KEY_EVEARA_SERVER = "eveara.server"
        const val CONFIG_KEY_EVEARA_CLIENT_ID = "eveara.clientId"
        const val CONFIG_KEY_EVEARA_CLIENT_SECRET = "eveara.clientSecret"
        const val CONFIG_KEY_EVEARA_PARTNER_SUBSCRIPTION_ID = "eveara.partnerSubscriptionId"
        const val CONFIG_KEY_EVEARA_NEWM_EMAIL = "eveara.newmEmail"
        const val CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES = "eveara.statusCheckMinutes"
        const val CONFIG_KEY_EVEARA_STATUS_CHECK_REFIRE = "eveara.statusCheckDeclinedMaxRefire"
        const val CONFIG_KEY_EMAIL_WHITELIST = "email.whitelist"
        const val CONFIG_KEY_DISTRIBUTION_PRICE_USD = "distribution.price.usd"
        const val CONFIG_KEY_OUTLET_STATUS_CHECK_MINUTES = "outlet.statusCheckMinutes"
        const val CONFIG_KEY_RECAPTCHA_ENABLED = "recaptcha.enabled"
        const val CONFIG_KEY_RECAPTCHA_MIN_SCORE = "recaptcha.minScore"
        const val CONFIG_KEY_NFTCDN_ENABLED = "nftcdn.enabled"
        const val CONFIG_KEY_MARKETPLACE_MONITORING_MULTI_MODE_ENABLED = "marketplace.monitoringMultiModeEnabled"
        const val CONFIG_KEY_MARKETPLACE_MONITORING_RETRY_DELAY = "marketplace.monitoringRetryDelay"
        const val CONFIG_KEY_MARKETPLACE_SALE_CONTRACT_ADDRESS = "marketplace.saleContractAddress"
        const val CONFIG_KEY_MARKETPLACE_QUEUE_CONTRACT_ADDRESS = "marketplace.queueContractAddress"
        const val CONFIG_KEY_MARKETPLACE_PENDING_SALE_TTL = "marketplace.pendingSaleTimeToLive"
        const val CONFIG_KEY_MARKETPLACE_PENDING_ORDER_TTL = "marketplace.pendingOrderTimeToLive"
        const val CONFIG_KEY_MARKETPLACE_ORDER_LOVELACE = "marketplace.orderLovelace"
        const val CONFIG_KEY_MARKETPLACE_SALE_LOVELACE = "marketplace.saleLovelace"
        const val CONFIG_KEY_MARKETPLACE_POINTER_POLICY_ID = "marketplace.pointerPolicyId"
        const val CONFIG_KEY_MARKETPLACE_POINTER_ASSET_NAME_PREFIX = "marketplace.pointerAssetNamePrefix"
        const val CONFIG_KEY_MARKETPLACE_CURRENCY_POLICY_ID = "marketplace.currencyPolicyId"
        const val CONFIG_KEY_MARKETPLACE_CURRENCY_ASSET_NAME = "marketplace.currencyAssetName"
        const val CONFIG_KEY_MARKETPLACE_INCENTIVE_MIN_AMOUNT = "marketplace.incentiveMinAmount"
        const val CONFIG_KEY_MARKETPLACE_SALE_REFERENCE_INPUT_UTXOS = "marketplace.saleReferenceInputUtxos"
        const val CONFIG_KEY_EARNINGS_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN = "earnings.monitorPaymentAddressTimeoutMin"
        const val CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE = "earnings.claimOrderFee"
        const val CONFIG_KEY_NEWM_PLAYLIST_ID = "newm.playlist.id"
        const val CONFIG_KEY_CLIENT_CONFIG_STUDIO = "clientConfig.studio"
        const val CONFIG_KEY_CLIENT_CONFIG_MARKETPLACE = "clientConfig.marketplace"
        const val CONFIG_KEY_CLIENT_CONFIG_MOBILE = "clientConfig.mobile"
    }
}
