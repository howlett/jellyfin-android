package org.jellyfin.mobile.api

import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.api.client.ApiClient
import org.jellyfin.apiclient.api.client.KtorClient
import org.jellyfin.apiclient.api.operations.*
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Requires another module to define the [Jellyfin] instance.
 */
val apiModule = module {
    single { get<Jellyfin>().createApi() } binds arrayOf(KtorClient::class, ApiClient::class)

    // Add app and device information
    factory { get<KtorClient>().deviceInfo }
    factory { get<KtorClient>().clientInfo }

    // Add API modules
    single { SystemApi(get()) }
    single { ImageApi(get()) }
    single { PlaystateApi(get()) } // Rename to PlayStateApi plz
    single { ItemsApi(get()) }
    single { UserViewsApi(get()) }
    single { ArtistsApi(get()) }
    single { GenresApi(get()) }
    single { PlaylistsApi(get()) }
    single { UniversalAudioApi(get()) }
}
