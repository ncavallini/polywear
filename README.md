# ETH Schedule — Wear OS

A Wear OS app that shows your ETH (`eduapp.ethz.ch`) schedule as a day-grouped
list, plus a phone companion app that handles login.

## Why two apps

`GET https://eduapp.ethz.ch/web/gw/schedule?semkez=<sem>` requires
authentication (returns `401` otherwise), and ETH's login is not an openly
registerable OAuth2 server — so a watch-only OAuth flow (`RemoteAuthClient`)
can't register the required redirect URI. Instead:

- **`:mobile`** runs the real eduapp web login in a WebView, captures the
  resulting credential (bearer token or session cookie), and sends it to the
  watch over the **Wearable Data Layer**.
- **`:wear`** stores the credential (encrypted), calls the schedule API
  directly, filters to the current week, and renders a day-grouped list.

Both modules share the same `applicationId` (`ch.ncavallini.eduappwear`) so the
Data Layer associates them automatically. They must be **signed with the same
key**.

## Phase 0 — confirmed ✅

A live response was captured and the code is now aligned to the real API:

- **Auth is a session cookie** (`credentials: include`, no `Authorization`
  header). The phone captures it via `CookieManager` and the watch sends it as a
  `Cookie` header (`AuthInterceptor`).
- The endpoint returns a **top-level JSON array** of lessons. Each lesson has
  `id`, `type` (`V`=Lecture, `U`=Exercise, `P`=Lab, `S`=Seminar), `code`,
  `semkez`, `start`/`end` (**UTC**, `...Z`), `locations` (string array; may
  contain `"n / a"`), and `title` (language map, e.g. `{"de": "..."}`). Modelled
  in `LessonDto` and mapped by `ScheduleMapper` (UTC → Europe/Zurich).

Remaining minor tunables you may want to verify against your own account:

- The exact cookie name (currently the whole cookie string for the eduapp origin
  is sent). If eduapp sets the session on a different host, adjust
  `CredentialCapturer.EDUAPP_ORIGIN`.
- `SemesterUtil.semkezFor` boundary dates (mid-Sep / mid-Feb) — adjust if a
  semester edge lands differently.

## Build

The Gradle wrapper **binaries** (`gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar`) are not included (they are binary). Either:

- Open the project in **Android Studio** (it will generate the wrapper), or
- Run once with a local Gradle 8.11+: `gradle wrapper`, then use `./gradlew`.

Then:

```bash
./gradlew :wear:assembleDebug :mobile:assembleDebug
./gradlew :wear:testDebugUnitTest        # unit tests (SemesterUtil, parsing/grouping)
```

## Run / verify

1. In Android Studio **Device Manager**, create a **Wear OS (API 30+)** emulator
   and a **phone** emulator, then **pair** them (Wear emulator ⋮ → Pair with
   phone). Pairing is required for the Data Layer.
2. Install `:mobile` on the phone and `:wear` on the watch.
3. On the phone, open **ETH Schedule Login**, sign in to eduapp, then tap
   **Send credential to watch**. The status line reports how many watches
   received it.
4. On the watch, open **ETH Schedule**. It should load the current week grouped
   by day, with today highlighted. Airplane-mode the watch to see the cached
   (offline) view; a `401` shows the "open the phone app to sign in" prompt.

## Project layout

```
:mobile   LoginActivity (WebView) · CredentialCapturer · CredentialSender
:wear     ui/ (Compose Wear, ScheduleScreen + ViewModel)
          data/ (ScheduleApi, ScheduleRepository, ScheduleMapper, cache, DTOs)
          auth/ (CredentialListenerService, TokenStore, AuthInterceptor)
          util/SemesterUtil
```

## Known limitations / risks

- Credential capture is inherently brittle: eduapp login/UI changes can break
  it. Phase 0 pins current behavior.
- No silent refresh: when the credential expires, you must re-auth on the phone
  (unless Phase 0 reveals a refresh token to plumb through).
- Automating login / reusing a session cookie may bump against ETH's terms —
  this is for personal use only.
