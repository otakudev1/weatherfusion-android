# WeatherFusion Android

v0.1.0 — keyless, multi-provider weather dashboard + home-screen widgets for Android.
Built entirely by GitHub Actions CI: no local toolchain needed, 100% free toolchain.

## What it does
- **Dashboard**: current temp card, provider spread (best_match vs GFS vs ICON) with a
  HIGH/MEDIUM/LOW confidence badge, 5-day strip, and a severity-tinted NWS alert banner
  (US locations only — silently absent elsewhere).
- **Location search**: Open-Meteo geocoding. The last-searched location is saved to
  SharedPreferences and is the single source of truth for both app and widgets.
- **Widgets**: 4x1 and 4x2 RemoteViews widgets with multi-day strip, condition line,
  confidence badge, and alert strip. Refreshed by a 3h WorkManager periodic job,
  on boot, and whenever the dashboard loads fresh data.

## Keyless APIs (no keys, no auth, no Supabase in v0.1)
- Open-Meteo geocoding + forecast (`models=best_match,gfs_seamless,icon_seamless`)
- api.weather.gov active alerts (User-Agent: `WeatherFusionApp/0.1 (personal)`), US-only

## Build
Push to `main` (or run the workflow manually). CI runs:
`./gradlew assembleDebug testDebugUnitTest` on ubuntu-latest (Temurin 17),
uploads the APK artifact, and publishes a GitHub Release `v0.1.0` with `app-debug.apk`.

## Engineering notes
- Widget layouts use **framework widgets only** (TextView/LinearLayout/ImageView);
  no `Space` or bare `View` spacers — those crash RemoteViews inflation on the host side.
- `GeoJsonMapper` is pure Kotlin (no Android deps) and covered by 6 JUnit tests:
  severity mapping, onset/expires parsing, multiple alerts, empty area,
  malformed-feature tolerance, US-only filter.
- Toolchain: Kotlin 2.0.21, AGP 8.5.2, Gradle 8.9, minSdk 24, target/compileSdk 34,
  core library desugaring for `java.time` on API 24+.
