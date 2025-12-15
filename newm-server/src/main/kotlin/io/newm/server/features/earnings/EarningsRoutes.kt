package io.newm.server.features.earnings

import com.google.iot.cbor.CborInteger
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.chain.util.asStakeAddress
import io.newm.chain.util.toHexString
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.earnings.model.AddSongRoyaltyRequest
import io.newm.server.features.earnings.model.ClaimOrderRequest
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.earnings.model.GetEarningsBySongIdResponse
import io.newm.server.features.earnings.model.GetEarningsResponse
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.songId
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.server.typealiases.SongId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import io.newm.shared.ktx.toLocalDateTime
import java.util.UUID

private const val EARNINGS_PATH = "v1/earnings"
private const val EARNINGS_PATH_ADMIN = "v1/earnings/admin"

internal val ISRC_REGEX = Regex("^[A-Z]{2}-?\\w{3}-?\\d{2}-?\\d{5}$", RegexOption.IGNORE_CASE)

internal suspend fun resolveSongId(
    identifier: String,
    songRepository: SongRepository
): SongId {
    // Try to parse as UUID first
    try {
        return UUID.fromString(identifier)
    } catch (_: IllegalArgumentException) {
        // Not a UUID, try ISRC
    }

    // Validate ISRC format
    if (!ISRC_REGEX.matches(identifier)) {
        throw IllegalArgumentException("Invalid identifier: must be a valid UUID or ISRC")
    }

    // Lookup by ISRC
    val song = songRepository.getByIsrc(identifier)
        ?: throw NoSuchElementException("No song found with ISRC: $identifier")

    return song.id!!
}

fun Routing.createEarningsRoutes() {
    val configRepository: ConfigRepository by inject()
    val cardanoRepository: CardanoRepository by inject()
    val earningsRepository: EarningsRepository by inject()
    val recaptchaRepository: RecaptchaRepository by inject()
    val songRepository: SongRepository by inject()

    authenticate(AUTH_JWT_ADMIN) {
        route(EARNINGS_PATH_ADMIN) {
            get {
                val earnings = earningsRepository.getAll()
                respond(earnings)
            }
            // create earning records
            post {
                val earnings = receive<List<Earning>>()
                earningsRepository.addAll(earnings)
                respond(HttpStatusCode.Created)
            }
            get("{songIdOrIsrc}") {
                // get earnings by song id or ISRC
                val resolvedSongId = resolveSongId(parameters["songIdOrIsrc"]!!, songRepository)
                val earnings = earningsRepository.getAllBySongId(resolvedSongId)
                respond(earnings)
            }
            post("{songIdOrIsrc}") {
                // create earning records for a song based on receiving a total amount of royalties.
                // Accepts either a songId (UUID) or ISRC as the path parameter.
                val resolvedSongId = resolveSongId(parameters["songIdOrIsrc"]!!, songRepository)
                val royaltyRequest: AddSongRoyaltyRequest = receive()
                earningsRepository.addRoyaltySplits(resolvedSongId, royaltyRequest)
                respond(HttpStatusCode.Created)
            }
        }
    }

    // Claiming is un-authenticated, but we still check recaptcha to prevent bots
    authenticate(AUTH_JWT, optional = true) {
        route(EARNINGS_PATH) {
            // get earnings
            get("{walletAddress}") {
                val stakeAddress = parameters["walletAddress"]!!.asStakeAddress(cardanoRepository.isMainnet())
                recaptchaRepository.verify("get_earnings", request)
                val earnings = earningsRepository.getAllByStakeAddress(stakeAddress).filter { it.isActive }
                val totalClaimed = earnings.filter { it.claimed }.sumOf { it.amount }
                val paymentAmountLovelace = configRepository.getLong(CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE)
                val changeAmountLovelace = 2000000L // 2 ada
                val amountCborHex = CborInteger
                    .create(paymentAmountLovelace + changeAmountLovelace)
                    .toCborByteArray()
                    .toHexString()
                respond(
                    GetEarningsResponse(
                        totalClaimed = totalClaimed,
                        earnings = earnings,
                        amountCborHex = amountCborHex,
                    )
                )
            }
            // get earnings by song id
            get("song/{songId}") {
                recaptchaRepository.verify("get_earnings", request)
                val startDate = parameters["startDate"]?.toLocalDateTime()
                val endDate = parameters["endDate"]?.toLocalDateTime()
                val earnings = earningsRepository.getAllBySongId(songId).filter { earning ->
                    (startDate == null || earning.createdAt >= startDate) &&
                        (endDate == null || earning.createdAt <= endDate)
                }
                val totalAmount = earnings.sumOf { it.amount }
                respond(
                    GetEarningsBySongIdResponse(
                        totalAmount = totalAmount,
                        earnings = earnings
                    )
                )
            }
            // create a claim for all earnings on this wallet stake address
            post {
                val claimOrderRequest = receive<ClaimOrderRequest>()
                recaptchaRepository.verify("post_earnings", request)
                val claimOrder = earningsRepository.createClaimOrder(claimOrderRequest)

                if (claimOrder != null) {
                    respond(claimOrder)
                } else {
                    respond(HttpStatusCode.NotFound, "No unclaimed earnings found for this wallet address.")
                }
            }
        }
    }
}
