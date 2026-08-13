# Smart Field Calculator

An offline, rugged-styled Android application for **educational trajectory simulation**. Enter the
environmental conditions on a calculator-style keypad, press **SOLVE**, and the app integrates the
equations of motion for a point mass under gravity and aerodynamic drag, then shows the resulting
trajectory as a summary, a table and a graph.

Built with Kotlin, Jetpack Compose (Material 3), Room and DataStore. No internet permission is
declared — everything runs locally.

---

## Scope

This is a **physics/simulation teaching tool**. It reports what the simulated body does:

| Reported | Not reported |
|---|---|
| Height, lateral drift, velocity, Mach, kinetic energy, time of flight | Sight or aiming corrections |
| Air density, speed of sound, pressure | Turret clicks, holds, dial values |
| Drag coefficient behaviour vs Mach | Any engagement instruction |

The simulation engine is a generic point-mass integrator — the same model used to teach projectile
motion with air resistance. Profiles hold generic body parameters (mass, calibre, length, launch
velocity, drag model) and ship with placeholder demonstration data only.

---

## Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17 (bundled with recent Android Studio)
- Android SDK Platform 35; the app runs on API 26+ (Android 8.0)
- Gradle 8.9 — the wrapper is committed, so nothing to install

## Build and run

```bash
git clone https://github.com/WaseemMirzaa/shair-jang.git
cd shair-jang

# Point the build at your SDK (Android Studio writes this for you on first open)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew assembleDebug          # debug APK
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented UI tests (device/emulator required)
```

In Android Studio: **File → Open**, select the repository root, let Gradle sync, pick a device on
API 26+ and press **Run**.

### Exporting an APK

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release-unsigned.apk
./gradlew bundleRelease     # app/build/outputs/bundle/release/app-release.aab
```

The release build is minified and resource-shrunk. To ship a signed APK, add a `signingConfigs`
block to `app/build.gradle.kts` (or use **Build → Generate Signed Bundle / APK** in the IDE).

On first launch the app seeds three locked demo profiles, so it is demonstrable immediately with no
setup and no network.

---

## Using it

```
START → SELECT PROFILE → MAIN CALCULATOR → enter values → SOLVE → RESULT
                                                                   ├── TRAJECTORY TABLE
                                                                   └── GRAPH
```

The operator screen only accepts the seven changing variables:

| Field | Example | Notes |
|---|---|---|
| RANGE | 650 m | required, 0–5000 m |
| WIND SPEED | 8.0 m/s | 0–60 m/s |
| WIND DIRECTION | 3 o'clock | clock face, 1–12; names where the wind blows **from** |
| TEMPERATURE | 30 °C | −60…60 °C |
| ALTITUDE | 4500 ft | pressure is derived from it, never asked for |
| HUMIDITY | 50 % | 0–100 % |
| INCLINATION | +8° | optional, defaults to 0° |

Tap a row to select it, type with the keypad, `ENTER`/`DOWN` to move on, `CLEAR` to wipe the entry,
`BACK` to delete a digit. Everything else — profile management, drag models, integrator settings —
lives behind the menu and the admin PIN. *Complexity inside, simplicity outside.*

**Admin mode**: MENU → ADMIN, default PIN `0000` (changeable in Settings). Operators can only
*select* profiles; creating, editing and deleting them requires the PIN.

---

## Architecture

```
app/src/main/java/com/codetivelab/fieldcalc/
├── domain/                     ← pure Kotlin, zero Android imports
│   ├── models/                 Units, OperatorInput, Profile, Trajectory, AtmosphereState
│   ├── units/                  UnitConverter — SI ⇄ metric/imperial at the display edge
│   ├── validation/             InputValidator — returns codes, not sentences
│   ├── input/                  InputEditor — keypad entry rules (buffer, navigation, commit)
│   ├── atmosphere/             AtmosphereModel — ISA pressure, moist-air density, speed of sound
│   └── engine/                 SimulationEngine, TrajectorySimulationEngine, Integrator,
│                               DragModel, Vec3
├── data/
│   ├── database/               Room entity, DAO, database, type converters
│   ├── repository/             ProfileRepository (entity ⇄ domain, demo seeding)
│   └── prefs/                  SettingsStore (DataStore: units, theme, last profile, last inputs)
├── ui/
│   ├── theme/                  Rugged olive/LCD Compose theme (3 variants)
│   ├── components/             RuggedButton, LcdPanel, Keypad
│   ├── calculator/             CalculatorViewModel + CalculatorScreen
│   ├── results/                ResultsScreen (summary/table/graph) + TrajectoryGraph
│   ├── profile/                Operator profile picker (select-only)
│   ├── admin/                  PIN gate + profile editor
│   ├── settings/               Units, theme, brightness, timeout, sound, vibration, language, PIN
│   └── i18n/                   Messages.kt — domain codes → string resources
├── navigation/                 NavGraph
├── ServiceLocator.kt           Manual DI — the one place an engine is named
└── MainActivity.kt
```

**The `domain` package has no Android dependencies at all.** That is the portability contract: the
same source compiles and runs on a plain JVM today (see *Testing* below) and can be ported to ESP32
/ Raspberry Pi / dedicated hardware, with `SimulationEngine` as the stable API:

```
Android UI  →  SimulationEngine API  →  physics modules
ESP32 UI    →  SimulationEngine API  →  the same physics modules
```

---

## The calculation engine

```
OperatorInput + Profile
        ↓
   unit conversion (already SI internally)
        ↓
   AtmosphereModel      → density, speed of sound, pressure
        ↓
   DragModel            → Cd(Mach)
        ↓
   Integrator (RK4)     → position/velocity, step by step
        ↓
   sampling             → Trajectory(points)
        ↓
   derived values       → Mach, kinetic energy
