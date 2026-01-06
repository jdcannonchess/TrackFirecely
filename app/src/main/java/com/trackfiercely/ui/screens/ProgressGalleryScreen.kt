package com.trackfiercely.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trackfiercely.ui.theme.*
import com.trackfiercely.util.DateUtils
import com.trackfiercely.viewmodel.ComparePreset
import com.trackfiercely.viewmodel.ProgressGalleryUiState
import com.trackfiercely.viewmodel.ProgressGalleryViewModel
import com.trackfiercely.viewmodel.ProgressPhotoEntry
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressGalleryScreen(
    viewModel: ProgressGalleryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isCompareMode) "Compare" else "Progress Photos",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isCompareMode) {
                            viewModel.exitCompareMode()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!uiState.isCompareMode && uiState.hasPhotos && uiState.photos.size > 1) {
                        TextButton(onClick = { viewModel.enterCompareMode() }) {
                            Text(
                                text = "Compare",
                                color = FallPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (uiState.isCompareMode) {
                        TextButton(onClick = { viewModel.exitCompareMode() }) {
                            Text(
                                text = "Done",
                                color = FallPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                !uiState.hasPhotos -> {
                    EmptyState()
                }
                uiState.isCompareMode -> {
                    CompareMode(
                        uiState = uiState,
                        onPresetSelected = { viewModel.applyComparePreset(it) }
                    )
                }
                else -> {
                    GalleryMode(
                        uiState = uiState,
                        onIndexChanged = { viewModel.setCurrentIndex(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = FallPrimary)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Progress Photos Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Take your first progress photo using the\n\"Progress Photo\" task to start tracking\nyour journey!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryMode(
    uiState: ProgressGalleryUiState,
    onIndexChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { uiState.photos.size }
    )
    
    // Sync pager with view model
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentIndex) {
            onIndexChanged(pagerState.currentPage)
        }
    }
    
    LaunchedEffect(uiState.currentIndex) {
        if (pagerState.currentPage != uiState.currentIndex) {
            pagerState.animateScrollToPage(uiState.currentIndex)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Photo pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val photo = uiState.photos[page]
            PhotoCard(
                photo = photo,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
        
        // Page indicators
        if (uiState.photos.size > 1) {
            PageIndicator(
                pageCount = uiState.photos.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun PhotoCard(
    photo: ProgressPhotoEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceAlt
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Photo
            AsyncImage(
                model = photo.photoUri,
                contentDescription = "Progress photo from ${photo.date}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            // Gradient overlay at bottom for text readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    // Date
                    Text(
                        text = photo.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    // Weight
                    if (photo.weight != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = FallPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Weight: ${String.format("%.1f", photo.weight)} lbs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            if (!photo.isWeightExact) {
                                Text(
                                    text = " (closest)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show limited dots for large collections
        val maxDots = 7
        val showDots = pageCount <= maxDots
        
        if (showDots) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage) FallPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        } else {
            // Show text indicator for large collections
            Text(
                text = "${currentPage + 1} / $pageCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompareMode(
    uiState: ProgressGalleryUiState,
    onPresetSelected: (ComparePreset) -> Unit
) {
    val leftPhoto = uiState.compareLeftPhoto
    val rightPhoto = uiState.compareRightPhoto
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Side-by-side photos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left photo (newer)
            ComparePhotoCard(
                photo = leftPhoto,
                label = "Current",
                modifier = Modifier.weight(1f)
            )
            
            // Right photo (older)
            ComparePhotoCard(
                photo = rightPhoto,
                label = "Before",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Weight change summary
        WeightChangeSummary(
            leftPhoto = leftPhoto,
            rightPhoto = rightPhoto,
            weightChange = uiState.weightChange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Preset buttons
        ComparePresetButtons(
            onPresetSelected = onPresetSelected
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ComparePhotoCard(
    photo: ProgressPhotoEntry?,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceAlt
        )
    ) {
        if (photo != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = photo.photoUri,
                    contentDescription = "$label photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Overlay with info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = photo.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (photo.weight != null) {
                            Text(
                                text = "${String.format("%.1f", photo.weight)} lbs",
                                style = MaterialTheme.typography.bodySmall,
                                color = FallPrimary
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No photo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeightChangeSummary(
    leftPhoto: ProgressPhotoEntry?,
    rightPhoto: ProgressPhotoEntry?,
    weightChange: Float?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (weightChange != null) {
                val isLoss = weightChange < 0
                val changeText = if (isLoss) {
                    "${String.format("%.1f", kotlin.math.abs(weightChange))} lbs lost"
                } else if (weightChange > 0) {
                    "+${String.format("%.1f", weightChange)} lbs"
                } else {
                    "No change"
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLoss) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (isLoss) SageGreen else HealthFitnessColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = changeText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isLoss) SageGreen else HealthFitnessColor
                    )
                }
                
                if (leftPhoto != null && rightPhoto != null) {
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(rightPhoto.date, leftPhoto.date)
                    Text(
                        text = "Over $daysBetween days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Weight data not available for comparison",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ComparePresetButtons(
    onPresetSelected: (ComparePreset) -> Unit
) {
    Column {
        Text(
            text = "Compare to:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip(
                preset = ComparePreset.ONE_WEEK,
                onClick = { onPresetSelected(ComparePreset.ONE_WEEK) },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                preset = ComparePreset.ONE_MONTH,
                onClick = { onPresetSelected(ComparePreset.ONE_MONTH) },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                preset = ComparePreset.THREE_MONTHS,
                onClick = { onPresetSelected(ComparePreset.THREE_MONTHS) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip(
                preset = ComparePreset.SIX_MONTHS,
                onClick = { onPresetSelected(ComparePreset.SIX_MONTHS) },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                preset = ComparePreset.ONE_YEAR,
                onClick = { onPresetSelected(ComparePreset.ONE_YEAR) },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                preset = ComparePreset.FIRST,
                onClick = { onPresetSelected(ComparePreset.FIRST) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PresetChip(
    preset: ComparePreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = FallPrimary.copy(alpha = 0.15f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = preset.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = FallPrimary
            )
        }
    }
}

