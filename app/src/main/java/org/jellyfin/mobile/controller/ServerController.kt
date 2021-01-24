package org.jellyfin.mobile.controller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.apiclient.api.client.KtorClient
import org.jellyfin.mobile.AppPreferences
import org.jellyfin.mobile.model.sql.dao.ServerDao
import org.jellyfin.mobile.model.sql.dao.UserDao
import org.jellyfin.mobile.model.sql.entity.ServerEntity
import org.jellyfin.mobile.model.sql.entity.ServerUser
import java.util.*

class ServerController(
    private val appPreferences: AppPreferences,
    private val apiClient: KtorClient,
    private val serverDao: ServerDao,
    private val userDao: UserDao,
) {
    /**
     * Migrate from preferences if necessary
     */
    @Suppress("DEPRECATION")
    suspend fun migrateFromPreferences() {
        appPreferences.instanceUrl?.let { url ->
            setupServer(url)
            appPreferences.instanceUrl = null
        }
    }

    suspend fun setupServer(hostname: String) {
        appPreferences.currentServerId = withContext(Dispatchers.IO) {
            serverDao.getServerByHostname(hostname)?.id ?: serverDao.insert(hostname)
        }
    }

    suspend fun setupUser(serverId: Long, userId: String, accessToken: String) {
        appPreferences.currentUserId = withContext(Dispatchers.IO) {
            userDao.upsert(serverId, userId, accessToken)
        }
        appPreferences.currentUserUuid = UUID.fromString(userId.replace("^([a-z\\d]{8})([a-z\\d]{4})([a-z\\d]{4})([a-z\\d]{4})([a-z\\d]{12})\$".toRegex(), "$1-$2-$3-$4-$5")) // TODO Use UUIDSerializer
        apiClient.accessToken = accessToken
    }

    suspend fun loadCurrentServer(): ServerEntity? = withContext(Dispatchers.IO) {
        val serverId = appPreferences.currentServerId ?: return@withContext null
        serverDao.getServer(serverId)
    }

    suspend fun loadCurrentServerUser(): ServerUser? = withContext(Dispatchers.IO) {
        val serverId = appPreferences.currentServerId ?: return@withContext null
        val userId = appPreferences.currentUserId ?: return@withContext null
        userDao.getServerUser(serverId, userId)
    }
}
