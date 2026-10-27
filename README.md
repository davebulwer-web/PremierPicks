# Premier Picks

A modern Android remake of the 1993 Amiga game *Premier Picks*: a turn-based,
card-flipping football game wrapped in a 21-team league season.

## Stack

- **Kotlin** throughout.
- **`engine/`**: the game rules as a pure Kotlin/JVM module with no Android
  dependencies, so they are fast to unit test and reusable from the app.
- **`app/`**: the Android app, built with Jetpack Compose. Pick a team and play a
  42-round season against the computer; the rest of the league's fixtures are simulated.
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

## Rules as implemented

Where the functional spec left room for interpretation, the engine makes these calls:

| Topic | Decision |
| --- | --- |
| Board | 18 cards per half, laid out 6 across and 3 down like the original: 2–3 goals, 1 penalty, 1 hint, 1 half-time/full-time whistle, 2 substitutes, 2 injuries, 2 discipline cards (each yellow, or red with 25% chance), and saves fill the rest (6–7). |
| Kick-off | Home side chooses first in the first half, away side in the second. |
| Half ends | When the whistle card is flipped, even with cards still face down. |
| Injury (foul) | The injury card itself uses a pick. Unspent picks are added to the opponent's next turn on top of the budget they choose. Bonus picks are dropped at half time. |
| Substitute | Swapped for a random other card (never another substitute or the whistle), which takes effect with the same pick. |
| Penalty | Scores 80% of the time. |
| Hint | Highlights every unrevealed goal for the rest of the half (the UI can choose to flash it for 3 seconds instead). |
| Discipline | Two yellows, or one red, is a sending-off. Each sending-off lowers that side's maximum budget by 1 for the rest of the match, never below 1. |
| AI | Level or ahead: budget 1–2. Behind in the second half with half the board or less left: 4–6. Otherwise behind: 2–4. It takes a hinted goal if one is showing, otherwise picks at random. |
| League | 21 teams cannot all play every round, so one team rests each round: 42 rounds, 40 games per team (20 home, 20 away). 3 points for a win; ties broken by goal difference, then goals scored. |
| Teams | Fictional club names, to keep the released game clear of real club trademarks. |
