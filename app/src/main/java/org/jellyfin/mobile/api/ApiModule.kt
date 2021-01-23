package org.jellyfin.mobile.api

import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.api.client.KtorClient
import org.jellyfin.apiclient.api.operations.SystemApi
import org.jellyfin.apiclient.api.operations.UserApi
import org.koin.dsl.module

/**
 * Requires another module to define the [Jellyfin] instance.
 */
val apiModule = module {
    single { get<Jellyfin>().createApi() }

    // Add app and device information
    factory { get<KtorClient>().deviceInfo }
    factory { get<KtorClient>().clientInfo }

    // Add API modules
    single { SystemApi(get()) }
    single { UserApi(get()) }
}
