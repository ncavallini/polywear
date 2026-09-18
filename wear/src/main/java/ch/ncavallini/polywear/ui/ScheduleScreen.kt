package ch.ncavallini.polywear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import ch.ncavallini.polywear.data.model.DaySchedule
import ch.ncavallini.polywear.data.model.ScheduleEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onRetry: () -> Unit,
    onEventClick: (String) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        when (state) {
            is ScheduleUiState.Loading -> CenteredProgress()
            is ScheduleUiState.NeedsAuth -> CenteredMessage("Open the phone app to sign in to eduapp")
            is ScheduleUiState.Error -> ErrorContent(state.message, onRetry)
            is ScheduleUiState.Content ->
                ScheduleList(
                    state.days,
                    state.stale,
                    state.weekLabel,
                    listState,
                    onEventClick,
                    onRetry,
                    onPreviousWeek,
                    onNextWeek,
                )
        }
    }
}

@Composable
private fun ScheduleList(
    days: List<DaySchedule>,
    stale: Boolean,
    weekLabel: String,
    listState: ScalingLazyListState,
    onEventClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val today = LocalDate.now(ZoneId.of("Europe/Zurich"))
    val now = remember { LocalDateTime.now(ZoneId.of("Europe/Zurich")) }
    // Focus on the next upcoming (or ongoing) class rather than the top of the list.
    // Re-focus whenever the shown week changes.
    val focusIndex = remember(days) { nextEventItemIndex(days, now) }
    LaunchedEffect(weekLabel) { listState.scrollToItem(focusIndex) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                text = weekLabel,
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
        item { WeekNav(onPreviousWeek, onNextWeek) }
        if (days.isEmpty()) {
            item {
                Text(
                    text = "No classes this week",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        days.forEach { day ->
            item(key = "day-${day.date}") {
                DayHeader(day.date, isToday = day.date == today)
            }
            items(day.events, key = { it.id }) { event ->
                EventCard(event, onEventClick)
            }
        }
        item {
            CompactChip(
                onClick = onRefresh,
                label = { Text("Refresh") },
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun WeekNav(onPreviousWeek: () -> Unit, onNextWeek: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompactChip(
            onClick = onPreviousWeek,
            label = {
                Text("‹ Prev", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            modifier = Modifier.weight(1f),
        )
        CompactChip(
            onClick = onNextWeek,
            label = {
                Text("Next ›", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Flattened [ScalingLazyColumn] item index of the next upcoming (or currently
 * ongoing) class, so the list opens focused on "what's next" rather than the top.
 * Items 0 and 1 are the week caption and the week-navigation row; each day then
 * contributes one header item plus one item per event. Falls back to the last
 * event, then to the caption.
 */
private fun nextEventItemIndex(days: List<DaySchedule>, now: LocalDateTime): Int {
    val headerItems = 2 // week caption + week-navigation row
    val upcoming = days.flatMap { it.events }
        .firstOrNull { (it.end ?: it.start) >= now }
        ?: days.flatMap { it.events }.lastOrNull()
        ?: return headerItems

    var index = headerItems
    for (day in days) {
        index++ // day header
        for (event in day.events) {
            if (event.id == upcoming.id) return index
            index++
        }
    }
    return headerItems
}

@Composable
private fun DayHeader(date: LocalDate, isToday: Boolean) {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = weekday,
            style = MaterialTheme.typography.title1,
            color = if (isToday) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground,
        )
        Text(
            text = "${date.dayOfMonth} $month" + if (isToday) " · Today" else "",
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun EventCard(event: ScheduleEvent, onEventClick: (String) -> Unit) {
    val color = CourseColors.forCode(event.code)
    val time = buildString {
        append(event.start.format(timeFormat))
        event.end?.let { append(" – "); append(it.format(timeFormat)) }
    }
    val place = listOfNotNull(event.location, event.category).joinToString(" · ")
    Card(
        onClick = { onEventClick(event.id) },
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = color,
            endBackgroundColor = color,
        ),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.title2,
                color = Color.White,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.title3,
                color = Color.White,
            )
            if (place.isNotEmpty()) {
                Text(
                    text = place,
                    style = MaterialTheme.typography.body1,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun CenteredProgress() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.body2)
        Button(onClick = onRetry) { Text("Retry") }
    }
}
