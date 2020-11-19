package org.jellyfin.mobile.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.ImageOptions
import org.jellyfin.apiclient.model.entities.ImageType

class ImageResolver(
    val context: Context,
    val apiClient: ApiClient,
    val imageLoader: ImageLoader,
) {
    suspend fun getImagePalette(
        id: String,
        imageTag: String?,
        imageType: ImageType = ImageType.Primary
    ): Palette? {
        val url = apiClient.GetImageUrl(id, ImageOptions().apply {
            setImageType(imageType)
            maxWidth = 400
            maxHeight = 400
            quality = 90
            tag = imageTag
        })
        val imageResult = imageLoader.execute(ImageRequest.Builder(context).data(url).build())
        val drawable = imageResult.drawable ?: return null
        return withContext(Dispatchers.IO) {
            val bitmap = drawable.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
            Palette.from(bitmap).generate()
        }
    }
}
