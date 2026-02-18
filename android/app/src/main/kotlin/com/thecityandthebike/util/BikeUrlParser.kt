package com.thecityandthebike.util

import java.net.URI

data class ParsedBikeUrl(val provider: String?, val bikeId: String)

val WHITELISTED_PROVIDERS = setOf("lime", "bird")

/**
 * Parse a bike QR code value into a provider and bike ID.
 *
 * Rules:
 * - lime.bike hostname → provider="lime", id=last path segment with '=' padding stripped
 * - ride.bird.co hostname → provider="bird", id=last path segment
 * - Anything else → provider=null, id=raw value as-is
 */
fun parseBikeUrl(raw: String): ParsedBikeUrl {
    val parsed = try {
        URI(raw)
    } catch (_: Exception) {
        return ParsedBikeUrl(provider = null, bikeId = raw)
    }

    val scheme = parsed.scheme
    val hostname = parsed.host
    if (scheme.isNullOrEmpty() || hostname.isNullOrEmpty()) {
        return ParsedBikeUrl(provider = null, bikeId = raw)
    }

    val pathSegments = (parsed.path ?: "").split("/").filter { it.isNotEmpty() }
    if (pathSegments.isEmpty()) {
        return ParsedBikeUrl(provider = null, bikeId = raw)
    }

    val lastSegment = pathSegments.last()

    return when (hostname) {
        "lime.bike" -> ParsedBikeUrl(provider = "lime", bikeId = lastSegment.trimEnd('='))
        "ride.bird.co" -> ParsedBikeUrl(provider = "bird", bikeId = lastSegment)
        else -> ParsedBikeUrl(provider = null, bikeId = raw)
    }
}

fun isRecognizedProvider(raw: String): Boolean {
    return parseBikeUrl(raw).provider in WHITELISTED_PROVIDERS
}
