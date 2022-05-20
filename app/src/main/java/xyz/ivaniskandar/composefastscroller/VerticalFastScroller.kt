package xyz.ivaniskandar.composefastscroller

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun VerticalFastScroller(
    listState: LazyListState,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    endContentPadding: Dp = Dp.Hairline,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val reverseLayout = false // listState.layoutInfo.reverseLayout // incomplete
    val thumbTopPadding = listState.layoutInfo.beforeContentPadding.toFloat()
    var thumbOffsetY by remember { mutableStateOf(thumbTopPadding) }

    val dragInteractionSource = remember { MutableInteractionSource() }
    val isThumbDragged by dragInteractionSource.collectIsDraggedAsState()
    val scrolled = remember {
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }

    BoxWithConstraints {
        content()

        val heightPx = constraints.maxHeight.toFloat() - thumbTopPadding - listState.layoutInfo.afterContentPadding
        val thumbHeightPx = with(LocalDensity.current) { ThumbLength.toPx() }
        val trackHeightPx = heightPx - thumbHeightPx
        val layoutInfo = listState.layoutInfo

        // When thumb dragged
        LaunchedEffect(thumbOffsetY) {
            if (layoutInfo.totalItemsCount == 0 || !isThumbDragged) return@LaunchedEffect
            val scrollRatio = (thumbOffsetY - thumbTopPadding) / trackHeightPx
            val scrollItem = layoutInfo.totalItemsCount * scrollRatio
            val scrollItemRounded = scrollItem.roundToInt()
            val scrollItemSize = layoutInfo.visibleItemsInfo.find { it.index == scrollItemRounded }?.size ?: 0
            val scrollItemOffset = scrollItemSize * (scrollItem - scrollItemRounded)
            listState.scrollToItem(index = scrollItemRounded, scrollOffset = scrollItemOffset.roundToInt())
            scrolled.tryEmit(Unit)
        }

        // When list scrolled
        LaunchedEffect(listState.firstVisibleItemScrollOffset) {
            if (listState.layoutInfo.totalItemsCount == 0 || isThumbDragged) return@LaunchedEffect
            val scrollOffset = computeScrollOffset(state = listState, reverseLayout = reverseLayout)
            val scrollRange = computeScrollRange(state = listState)
            val proportion = scrollOffset.toFloat() / (scrollRange.toFloat() - heightPx)
            thumbOffsetY = if (reverseLayout) {
                trackHeightPx - (trackHeightPx * proportion + thumbTopPadding)
            } else {
                trackHeightPx * proportion + thumbTopPadding
            }
            thumbOffsetY = trackHeightPx * proportion + thumbTopPadding
            scrolled.tryEmit(Unit)
        }

        // Thumb alpha
        val alpha = remember { Animatable(0f) }
        val isThumbVisible = alpha.value > 0f
        LaunchedEffect(scrolled, alpha) {
            scrolled.collectLatest {
                alpha.snapTo(1f)
                alpha.animateTo(0f, animationSpec = FadeOutAnimationSpec)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, thumbOffsetY.roundToInt()) }
                .height(ThumbLength)
                .then(
                    // Exclude thumb from gesture area only when needed
                    if (isThumbVisible && !isThumbDragged && !listState.isScrollInProgress) {
                        Modifier.systemGestureExclusion()
                    } else Modifier
                )
                .padding(horizontal = 8.dp)
                .padding(end = endContentPadding)
                .width(ThumbThickness)
                .alpha(alpha.value)
                .background(color = thumbColor, shape = ThumbShape)
                .then(
                    // Recompose opts
                    if (!listState.isScrollInProgress) {
                        Modifier.draggable(
                            interactionSource = dragInteractionSource,
                            orientation = Orientation.Vertical,
                            enabled = isThumbVisible,
                            state = rememberDraggableState { delta ->
                                val newOffsetY = thumbOffsetY + delta
                                thumbOffsetY = newOffsetY.coerceIn(thumbTopPadding, thumbTopPadding + trackHeightPx)
                            },
                            reverseDirection = reverseLayout
                        )
                    } else Modifier
                )
        )
    }
}

private fun computeScrollOffset(state: LazyListState, reverseLayout: Boolean): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val totalItems = state.layoutInfo.totalItemsCount
    val visibleItems = state.layoutInfo.visibleItemsInfo
    val startChild = visibleItems.first()
    val endChild = visibleItems.last()
    val minPosition = min(startChild.index, endChild.index)
    val maxPosition = max(startChild.index, endChild.index)
    val itemsBefore = if (reverseLayout) {
        (totalItems - maxPosition - 1).coerceAtLeast(0)
    } else {
        minPosition.coerceAtLeast(0)
    }
    val startDecoratedTop = startChild.top
    val laidOutArea = abs(endChild.bottom - startDecoratedTop)
    val itemRange = abs(minPosition - maxPosition) + 1
    val avgSizePerRow = laidOutArea.toFloat() / itemRange
    return (itemsBefore * avgSizePerRow + (0 - startDecoratedTop)).roundToInt()
}

private fun computeScrollRange(state: LazyListState): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val visibleItems = state.layoutInfo.visibleItemsInfo
    val startChild = visibleItems.first()
    val endChild = visibleItems.last()
    val laidOutArea = endChild.bottom - startChild.top
    val laidOutRange = abs(startChild.index - endChild.index) + 1
    return (laidOutArea.toFloat() / laidOutRange * state.layoutInfo.totalItemsCount).roundToInt()
}

private val ThumbLength = 48.dp
private val ThumbThickness = 8.dp
private val ThumbShape = RoundedCornerShape(ThumbThickness / 2)
private val FadeOutAnimationSpec = tween<Float>(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
    delayMillis = 2000,
)

private val LazyListItemInfo.top: Int
    get() = offset

private val LazyListItemInfo.bottom: Int
    get() = offset + size
