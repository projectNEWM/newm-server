package io.newm.server.cors

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.RequestConnectionPoint
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.PluginBuilder
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.util.CaseInsensitiveSet
import io.ktor.util.InternalAPI
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.toLowerCasePreservingASCIIRules
import io.ktor.util.unmodifiable
import io.newm.server.recaptcha.RecaptchaHeaders
import io.newm.shared.ktx.getConfigSplitStrings

private val LOGGER = KtorSimpleLogger("CORS")

fun Application.installCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(RecaptchaHeaders.Platform)
        allowHeader(RecaptchaHeaders.Token)
        allowCredentials = true
        allowNonSimpleContentTypes = true

        this@installCORS.environment.getConfigSplitStrings("cors.hosts").forEach { host ->
            val parts = host.split("://")
            if (parts.size > 1) {
                allowHost(parts[1], listOf(parts[0]))
            } else {
                allowHost(host)
            }
        }
        this@installCORS.log.info("CORS allowed hosts: $hosts")
    }
}

public val CORS: RouteScopedPlugin<CORSConfig> = createRouteScopedPlugin("CORS", ::CORSConfig) {
    buildPlugin()
}

/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * A configuration for the [io.ktor.server.plugins.cors.routing.CORS] plugin.
 */
public class CORSConfig {
    private val wildcardWithDot = "*."

    public companion object {
        /**
         * The default CORS max age value.
         */
        public const val CORS_DEFAULT_MAX_AGE: Long = 24L * 3600 // 1 day

        /**
         * Default HTTP methods that are always allowed by CORS.
         */
        public val CorsDefaultMethods: Set<HttpMethod> = setOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Head)

        /**
         * Default HTTP headers that are always allowed by CORS
         * (simple request headers according to https://www.w3.org/TR/cors/#simple-header).
         * Note that `Content-Type` header simplicity depends on its value.
         */
        public val CorsSimpleRequestHeaders: Set<String> = caseInsensitiveSet(
            HttpHeaders.Accept,
            HttpHeaders.AcceptLanguage,
            HttpHeaders.ContentLanguage,
            HttpHeaders.ContentType
        )

        /**
         * Default HTTP headers that are always allowed by CORS to be used in a response
         * (simple request headers according to https://www.w3.org/TR/cors/#simple-header).
         */
        public val CorsSimpleResponseHeaders: Set<String> = caseInsensitiveSet(
            HttpHeaders.CacheControl,
            HttpHeaders.ContentLanguage,
            HttpHeaders.ContentType,
            HttpHeaders.Expires,
            HttpHeaders.LastModified,
            HttpHeaders.Pragma
        )

        /**
         * The allowed set of content types that are allowed by CORS without preflight check.
         */
        @Suppress("unused")
        public val CorsSimpleContentTypes: Set<ContentType> =
            setOf(
                ContentType.Application.FormUrlEncoded,
                ContentType.MultiPart.FormData,
                ContentType.Text.Plain
            ).unmodifiable()

