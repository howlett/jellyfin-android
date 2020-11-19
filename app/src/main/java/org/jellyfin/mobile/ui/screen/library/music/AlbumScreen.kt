package org.jellyfin.mobile.ui.screen.library.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.VerticalGradient
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jellyfin.mobile.R
import org.jellyfin.mobile.model.dto.AlbumInfo
import org.jellyfin.mobile.ui.DefaultCornerRounding
import org.jellyfin.mobile.ui.ToolbarBackButton
import org.jellyfin.mobile.ui.inject
import org.jellyfin.mobile.ui.screen.AbstractScreen
import org.jellyfin.mobile.ui.utils.ApiImage
import org.jellyfin.mobile.utils.ImageResolver

class AlbumScreen(private val albumInfo: AlbumInfo) : AbstractScreen() {

    @OptIn(ExperimentalUnsignedTypes::class)
    @Composable
    override fun Content() {
        val onPrimaryColor = MaterialTheme.colors.onPrimary
        val backgroundColor = MaterialTheme.colors.background
        var titleColor: Color by remember { mutableStateOf(onPrimaryColor) }
        var gradientBackgroundColor: Color by remember { mutableStateOf(backgroundColor) }
        val imageResolver: ImageResolver by inject()
        rememberCoroutineScope().launch {
            imageResolver.getImagePalette(albumInfo.id, albumInfo.primaryImageTag)?.dominantSwatch?.run {
                titleColor = Color(titleTextColor)
                gradientBackgroundColor = Color(rgb)
            }
        }

        Surface(
            color = MaterialTheme.colors.background,
        ) {
            ScrollableColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.drawBehind {
                        drawRect(
                            brush = VerticalGradient(
                                colors = listOf(gradientBackgroundColor, backgroundColor),
                                startY = 0f,
                                endY = size.height,
                            )
                        )
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TopAppBar(
                        title = {
                            Text(text = albumInfo.name)
                        },
                        navigationIcon = {
                            ToolbarBackButton()
                        },
                        backgroundColor = Color.Transparent,
                        contentColor = titleColor,
                        elevation = 0.dp,
                    )
                    Box(
                        modifier = Modifier.padding(top = 56.dp, bottom = 20.dp),
                    ) {
                        ApiImage(
                            id = albumInfo.id,
                            modifier = Modifier.size(160.dp).clip(DefaultCornerRounding),
                            imageTag = albumInfo.primaryImageTag,
                            fallback = {
                                Image(asset = vectorResource(R.drawable.fallback_image_album_cover))
                            },
                        )
                    }
                    Text(
                        text = albumInfo.name,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h3,
                    )
                    Text(
                        text = albumInfo.artist,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.h5,
                    )
                }
            }
        }
    }
}