```

**Atmosphere** (`AtmosphereModel.calculateAtmosphere(altitude, temperature, humidity)`) uses the ISA
barometric formula for pressure, the Tetens formula for water-vapour partial pressure, ideal-gas
moist-air density, and Newton–Laplace for the speed of sound.

**Forces** — gravity, plus drag opposing the velocity *relative to the moving air*:

```
a = −½·ρ·Cd(M)·A·|v−w|·(v−w) / m  +  g
```

where `w` is the wind vector taken from the clock direction, `A` the frontal area from the calibre,
and `Cd(M)` a piecewise-linear drag curve. Three drag models ship: `GENERIC` (streamlined body — low
subsonic drag, transonic rise, supersonic decay), `SPHERE` (classic wind-tunnel curve) and
`CONSTANT` (a fixed Cd; `cd = 0` gives a drag-free vacuum, which is how the tests check the
integrator against the analytic parabola).

**Integration** is fixed-step RK4 by default; `EULER` and `HEUN` are selectable through
`EngineConfig`, along with the step size, the flight-time cap and the number of sampled points:

```kotlin
TrajectorySimulationEngine(
    EngineConfig(timeStepS = 0.001, sampleCount = 120, method = IntegrationMethod.RK4)
)
```

Solving happens on `Dispatchers.Default`; the screen shows `CALCULATING…` and then
`CALCULATION COMPLETE`. A typical solve takes single-digit milliseconds.

Sample output for the demo profile at 650 m, 8 m/s wind at 3 o'clock, 30 °C, 4500 ft, 50 %, +8°:

```
STATUS OK
ATMOSPHERE  density 0.9779 kg/m3   sound 350.7 m/s   pressure 859.0 hPa

RANGE  HEIGHT   DRIFT   VEL     MACH   ENERGY   TIME
  100   13.97   -0.05   733.1   2.09     2687  0.132
  200   27.76   -0.19   669.7   1.91     2242  0.276
  300   41.32   -0.44   609.4   1.74     1857  0.434
  400   54.61   -0.82   552.0   1.57     1523  0.608
  500   67.57   -1.35   497.3   1.42     1236  0.800
  600   80.13   -2.06   445.0   1.27      990  1.015
  650   86.22   -2.48   420.0   1.20      882  1.131
```

(Height rises because the body is launched along the +8° line of departure; drift is negative
because a 3 o'clock wind blows from right to left.)

---

## Data model

### Room database — `field_calc.db`, table `profiles`

| Column | Type | Meaning |
|---|---|---|
| `id` | INTEGER PK | autogenerated |
| `name` | TEXT | display name |
| `isDemo` / `isLocked` | INTEGER | DEMO badge / operator-editable flag |
| `unitSystem` | TEXT | `METRIC` or `IMPERIAL` |
| `proj_*` | embedded | calibre (mm), mass (kg), length (m), launch velocity (m/s), drag model id, aero params |
| `geo_*` | embedded | reference height (m), reference distance (m), simulator config |
| `env_*` | embedded | default temperature (°C), humidity (%), altitude (m) |

`aeroParams` is a `Map<String, Double>` serialised as `key=value;key=value`; recognised keys are
`formFactor` (scales the drag curve) and `cd` (the CONSTANT model's coefficient).

### Settings — DataStore (`settings`)

Unit system, theme, brightness, screen timeout, sound, vibration, language, admin PIN, last selected
profile, and the last operator inputs (stored in SI so they survive a unit-system change).

### Adding a profile

*In the app*: MENU → ADMIN → PIN `0000` → NEW PROFILE, fill in the fields, pick a drag model, SAVE.

*In code*: add it to `ProfileRepository.seedProfiles()` and it will be created on a fresh install.

---

## Testing

```bash
./gradlew test                   # 102 JVM unit tests
./gradlew connectedAndroidTest   # instrumented Compose UI tests
```

Unit tests (`app/src/test/`) cover the unit converter (round trips, every quantity, formatting),
input validation, the atmosphere model, the drag models, the integrators, the trajectory model
(interpolation, table sampling), the keypad entry rules (`InputEditor`: digit/dot/sign/backspace
behaviour, field navigation, commit-with-unit-conversion, display formatting), the Room type
converters, and the engine itself — including a check that the RK4 solution of a
drag-free launch matches the closed-form parabola to under a millimetre, that total energy is
conserved in vacuum, that RK4 beats Euler at the same step, and that wind, altitude and inclination
push the results in the physically correct direction. `ProfileRepositoryTest` exercises seeding,
save/update/delete against an in-memory fake DAO.

UI tests (`app/src/androidTest/`) drive the real app: keypad digit entry, CLEAR, BACK, ENTER field
advance, arrow keys, SOLVE → results → table → graph → back, invalid-input messaging, profile
selection and metric/imperial switching.

Because the `domain` package is Android-free, it can be compiled and run with a plain Kotlin
compiler and JUnit — no SDK, no emulator. The keypad rules live in `domain/input/InputEditor.kt`
rather than in the ViewModel for exactly this reason: what the operator types is behaviour worth
testing on every host, not just on Android.

---

## Offline operation

No `INTERNET` permission is declared in the manifest. There is no account, no cloud service and no
external API. Profiles live in Room, settings in DataStore, and the simulation runs on-device.