        @OptIn(InternalAPI::class)
        private fun caseInsensitiveSet(vararg elements: String): Set<String> = CaseInsensitiveSet(elements.asList())
    }

    /**
     * Allowed [CORS] hosts.
     */
    public val hosts: MutableSet<String> = HashSet()

    /**
     * Allowed [CORS] headers.
     */
    @OptIn(InternalAPI::class)
    public val headers: MutableSet<String> = CaseInsensitiveSet()

    /**
     * Allowed [CORS] HTTP methods.
     */
    public val methods: MutableSet<HttpMethod> = HashSet()

    /**
     * Exposed HTTP headers that could be accessed by a client.
     */
    @OptIn(InternalAPI::class)
    public val exposedHeaders: MutableSet<String> = CaseInsensitiveSet()

    /**
     * Allows passing credential information (such as cookies or authentication information)
     * with cross-origin requests.
     * This property sets the `Access-Control-Allow-Credentials` response header to `true`.
     */
    public var allowCredentials: Boolean = false

    /**
     * If present allows any origin matching any of the predicates.
     */
    internal val originPredicates: MutableList<(String) -> Boolean> = mutableListOf()

    /**
     * If present represents the prefix for headers which are permitted in CORS requests.
     */
    public val headerPredicates: MutableList<(String) -> Boolean> = mutableListOf()

    /**
     * Specifies how long the response to the preflight request can be cached
     * without sending another preflight request.
     */
    public var maxAgeInSeconds: Long = CORS_DEFAULT_MAX_AGE
        set(newMaxAge) {
            check(newMaxAge >= 0L) { "maxAgeInSeconds shouldn't be negative: $newMaxAge" }
            field = newMaxAge
        }

    /**
     * Allows requests from the same origin.
     */
    public var allowSameOrigin: Boolean = true

    /**
     * Allows sending requests with non-simple content-types. The following content types are considered simple:
     * - `text/plain`
     * - `application/x-www-form-urlencoded`
     * - `multipart/form-data`
     */
    public var allowNonSimpleContentTypes: Boolean = false

    /**
     * Allows requests from any host.
     */
    public fun anyHost() {
        hosts.add("*")
    }

    /**
     * Allows requests from the specified domains and schemes.
     * A wildcard is supported for either the host or any subdomain.
     * If you specify a wildcard in the host, you cannot add specific subdomains.
     * Otherwise, you can mix wildcard and non-wildcard subdomains as long as
     * the wildcard is always in front of the domain, e.g. `*.sub.domain.com` but not `sub.*.domain.com`.
     *
     * @param host host as it appears in the Host header (e.g. localhost:8080)
     * @param schemes protocols allowed for the origin site; defaults to http and https
     * @param subDomains additional subdomains for the given host
     */
    public fun allowHost(
        host: String,
        schemes: List<String> = listOf("http", "https"),
        subDomains: List<String> = emptyList()
    ) {
        if (host == "*") return anyHost()

        require("://" !in host) { "scheme should be specified as a separate parameter schemes" }

        for (schema in schemes) {
            addHost("$schema://$host")

            for (subDomain in subDomains) {
                validateWildcardRequirements(subDomain)
                addHost("$schema://$subDomain.$host")
            }
        }
    }

    private fun addHost(host: String) {
        validateWildcardRequirements(host)
        hosts.add(host)
    }

    private fun validateWildcardRequirements(host: String) {
        if ('*' !in host) return

        fun String.countMatches(subString: String): Int = windowed(subString.length) { if (it == subString) 1 else 0 }.sum()

        require(wildcardInFrontOfDomain(host)) { "wildcard must appear in front of the domain, e.g. *.domain.com" }
        require(host.countMatches(wildcardWithDot) == 1) { "wildcard cannot appear more than once" }
    }

    private fun wildcardInFrontOfDomain(host: String): Boolean {
        val indexOfWildcard = host.indexOf(wildcardWithDot)
        return wildcardWithDot in host &&
            !host.endsWith(wildcardWithDot) &&
            (indexOfWildcard <= 0 || host.substringBefore(wildcardWithDot).endsWith("://"))
    }

    /**
     * Allows exposing the [header] using `Access-Control-Expose-Headers`.
     * The `Access-Control-Expose-Headers` header adds the specified headers
     * to the allowlist that JavaScript in browsers can access.
     */
    public fun exposeHeader(header: String) {
        if (header !in CorsSimpleResponseHeaders) {
            exposedHeaders.add(header)
        }
    }

    /**
     * Allows using the `X-Http-Method-Override` header for the actual [CORS] request.
     */
    @Suppress("unused")
    public fun allowXHttpMethodOverride() {
        allowHeader(HttpHeaders.XHttpMethodOverride)
    }

    /**
     * Allows using an origin matching [predicate] for the actual [CORS] request.
     */
    public fun allowOrigins(predicate: (String) -> Boolean) {
        this.originPredicates.add(predicate)
    }

    /**
     * Allows using headers prefixed with [headerPrefix] for the actual [CORS] request.
     */
    public fun allowHeadersPrefixed(headerPrefix: String) {
        val prefix = headerPrefix.lowercase()
        this.headerPredicates.add { name -> name.startsWith(prefix) }
    }

    /**
     * Allows using headers matching [predicate] for the actual [CORS] request.
     */
    public fun allowHeaders(predicate: (String) -> Boolean) {
        this.headerPredicates.add(predicate)
    }

    /**
     * Allow using a specified [header] for the actual [CORS] request.
     */
    public fun allowHeader(header: String) {
        if (header.equals(HttpHeaders.ContentType, ignoreCase = true)) {
            allowNonSimpleContentTypes = true
            return
        }

        if (header !in CorsSimpleRequestHeaders) {
            headers.add(header)
        }
    }

    /**
     * Adds a specified [method] to a list of methods allowed by [CORS].
     *
     * Note that CORS operates with real HTTP methods only and
     * doesn't handle method overridden by `X-Http-Method-Override`.
     */
    public fun allowMethod(method: HttpMethod) {
        if (method !in CorsDefaultMethods) {
            methods.add(method)
        }
    }
}

