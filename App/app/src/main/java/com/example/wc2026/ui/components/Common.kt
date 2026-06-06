package com.carldong.fifa.worldcup2026.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.carldong.fifa.worldcup2026.theme.*
import java.net.URLEncoder

private fun assetUri(path: String): String {
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    if (path.startsWith("//")) return "https:$path"
    val encodedPath = path.split("/").joinToString("/") { segment ->
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
    }
    return "file:///android_asset/$encodedPath"
}

/** Team badge image from pic/team/ */
@Composable
fun TeamBadge(imageFile: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data("file:///android_asset/pic/team/$imageFile")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun AssetImage(path: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val ctx = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data(assetUri(path))
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
}

/** Player avatar from pic/player/ (avatarFile is full path like "pic/player/player_1.png") */
@Composable
fun PlayerAvatar(avatarFile: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    if (avatarFile.isNotEmpty()) {
        // Encode spaces in filename for file:// URI — Coil needs percent-encoded paths
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(assetUri(avatarFile))
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = modifier,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Blue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            error = {
                Box(Modifier.fillMaxSize().background(Color(0x1F787880)))
            }
        )
    }
}

/** Club logo from pic/club/ */
@Composable
fun ClubLogo(clubFile: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    if (clubFile.isNotEmpty()) {
        val data = if (
            clubFile.startsWith("http://") ||
            clubFile.startsWith("https://") ||
            clubFile.startsWith("//")
        ) {
            assetUri(clubFile)
        } else {
            assetUri("pic/club/$clubFile")
        }
        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(data)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}

fun clubToLogoFile(club: String): String = when (club.trim()) {
    "Real Madrid" -> "club_real_madrid.png"
    "Inter Miami" -> "club_inter_miami.png"
    "Liverpool" -> "9Liverpool.png"
    "Barcelona", "FC Barcelona" -> "241FC Barcelona.png"
    "Man City", "Manchester City" -> "10Manchester City.png"
    "Monaco", "AS Monaco" -> "69AS Monaco.png"
    "Manchester United" -> "11Manchester United.png"
    "Palmeiras" -> "Palmeiras.png"
    "Milan", "Milano FC", "Milano Fc" -> "131681Milano FC.png"
    "Inter Milan" -> "Inter Milan.png"
    "Lombardia FC", "Lombardia Fc" -> "131682Lombardia FC.png"
    "Bayern Munich", "FC Bayern München", "FC Bayern Munchen" -> "21FC Bayern München.png"
    "Arsenal" -> "club_arsenal.png"
    "Galatasaray" -> "club_galatasaray.png"
    "Atlético Madrid" -> "club_atletico_madrid.png"
    "Fiorentina" -> "club_fiorentina.png"
    "Brighton" -> "club_brighton.png"
    "Paris SG", "Paris Saint-Germain", "Paris Saint Germain", "Paris SG FC" -> "73Paris SG.png"
    else -> ""
}

fun clubCountryToFlagFile(country: String): String = when (country.trim().lowercase()) {
    "england", "the england" -> "England.png"
    "spain" -> "Spain.png"
    "italy" -> "Italy.png"
    "germany" -> "Germany.png"
    "france" -> "France.png"
    "portugal" -> "Portugal.png"
    "brazil" -> "Brazil.png"
    "netherlands", "the netherlands", "holland" -> "Netherlands.png"
    "saudi arabia" -> "Saudi Arabia.png"
    "turkey" -> "Turkey.png"
    "belgium" -> "Belgium.png"
    "united states", "the united states", "usa", "mls" -> "United States.png"
    "argentina" -> "Argentina.png"
    "russia" -> "Russia.png"
    "ukraine" -> "Ukraine.png"
    "croatia" -> "Croatia.png"
    "switzerland" -> "Switzerland.png"
    "denmark" -> "Denmark.png"
    "norway" -> "Norway.png"
    "sweden" -> "Sweden.png"
    "scotland" -> "Scotland.png"
    "greece" -> "Greece.png"
    "austria" -> "Austria.png"
    "czech republic", "the czech republic" -> "Czech Republic.png"
    "poland" -> "Poland.png"
    "mexico" -> "Mexico.png"
    "colombia" -> "Colombia.png"
    "uruguay" -> "Uruguay.png"
    "chile" -> "Chile.png"
    "japan" -> "Japan.png"
    "south korea", "korea republic" -> "South Korea.png"
    "china pr", "china" -> "China Pr.png"
    "morocco" -> "Morocco.png"
    "senegal" -> "Senegal.png"
    "nigeria" -> "Nigeria.png"
    "egypt" -> "Egypt.png"
    "cameroon" -> "Cameroon.png"
    "hungary" -> "Hungary.png"
    "serbia" -> "Serbia.png"
    "romania" -> "Romania.png"
    "canada" -> "Canada.png"
    else -> ""
}

/** Country flag image from pic/country/ */
@Composable
fun CountryFlag(flagFile: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data("file:///android_asset/pic/country/$flagFile")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

/** Map team.name (from teams.json) to the badge PNG filename in pic/team/ */
fun teamBadgeFile(name: String): String = when (name) {
    "USA"                            -> "United States.png"
    "Czech", "Czechia"               -> "Czech Republic.png"
    "Bosnia & Herzegovina",
    "Bosnia and Herzegovina"         -> "Bosnia and Herzegovina.png"
    "Türkiye", "Turkey"              -> "Turkey.png"
    "Curaçao", "Curacao"             -> "Curacao.png"
    else                             -> "$name.png"
}

fun rankBarColor(rank: Int): Color = when {
    rank <= 15  -> Blue
    rank <= 40  -> Green
    rank <= 80  -> Orange
    else        -> Gray
}

fun ovrColor(ovr: Int): Color = when {
    ovr >= 90 -> Gold
    ovr >= 85 -> Blue
    ovr >= 80 -> Color(0xFF1C7A3E)
    else      -> Gray
}

@Composable
fun RankBar(rank: Int, maxWidth: Dp = 36.dp) {
    val targetFraction = (1f - (rank - 1).toFloat() / 199f).coerceIn(0.02f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "rankBar"
    )
    val w = (maxWidth.value * animatedFraction).coerceAtLeast(4f).dp
    val h = when {
        rank <= 15  -> 4.dp
        rank <= 40  -> 3.dp
        rank <= 80  -> 2.5.dp
        else        -> 2.dp
    }
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(RoundedCornerShape(2.dp))
            .background(rankBarColor(rank))
    )
}

@Composable
fun PosBadge(pos: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (pos) {
        "GK" -> Color(0x26FF9500) to Color(0xFFCC7A00)
        "DF" -> Color(0x1F34C759) to Color(0xFF1A7A38)
        "MF" -> Color(0x1F007AFF) to Color(0xFF005EC4)
        "FW" -> Color(0x1AFF3B30) to Color(0xFFCC2020)
        else -> Color(0x1F8E8E93) to Gray
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(pos, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LiveDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "liveDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotScale"
    )
    Box(
        modifier = modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(Red.copy(alpha = alpha))
    )
}

