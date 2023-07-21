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
    suspend fun putString(id: String, value: String)

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
        const val CONFIG_KEY_SCHEDULER_EVEARA_SYNC = "scheduler.evearaSync"
        const val CONFIG_KEY_EVEARA_SERVER = "eveara.server"
        const val CONFIG_KEY_EVEARA_CLIENT_ID = "eveara.clientId"
        const val CONFIG_KEY_EVEARA_CLIENT_SECRET = "eveara.clientSecret"
        const val CONFIG_KEY_EVEARA_PARTNER_SUBSCRIPTION_ID = "eveara.partnerSubscriptionId"
        const val CONFIG_KEY_EVEARA_NEWM_EMAIL = "eveara.newmEmail"
        const val CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES = "eveara.statusCheckMinutes"
    }
}