internal fun PluginBuilder<CORSConfig>.buildPlugin() {
    val numberRegex = "[0-9]+".toRegex()
    val allowSameOrigin: Boolean = pluginConfig.allowSameOrigin
    val allowsAnyHost: Boolean = "*" in pluginConfig.hosts
    val allowCredentials: Boolean = pluginConfig.allowCredentials
    val allHeaders: Set<String> =
        (pluginConfig.headers + CORSConfig.CorsSimpleRequestHeaders).let { headers ->
            if (pluginConfig.allowNonSimpleContentTypes) headers else headers.minus(HttpHeaders.ContentType)
        }
    val originPredicates: List<(String) -> Boolean> = pluginConfig.originPredicates
    val headerPredicates: List<(String) -> Boolean> = pluginConfig.headerPredicates
    val methods: Set<HttpMethod> = HashSet(pluginConfig.methods + CORSConfig.CorsDefaultMethods)
    val allHeadersSet: Set<String> = allHeaders.map { it.toLowerCasePreservingASCIIRules() }.toSet()
    val allowNonSimpleContentTypes: Boolean = pluginConfig.allowNonSimpleContentTypes
    val headersList = pluginConfig.headers
        .filterNot { it in CORSConfig.CorsSimpleRequestHeaders }
        .let { if (allowNonSimpleContentTypes) it + HttpHeaders.ContentType else it }
    val methodsListHeaderValue = methods
// *** Ensure that we send ALL methods in the response, not just the ones that are not in the default set
//        .filterNot { it in CORSConfig.CorsDefaultMethods }
        .map { it.value }
        .sorted()
        .joinToString(", ")
    val maxAgeHeaderValue = pluginConfig.maxAgeInSeconds.let { if (it > 0) it.toString() else null }
    val exposedHeaders = when {
        pluginConfig.exposedHeaders.isNotEmpty() -> pluginConfig.exposedHeaders.sorted().joinToString(", ")
        else -> null
    }
    val hostsNormalized = HashSet(
        pluginConfig.hosts
            .filterNot { it.contains('*') }
            .map { normalizeOrigin(it, numberRegex) }
    )
    val hostsWithWildcard = HashSet(
        pluginConfig.hosts
            .filter { it.contains('*') }
            .map {
                val normalizedOrigin = normalizeOrigin(it, numberRegex)
                val (prefix, suffix) = normalizedOrigin.split('*')
                prefix to suffix
            }
    )

    /**
     * A plugin's [call] interceptor that does all the job. Usually there is no need to install it as it is done during
     * a plugin installation.
     */
    onCall { call ->
        if (!allowsAnyHost || allowCredentials) {
            call.corsVary()
        }

        val origin = call.request.headers
            .getAll(HttpHeaders.Origin)
            ?.singleOrNull() ?: return@onCall

        val checkOrigin = checkOrigin(
            origin,
            call.request.origin,
            allowSameOrigin,
            allowsAnyHost,
            hostsNormalized,
            hostsWithWildcard,
            originPredicates,
            numberRegex
        )
        when (checkOrigin) {
            OriginCheckResult.OK -> {
            }

            OriginCheckResult.SkipCORS -> return@onCall
            OriginCheckResult.Failed -> {
                LOGGER.trace("Respond forbidden ${call.request.uri}: origin doesn't match ${call.request.origin}")
                call.respondCorsFailed()
                return@onCall
            }
        }

        if (!allowNonSimpleContentTypes) {
            val contentType = call.request.header(HttpHeaders.ContentType)?.let { ContentType.parse(it) }
            if (contentType != null) {
                if (contentType.withoutParameters() !in CORSConfig.CorsSimpleContentTypes) {
                    LOGGER.trace("Respond forbidden ${call.request.uri}: Content-Type isn't allowed $contentType")
                    call.respondCorsFailed()
                    return@onCall
                }
            }
        }

        if (call.request.httpMethod == HttpMethod.Options) {
            LOGGER.trace("Respond preflight on OPTIONS for ${call.request.uri}")
            call.respondPreflight(
                origin,
                methodsListHeaderValue,
                headersList,
                methods,
                allowsAnyHost,
                allowCredentials,
                maxAgeHeaderValue,
                headerPredicates,
                allHeadersSet
            )
            return@onCall
        }

        if (!call.corsCheckCurrentMethod(methods)) {
            LOGGER.trace("Respond forbidden ${call.request.uri}: method doesn't match ${call.request.httpMethod}")
            call.respondCorsFailed()
            return@onCall
        }

        call.accessControlAllowOrigin(origin, allowsAnyHost, allowCredentials)
        call.accessControlAllowCredentials(allowCredentials)

        if (exposedHeaders != null) {
            call.response.header(HttpHeaders.AccessControlExposeHeaders, exposedHeaders)
        }
    }
}

