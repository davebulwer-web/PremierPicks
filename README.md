# Premier Picks

A modern Android remake of the 1993 Amiga game *Premier Picks*: a turn-based,
card-flipping football game wrapped in a 21-team league season.

## Stack

- **Kotlin** throughout.
- **`engine/`**: the game rules as a pure Kotlin/JVM module with no Android
  dependencies, so they are fast to unit test and reusable from the app.
- **`app/`**: the Android app, built with Jetpack Compose. Pick a team and play a
  season against the computer; the rest of the league's fixtures are simulated.
  The season is saved after every round (a match in progress is not).

## Building

```
./gradlew -p engine test      # game rules only, no Android SDK needed
./gradlew :app:assembleDebug  # the app, needs the Android SDK
```

Or open the project in Android Studio and run the `app` configuration.

Every push to a pull request also builds the app on GitHub: open the PR's
**Checks** tab, choose the *Build* run, and download `premier-picks-debug-apk`
from its Artifacts section to install on a phone.

## Rules

These follow the 1993 original as closely as we can tell from screenshots, reviews and
players' memories.

| Topic | Rule |
| --- | --- |
| Board | 18 face-down cards per half, 6 across and 3 down. One is the half-time/full-time whistle; the other 17 are drawn at random, goals and near misses being commonest, then penalties, free kicks, fouls and substitutions. |
| Card help | Before each half a light races along ONE to SIX; stop it and you see that many cards for 3 seconds, then they turn back over. The computer gets its own random preview and remembers what it saw. Can be switched off. |
| Turns | Sides take turns flipping one card each. Home starts the first half, away the second. |
| Goal | One goal to whoever flips it. |
| Penalty | The taker has 3 seconds to turn over cards looking for a goal. Find one and it counts; cards you look at turn back over. |
| Yellow card | A free kick: pick again. |
| Foul | The lying-down player: your turn ends and the opponent gets two picks. |
| Substitution | Swapped for a random other card (never another substitution or the whistle), which then takes effect. |
| Whistle | Ends the half on the spot, whoever flips it. The computer will blow it when it's ahead. |
| League | 22 clubs, with fictional names you can rename. Choose the number of games (up to 42, home and away) and the points for a win, draw and defeat. Ties broken by goal difference, then goals scored. |
