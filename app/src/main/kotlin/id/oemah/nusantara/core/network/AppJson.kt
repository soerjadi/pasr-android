package id.oemah.nusantara.core.network

import kotlinx.serialization.json.Json

/** Lenient on unknown fields so the server can add fields without breaking old app versions. */
val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
