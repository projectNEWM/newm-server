package io.newm.chain.cardano

import io.newm.chain.config.Config
import io.newm.chain.util.Blake2b
import io.newm.chain.util.Constants.BYRON_TO_SHELLEY_EPOCHS_GUILD
import io.newm.chain.util.Constants.BYRON_TO_SHELLEY_EPOCHS_MAINNET
import io.newm.chain.util.Constants.BYRON_TO_SHELLEY_EPOCHS_PREPROD
import io.newm.chain.util.Constants.BYRON_TO_SHELLEY_EPOCHS_PREVIEW
import io.newm.chain.util.Constants.NETWORK_MAGIC_GUILD
import io.newm.chain.util.Constants.NETWORK_MAGIC_MAINNET
import io.newm.chain.util.Constants.NETWORK_MAGIC_PREPROD
import io.newm.chain.util.Constants.NETWORK_MAGIC_PREVIEW
import io.newm.chain.util.toHexString
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.streams.asSequence

val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC))

private val source by lazy { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890`~!@#$%^&*()_-=+;:><?[]{}" }
private val random by lazy { SecureRandom() }
fun randomString(length: Long) =
    random.ints(length, 0, source.length)
        .asSequence()
        .map(source::get)
        .joinToString("")

fun randomHex(numBytes: Int) = ByteArray(numBytes).apply {
    random.nextBytes(this)
}.toHexString()

fun randomPercentage(bound: Int) = random.nextInt(bound)

val byronGenesis by lazy {
    when (shelleyGenesis.networkMagic) {
        NETWORK_MAGIC_MAINNET -> ByronGenesis(
            startTime = 1506203091L,
            protocolConsts = ProtocolConsts(k = 2160L),
            blockVersionData = BlockVersionData(slotDuration = 20000L)
        )

        NETWORK_MAGIC_GUILD -> ByronGenesis(
            startTime = 1639090522L,
            protocolConsts = ProtocolConsts(k = 36L),
            blockVersionData = BlockVersionData(slotDuration = 100L)
        )

        NETWORK_MAGIC_PREPROD -> ByronGenesis(
            startTime = 1654041600L,
            protocolConsts = ProtocolConsts(k = 2160L),
            blockVersionData = BlockVersionData(slotDuration = 20000L)
        )

        NETWORK_MAGIC_PREVIEW -> ByronGenesis(
            startTime = 1666656000L,
            protocolConsts = ProtocolConsts(k = 432L),
            blockVersionData = BlockVersionData(slotDuration = 20000L)
        )

        else -> throw IllegalStateException("Unknown network magic: ${shelleyGenesis.networkMagic}")
    }
}

val shelleyGenesis by lazy {
    Config.genesis
}

private val genesisStartTimeSec by lazy {
    shelleyGenesis.systemStart.epochSeconds
}

val byronToShelleyEpochs by lazy {
    when (shelleyGenesis.networkMagic) {
        NETWORK_MAGIC_MAINNET -> BYRON_TO_SHELLEY_EPOCHS_MAINNET
        NETWORK_MAGIC_GUILD -> BYRON_TO_SHELLEY_EPOCHS_GUILD
        NETWORK_MAGIC_PREPROD -> BYRON_TO_SHELLEY_EPOCHS_PREPROD
        NETWORK_MAGIC_PREVIEW -> BYRON_TO_SHELLEY_EPOCHS_PREVIEW
        else -> throw IllegalStateException("Unknown network magic: ${shelleyGenesis.networkMagic}")
    }
}

fun getCurrentEpoch(): Long =
    ((Instant.now().epochSecond) - genesisStartTimeSec) / shelleyGenesis.epochLength

fun getEpochForSlot(slot: Long): Long {
    val shelleyTransitionEpoch = getShelleyTransitionEpoch()
    val networkStartTime = genesisStartTimeSec
    val byronEpochLength = 10 * byronGenesis.protocolConsts.k
    val byronSlots = byronEpochLength * shelleyTransitionEpoch
    val shelleySlots = slot - byronSlots
    val byronSecs = (byronGenesis.blockVersionData.slotDuration * byronSlots) / 1000
    val shelleySecs = shelleySlots * shelleyGenesis.slotLength
    val timeAtSlot = networkStartTime + byronSecs + shelleySecs
    return (timeAtSlot - networkStartTime) / shelleyGenesis.epochLength
}

fun getCurrentSlot(): Long = getSlotAtInstant(Instant.now())

fun getSlotAtInstant(now: Instant): Long {
    val transTimeEnd = genesisStartTimeSec + (byronToShelleyEpochs * shelleyGenesis.epochLength)
    val byronSlots = (genesisStartTimeSec - byronGenesis.startTime) / 20L
    val transSlots = (byronToShelleyEpochs * shelleyGenesis.epochLength) / 20L
    val currentTimeSec = now.epochSecond
    return byronSlots + transSlots + ((currentTimeSec - transTimeEnd) / shelleyGenesis.slotLength)
}

fun getLastSlotOfYear(): Long {
    val transTimeEnd = genesisStartTimeSec + byronToShelleyEpochs * shelleyGenesis.epochLength
    val transSlots = byronToShelleyEpochs * shelleyGenesis.epochLength / 20
    val lastSecondOfYear = LocalDateTime.now(ZoneOffset.UTC)
        .with(TemporalAdjusters.lastDayOfYear())
        .withHour(23)
        .withMinute(59)
        .withSecond(59)
        .toEpochSecond(ZoneOffset.UTC)
    return transSlots + ((lastSecondOfYear - transTimeEnd) / shelleyGenesis.slotLength)
}

fun getFirstSlotOfEpoch(absoluteSlot: Long): Long {
    val shelleyTransitionEpoch = getShelleyTransitionEpoch()
    if (shelleyTransitionEpoch == -1L) {
        return -1L
    }
    val byronEpochLength = 10L * byronGenesis.protocolConsts.k
    val byronSlots = byronEpochLength * shelleyTransitionEpoch
    val shelleySlots = absoluteSlot - byronSlots
    val shelleySlotInEpoch = shelleySlots % shelleyGenesis.epochLength
    return absoluteSlot - shelleySlotInEpoch
}

fun getShelleyTransitionEpoch(): Long {
    val byronEpochLength = 10L * byronGenesis.protocolConsts.k
    var calcSlot = 0L
    var byronEpochs = getCurrentEpoch()
    val slotInEpoch = getSlotInEpoch()
    val slot = getCurrentSlot()
    var shelleyEpochs = 0L
    while (byronEpochs >= 0L) {
        calcSlot = (byronEpochs * byronEpochLength) + (shelleyEpochs * shelleyGenesis.epochLength) + slotInEpoch
        if (calcSlot == slot) {
            break
        }
        byronEpochs--
        shelleyEpochs++
    }

    if (calcSlot != slot || shelleyEpochs == 0L) {
        return -1L
    }

    return byronEpochs
}

fun getSlotInEpoch(): Long =
    shelleyGenesis.epochLength - getTimeUntilNextEpoch()

fun getTimeUntilNextEpoch(): Long =
    shelleyGenesis.epochLength - (Instant.now().epochSecond - genesisStartTimeSec) + (getCurrentEpoch() * shelleyGenesis.epochLength)

fun getInstantAtSlot(absoluteSlot: Long): Instant {
    val transTimeEnd = genesisStartTimeSec + byronToShelleyEpochs * shelleyGenesis.epochLength
    val transSlots = byronToShelleyEpochs * shelleyGenesis.epochLength / 20
    val epochSecond = transTimeEnd + ((absoluteSlot - transSlots) * shelleyGenesis.slotLength)
    return Instant.ofEpochSecond(epochSecond)
}

// fun calculateTransactionId(txBody: CborMap): String {
//    val txBodyCborBytes = txBody.toCborByteArray()
//    return calculateTransactionId(txBodyCborBytes)
// }

fun calculateTransactionId(txBodyCborBytes: ByteArray): String {
    return Blake2b.hash256(txBodyCborBytes).toHexString()
}
