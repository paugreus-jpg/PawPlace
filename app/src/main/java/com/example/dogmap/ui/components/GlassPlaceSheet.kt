package com.example.dogmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dogmap.R
import com.example.dogmap.data.models.Dog
import com.example.dogmap.ui.theme.BrandPrimary

@Composable
fun GlassPlaceSheet(
    dog: Dog,
    isLiked: Boolean,
    isFavorite: Boolean,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onViewDetail: () -> Unit,
    onShare: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xC7142028),
                        Color(0xEB0A1218),
                    )
                )
            )
            .padding(bottom = 22.dp)
    ) {
        Box(
            Modifier
                .padding(horizontal = 80.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, BrandPrimary, Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1F7DD8FF))
                    .border(1.dp, Color(0x447DD8FF), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (dog.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = dog.imageUrl,
                        contentDescription = dog.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = BrandPrimary,
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                if (dog.type.isNotBlank()) {
                    Text(
                        text = dog.type.uppercase(),
                        fontSize = 10.sp,
                        letterSpacing = 1.6.sp,
                        color = BrandPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = dog.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                    color = Color(0xFFF2F6F8),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "★ ${dog.likesCount}",
                        fontSize = 12.sp,
                        color = Color(0xA6F2F6F8)
                    )
                    if (dog.rating > 0) {
                        Text("·", color = Color(0x80F2F6F8))
                        Text(
                            "${dog.rating}/5",
                            fontSize = 12.sp,
                            color = Color(0xA6F2F6F8)
                        )
                    }
                }
            }
        }

        if (dog.description.isNotBlank()) {
            Text(
                text = dog.description,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xB3F2F6F8),
                modifier = Modifier.padding(horizontal = 22.dp),
            )
            Spacer(Modifier.height(16.dp))
        }

        if (dog.authorId.isNotBlank() && dog.authorName.isNotBlank()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onAuthorClick(dog.authorId) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    displayName = dog.authorName,
                    photoUrl = dog.authorPhotoUrl,
                    size = 28.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = dog.authorName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF2F6F8),
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0x14FFFFFF), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onViewDetail,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color(0xFF0A1218)
                )
            ) {
                Text(
                    stringResource(R.string.view_detail),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            GlassIconButton(onClick = onLike) {
                Icon(
                    if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.like_action),
                    tint = if (isLiked) Color(0xFFFF9DB1) else Color(0xCCF2F6F8)
                )
            }
            GlassIconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.favorites),
                    tint = if (isFavorite) BrandPrimary else Color(0xCCF2F6F8)
                )
            }
            GlassIconButton(onClick = onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = Color(0xCCF2F6F8)
                )
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0AFFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
