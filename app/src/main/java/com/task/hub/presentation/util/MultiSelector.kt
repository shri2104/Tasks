package com.task.hub.presentation.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.task.hub.presentation.ui.theme.Blue
import com.task.hub.presentation.ui.theme.Grey
import com.task.hub.presentation.ui.theme.priegoFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val AnimationDurationMillis = 300

@Stable
interface MultiSelectorState {
    val selectedIndex: Float
    val startCornerPercent: Int
    val endCornerPercent: Int
    val textColors: List<Color>

    fun selectOption(scope: CoroutineScope, index: Int)
}

@Stable
class MultiSelectorStateImpl(
    options: List<String>,
    selectedOption: String,
    private val selectedColor: Color,
    private val unselectedColor: Color,
) : MultiSelectorState {

    override val selectedIndex: Float
        get() = _selectedIndex.value
    override val startCornerPercent: Int
        get() = _startCornerPercent.value.toInt()
    override val endCornerPercent: Int
        get() = _endCornerPercent.value.toInt()

    override val textColors: List<Color>
        get() = _textColors.value

    private var _selectedIndex = Animatable(options.indexOf(selectedOption).toFloat())
    private var _startCornerPercent = Animatable(
        if (options.first() == selectedOption) {
            50f
        } else {
            15f
        }
    )
    private var _endCornerPercent = Animatable(
        if (options.last() == selectedOption) {
            50f
        } else {
            15f
        }
    )

    private var _textColors: State<List<Color>> = derivedStateOf {
        List(numOptions) { index ->
            lerp(
                start = unselectedColor,
                stop = selectedColor,
                fraction = 1f - (((selectedIndex - index.toFloat()).absoluteValue).coerceAtMost(1f))
            )
        }
    }

    private val numOptions = options.size
    private val animationSpec = tween<Float>(
        durationMillis = AnimationDurationMillis,
        easing = FastOutSlowInEasing,
    )

    override fun selectOption(scope: CoroutineScope, index: Int) {
        scope.launch {
            _selectedIndex.animateTo(
                targetValue = index.toFloat(),
                animationSpec = animationSpec,
            )
        }
        scope.launch {
            _startCornerPercent.animateTo(
                targetValue = if (index == 0) 50f else 15f,
                animationSpec = animationSpec,
            )
        }
        scope.launch {
            _endCornerPercent.animateTo(
                targetValue = if (index == numOptions - 1) 50f else 15f,
                animationSpec = animationSpec,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiSelectorStateImpl

        if (selectedColor != other.selectedColor) return false
        if (unselectedColor != other.unselectedColor) return false
        if (_selectedIndex != other._selectedIndex) return false
        if (_startCornerPercent != other._startCornerPercent) return false
        if (_endCornerPercent != other._endCornerPercent) return false
        if (numOptions != other.numOptions) return false
        if (animationSpec != other.animationSpec) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedColor.hashCode()
        result = 31 * result + unselectedColor.hashCode()
        result = 31 * result + _selectedIndex.hashCode()
        result = 31 * result + _startCornerPercent.hashCode()
        result = 31 * result + _endCornerPercent.hashCode()
        result = 31 * result + numOptions
        result = 31 * result + animationSpec.hashCode()
        return result
    }
}

@Composable
fun rememberMultiSelectorState(
    options: List<String>,
    selectedOption: String,
    selectedColor: Color,
    unSelectedColor: Color,
) = remember {
    MultiSelectorStateImpl(
        options,
        selectedOption,
        selectedColor,
        unSelectedColor,
    )
}

enum class MultiSelectorOption {
    Option,
    Background,
}

@Composable
fun MultiSelector(
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurface,
    state: MultiSelectorState = rememberMultiSelectorState(
        options = options,
        selectedOption = selectedOption,
        selectedColor = selectedColor,
        unSelectedColor = unselectedColor,
    ),
) {
    require(options.size >= 2) { "This composable requires at least 2 options" }
    require(options.contains(selectedOption)) { "Invalid selected option [$selectedOption]" }
    LaunchedEffect(key1 = options, key2 = selectedOption) {
        state.selectOption(this, options.indexOf(selectedOption))
    }
    Layout(
        modifier = modifier
            .clip(
                shape = RoundedCornerShape(12.dp)
            )
            .background(Grey),
        content = {
            val colors = state.textColors
            options.forEachIndexed { index, option ->
                Box(
                    modifier = Modifier
                        .layoutId(MultiSelectorOption.Option)
                        .noRippleClickable { onOptionSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        fontSize = 15.sp,
                        fontFamily = priegoFont,
                        fontWeight = FontWeight.Medium,
                        color = colors[index],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .layoutId(MultiSelectorOption.Background)
                    .padding(3.5.dp)
                    .clip(shape = RoundedCornerShape(12.dp))
                    .background(Blue)
            )
        }
    ) { measurables, constraints ->
        val optionWidth = constraints.maxWidth / options.size
        val optionConstraints = Constraints.fixed(
            width = optionWidth,
            height = constraints.maxHeight,
        )
        val optionPlaceables = measurables
            .filter { measurable -> measurable.layoutId == MultiSelectorOption.Option }
            .map { measurable -> measurable.measure(optionConstraints) }
        val backgroundPlaceable = measurables
            .first { measurable -> measurable.layoutId == MultiSelectorOption.Background }
            .measure(optionConstraints)
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
        ) {
            backgroundPlaceable.placeRelative(
                x = (state.selectedIndex * optionWidth).toInt(),
                y = 0,
            )
            optionPlaceables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = optionWidth * index,
                    y = 0,
                )
            }
        }
    }
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}