private enum class OriginCheckResult {
    OK,
    SkipCORS,
    Failed
}

private fun checkOrigin(
    origin: String,
    point: RequestConnectionPoint,
    allowSameOrigin: Boolean,
    allowsAnyHost: Boolean,
    hostsNormalized: Set<String>,
    hostsWithWildcard: Set<Pair<String, String>>,
    originPredicates: List<(String) -> Boolean>,
    numberRegex: Regex
): OriginCheckResult =
    when {
        !isValidOrigin(origin) -> OriginCheckResult.SkipCORS
        allowSameOrigin && isSameOrigin(origin, point, numberRegex) -> OriginCheckResult.SkipCORS
        !corsCheckOrigins(
            origin,
            allowsAnyHost,
            hostsNormalized,
            hostsWithWildcard,
            originPredicates,
            numberRegex
        ) -> OriginCheckResult.Failed

        else -> OriginCheckResult.OK
    }

private suspend fun ApplicationCall.respondPreflight(
    origin: String,
    methodsListHeaderValue: String,
    headersList: List<String>,
    methods: Set<HttpMethod>,
    allowsAnyHost: Boolean,
    allowCredentials: Boolean,
    maxAgeHeaderValue: String?,
    headerPredicates: List<(String) -> Boolean>,
    allHeadersSet: Set<String>
) {
    val requestHeaders = request.headers
        .getAll(HttpHeaders.AccessControlRequestHeaders)
        ?.flatMap { it.split(",") }
        ?.filter { it.isNotBlank() }
        ?.map {
            it.trim().toLowerCasePreservingASCIIRules()
        } ?: emptyList()

    if (!corsCheckRequestMethod(methods)) {
        LOGGER.trace("Return Forbidden for ${this.request.uri}: CORS method doesn't match ${request.httpMethod}")
        respond(HttpStatusCode.Forbidden)
        return
    }

    if (!corsCheckRequestHeaders(requestHeaders, allHeadersSet, headerPredicates)) {
        LOGGER.trace("Return Forbidden for ${this.request.uri}: request has not allowed headers.")
        respond(HttpStatusCode.Forbidden)
        return
    }

    accessControlAllowOrigin(origin, allowsAnyHost, allowCredentials)
    accessControlAllowCredentials(allowCredentials)
    if (methodsListHeaderValue.isNotEmpty()) {
        response.header(HttpHeaders.AccessControlAllowMethods, methodsListHeaderValue)
    }

    val requestHeadersMatchingPrefix = requestHeaders
        .filter { header -> headerMatchesAPredicate(header, headerPredicates) }

    val headersListHeaderValue = (headersList + requestHeadersMatchingPrefix).sorted().joinToString(", ")

    response.header(HttpHeaders.AccessControlAllowHeaders, headersListHeaderValue)
    accessControlMaxAge(maxAgeHeaderValue)

    respond(HttpStatusCode.OK)
}

internal fun ApplicationCall.accessControlAllowOrigin(
    origin: String,
    allowsAnyHost: Boolean,
    allowCredentials: Boolean
) {
    val headerOrigin = if (allowsAnyHost && !allowCredentials) "*" else origin
    response.header(HttpHeaders.AccessControlAllowOrigin, headerOrigin)
}

