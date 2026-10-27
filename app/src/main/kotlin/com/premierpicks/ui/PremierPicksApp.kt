package com.premierpicks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Top-level navigation: pick a team, then move between the season hub and matches. */
@Composable
fun PremierPicksApp(
    seasonVm: SeasonViewModel = viewModel(),
    matchVm: MatchViewModel = viewModel(),
) {
    val ui by seasonVm.ui.collectAsStateWithLifecycle()
    val season = ui.season
    val playing = ui.playing

    when {
        season == null -> TeamPickerScreen(onPick = seasonVm::newSeason)
        playing != null -> {
            // A match can't be abandoned half way: back does nothing.
            BackHandler {}
            MatchScreen(matchVm, onFinished = seasonVm::finishMatch)
        }
        else -> SeasonScreen(
            season = season,
            version = ui.version,
            onPlay = {
                seasonVm.playRound()?.let { fixture ->
                    matchVm.start(season.playerSide(fixture), fixture.home, fixture.away)
                }
            },
            onNewSeason = seasonVm::abandonSeason,
        )
    }
}
