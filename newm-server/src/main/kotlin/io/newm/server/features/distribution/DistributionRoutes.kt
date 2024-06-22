package io.newm.server.features.distribution

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.features.distribution.model.GetDateResponse
import io.newm.server.features.model.CountResponse
import io.newm.server.ktx.myUserId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get

private const val ROLES_PATH = "v1/distribution/roles"
private const val GENRES_PATH = "v1/distribution/genres"
private const val COUNTRIES_PATH = "v1/distribution/countries"
private const val LANGUAGES_PATH = "v1/distribution/languages"
private const val EARLIEST_RELEASE_DATE_PATH = "v1/distribution/earliest-release-date"

@Suppress("unused")
fun Routing.createDistributionRoutes() {
    val distributionRepository: DistributionRepository by inject()

    authenticate(AUTH_JWT) {
        route(ROLES_PATH) {
            get {
                respond(distributionRepository.getRoles().roles)
            }
            get("count") {
                respond(
                    CountResponse(
                        distributionRepository
                            .getRoles()
                            .roles.size
                            .toLong()
                    )
                )
            }
        }
        route(GENRES_PATH) {
            get {
                respond(distributionRepository.getGenres().genres)
            }
            get("count") {
                respond(
                    CountResponse(
                        distributionRepository
                            .getGenres()
                            .genres.size
                            .toLong()
                    )
                )
            }
        }
        route(COUNTRIES_PATH) {
            get {
                respond(distributionRepository.getCountries().countries)
            }
            get("count") {
                respond(
                    CountResponse(
                        distributionRepository
                            .getCountries()
                            .countries.size
                            .toLong()
                    )
                )
            }
        }
        route(LANGUAGES_PATH) {
            get {
                respond(distributionRepository.getLanguages().languages)
            }
            get("count") {
                respond(
                    CountResponse(
                        distributionRepository
                            .getLanguages()
                            .languages.size
                            .toLong()
                    )
                )
            }
        }
        get(EARLIEST_RELEASE_DATE_PATH) {
            respond(GetDateResponse(distributionRepository.getEarliestReleaseDate(myUserId)))
        }
    }
}