internal fun ApplicationCall.corsVary() {
    val vary = response.headers[HttpHeaders.Vary]
    val varyValue = if (vary == null) HttpHeaders.Origin else vary + ", " + HttpHeaders.Origin
    response.header(HttpHeaders.Vary, varyValue)
}

internal fun ApplicationCall.accessControlAllowCredentials(allowCredentials: Boolean) {
    if (allowCredentials) {
        response.header(HttpHeaders.AccessControlAllowCredentials, "true")
    }
}

internal fun ApplicationCall.accessControlMaxAge(maxAgeHeaderValue: String?) {
    if (maxAgeHeaderValue != null) {
        response.header(HttpHeaders.AccessControlMaxAge, maxAgeHeaderValue)
    }
}

internal fun isSameOrigin(
    origin: String,
    point: RequestConnectionPoint,
    numberRegex: Regex
): Boolean {
    val requestOrigin = "${point.scheme}://${point.serverHost}:${point.serverPort}"
    return normalizeOrigin(requestOrigin, numberRegex) == normalizeOrigin(origin, numberRegex)
}

internal fun corsCheckOrigins(
    origin: String,
    allowsAnyHost: Boolean,
    hostsNormalized: Set<String>,
    hostsWithWildcard: Set<Pair<String, String>>,
    originPredicates: List<(String) -> Boolean>,
    numberRegex: Regex
): Boolean {
    val normalizedOrigin = normalizeOrigin(origin, numberRegex)
    return allowsAnyHost ||
        normalizedOrigin in hostsNormalized ||
        hostsWithWildcard.any { (prefix, suffix) ->
            normalizedOrigin.startsWith(prefix) && normalizedOrigin.endsWith(suffix)
        } ||
        originPredicates.any { it(origin) }
}

internal fun corsCheckRequestHeaders(
    requestHeaders: List<String>,
    allHeadersSet: Set<String>,
    headerPredicates: List<(String) -> Boolean>
): Boolean =
    requestHeaders.all { header ->
        header in allHeadersSet || headerMatchesAPredicate(header, headerPredicates)
    }

internal fun headerMatchesAPredicate(
    header: String,
    headerPredicates: List<(String) -> Boolean>
): Boolean = headerPredicates.any { it(header) }

internal fun ApplicationCall.corsCheckCurrentMethod(methods: Set<HttpMethod>): Boolean = request.httpMethod in methods

internal fun ApplicationCall.corsCheckRequestMethod(methods: Set<HttpMethod>): Boolean {
    val requestMethod = request.header(HttpHeaders.AccessControlRequestMethod)?.let { HttpMethod(it) }
    return requestMethod != null && requestMethod in methods
}

internal suspend fun ApplicationCall.respondCorsFailed() {
    respond(HttpStatusCode.Forbidden)
}

internal fun isValidOrigin(origin: String): Boolean {
    if (origin.isEmpty()) return false
    if (origin == "null") return true
    if ("%" in origin) return false

    val protoDelimiter = origin.indexOf("://")
    if (protoDelimiter <= 0) return false

    val protoValid = origin[0].isLetter() &&
        origin.subSequence(0, protoDelimiter).all { ch ->
            ch.isLetter() || ch.isDigit() || ch == '-' || ch == '+' || ch == '.'
        }

    if (!protoValid) return false

    var portIndex = origin.length
    for (index in protoDelimiter + 3 until origin.length) {
        val ch = origin[index]
        if (ch == ':' || ch == '/') {
            portIndex = index + 1
            break
        }
        if (ch == '?') return false
    }

    for (index in portIndex until origin.length) {
        if (!origin[index].isDigit()) return false
    }

    return true
}

internal fun normalizeOrigin(
    origin: String,
    numberRegex: Regex
): String {
    if (origin == "null" || origin == "*") return origin

    val builder = StringBuilder(origin.length)
    builder.append(origin)

    if (!origin.substringAfterLast(":", "").matches(numberRegex)) {
        val port = when (origin.substringBefore(':')) {
            "http" -> "80"
            "https" -> "443"
            else -> null
        }

        if (port != null) {
            builder.append(":$port")
        }
    }

    return builder.toString()
}
