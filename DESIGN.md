# MYWORLD — Autonomous Alternate-Earth Simulation

## Core promise
- **One real day is one simulated month**. Month 0 begins in alternate-history 2040.01.
- **24 independent national agents** choose policy, research, trade, diplomatic talks, and respond to citizens autonomously.
- **Two widgets**: 1×1 compact date/peace/economy, 2×2 date/economy/peace/climate/population/monthly report.
- Both widget sizes share the same persistent state and open the same app.
- Player can inspect any country, change eight domestic policy areas, and influence five diplomatic actions.
- Offline simulation, no API, account, network, tracking, or location permission.

## Variables and units
| Variable | Internal model |
|---|---|
| Economy | 0–100 index; derived from industry, education, health, transport, energy, food, technology, bilateral trade, fiscal balance |
| Treasury | Fictional -100 to 350 budget units, monthly income less welfare/health/education/defense administration costs |
| Population | Fictional millions tracked for 24 countries; welfare and supply determine slow monthly changes |
| Science | 0–100 science capacity feeding research |
| Technology | Five branches: energy, medicine, automation, environment, space, each level 0–10 plus accumulated research score |
| Society | Five distinct satisfaction groups: civilians, industry, researchers, farmers, workers |
| Stability | Government stability and civic trust; affected by satisfaction, shortages and policy |
| Food and energy | Scarcity influences productivity, health, and domestic stability |
| Bilateral relations | -100 to +100 trust, 0–100 trade, 0–100 tension, trade pact flag, conflict flag |
| Environment | National environment quality, worldwide carbon pressure, climate resilience |
| Player influence | Starts at 16, recovers 4 per simulated month, capped at 24; maximum 3 actions per month |

## The autonomous month
1. Advance simulated time, regenerate intervention points.
2. Each country computes revenue, spending, productivity and trade contribution.
3. Each government selects among eight policy priorities based on needs, ideology, citizen sentiment and a deterministic PRNG.
4. Policy investments require a fiscal budget and adjust economy, research, welfare, health, environment, food, industry or administration.
5. Research teams accumulate progress in five specializations and reach breakthrough milestones.
6. Population changes with welfare, health and food availability; discontent causes lower stability and may change policy priorities.
7. For each pair of states, relationships, trade pacts, international tensions, and macroeconomic effects of conflict are updated.
8. Independent diplomatic agent interactions begin exchanges, treaty proposals or negotiation, including mediated ceasefires.
9. Occasional societal and environmental events unfold and change future probabilities.
10. Worldwide peace and climate metrics are recalculated; a time-stamped monthly news report is saved.
11. Every 12 simulated months the game adds an annual world report.

The PRNG seed and state are saved so the simulation is replayable from the same state and decisions. Autonomous policy trajectories are linked to budgets, citizens' wellbeing, resources, trade and the environment rather than being merely random stories.

## Player interactions
- Eight policies: education, science, public health, energy transition, agriculture, transportation, welfare, governance.
- Five diplomatic actions: trade deal, dialogue, exchanges, joint climate study, peace mediation.
- Actions spend player influence; domestic interventions also spend local treasury.
- One player can intervene in any represented country, not just one permanent faction.
- Maximum 3 interventions per game month; no limits on observing world events.

## Time and background operation
A periodic JobScheduler task requests roughly 15-minute checks; Android may delay or stop background work during battery saving. The world therefore uses a durable origin timestamp. When either widget or the main application is opened, every elapsed simulated month is processed in order and stored. The world progresses correctly **by elapsed time**, even if the operating system suppresses background jobs. Widget contents may be stale when Android delays widget updates, but opening the app catches up. Clock tampering and very long time gaps can change this behavior, and a safety cap prevents excessive compute.

## Widgets
- 1×1: miniature Earth, 2040.01, world peace and economic index.
- 2×2: prominent Earth, complete year and month, peace, economy, environmental resilience, tracked-country population, recent monthly headline.
- Both can be placed separately, tap to open the app, and share the same local save.

## Caveats
Fictional game values only: the world does not predict genuine geopolitical developments. The map is stylized with simplified continent shapes and country markers; it is not a claim about national borders. The conflict system only simulates overall human/social/economic impact and the process of diplomacy; no real-world tactics are modelled. For deep accuracy, later versions should add richer region-level economies, logistics, elections, demographic cohorts and agent rationales with regression tests.

## Android build
Java 17, Gradle 8.9, Android 35. Compile using:
    gradle :app:assembleDebug

GitHub Actions builds and publishes the personal debug APK to downloads/myworld-debug.apk. The debug APK is not a Play Store-signed release.
