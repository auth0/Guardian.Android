# GuardianSDK sample app

A minimal Guardian authenticator built against the local `:guardian` module. It enrolls the device
by scanning a Guardian QR code, receives push notifications through FCM, and lets you allow or reject
login requests. It is also the host app for the SDK's integration tests.

The sample is compiled on every PR to `master` and in the merge queue (the `sample` job in
`pr.yml`), so it can't silently break. It is never
published.

## Prerequisites

- JDK 17 or newer (see `.java-version`)
- Android SDK platforms 36 (the app) and 33 (the `:guardian` module). Gradle downloads them if you
  accept the licences, otherwise install them with the SDK Manager. Point Gradle at the SDK with
  `ANDROID_HOME` or a `local.properties` file (`sdk.dir=…`).
- A device or emulator running Android 7.0 (API 24) or newer **with Google Play services**. On an
  emulator pick a "Google APIs" or "Google Play" system image, because FCM doesn't work without it.

## Configuration

### Guardian tenant

The tenant URL is compiled into `BuildConfig.TENANT_URL`. It defaults to
`https://guardian-demo.us.auth0.com`. Override it with any of these, listed by precedence:

1. A Gradle property on the command line:
   `./gradlew :app:installDebug -PguardianTenantUrl=https://<your-tenant-domain>`
2. The same property in `~/.gradle/gradle.properties`, so it stays out of the repo:
   `guardianTenantUrl=https://<your-tenant-domain>`
3. The `GUARDIAN_TENANT_URL` environment variable, which is handy in CI. Android Studio started from
   the macOS Dock doesn't inherit shell variables; use the Gradle property there.

Any other way of setting the Gradle project property (for example the
`ORG_GRADLE_PROJECT_guardianTenantUrl` environment variable) also works and beats
`GUARDIAN_TENANT_URL`, which is only the fallback.

The value must be an `https://` URL. Because it is compiled in, **rebuild and reinstall after
changing it**. It must be the domain the enrollment link is issued from (your tenant's canonical or
custom domain), otherwise enrollment fails.

The tenant must have the **Push Notification using Auth0 Guardian** MFA factor enabled, with push
credentials for the Firebase project described below. In the Auth0 Dashboard open
**Security → Multi-factor Auth → Push Notification using Auth0 Guardian** and, under **Custom App**,
configure the Android push provider: either direct FCM, which needs a service-account key JSON
from the Firebase project (**Project settings → Service accounts**), or Amazon SNS. Labels may differ
slightly; see Auth0's "Configure Push Notifications for MFA" documentation.

### Firebase (`google-services.json`)

The committed `app/google-services.json` is a **placeholder with dummy data**. It stays in the repo
on purpose, so a clean clone and CI can build the app. Never commit a real config over it.

With the placeholder the app builds, installs and launches, but it can't obtain an FCM token. It
typically shows "Could not obtain FCM token" and closes when you dismiss the dialog, so the
**Enroll** button on the main screen is never reachable. Enrollment without a Firebase project is
only possible through the test hook below.

To receive real push notifications:

1. In the [Firebase console](https://console.firebase.google.com), create a test project, or use an
   existing one, and add an Android app with package name **`com.auth0.guardian.sample`** exactly
   (the build fails if the file has no client for that package).
2. Download its `google-services.json` and save it as **`app/src/debug/google-services.json`**.
   The Google Services plugin prefers this file over the placeholder for debug builds, and the path is
   git-ignored so the real config is never committed. Release builds still use the placeholder.
3. Give the tenant this project's FCM credentials (see above).

## Build and run

```sh
./gradlew :app:assembleDebug                      # build only
./gradlew :app:installDebug -PguardianTenantUrl=https://<your-tenant-domain>
```

or open the project in Android Studio and run the `app` configuration.

### Manual test flow

1. **Set up a tenant user that must enroll in push MFA.** Create a test user, then in
   **Security → Multi-factor Auth** enable Push and set the policy to require MFA ("Always").
2. Launch the app. On Android 13 and newer, grant the notification permission. The FCM token is shown
   while no account is enrolled.
3. Log in as the test user (for example with the database connection's **Try** button in the
   Dashboard). Universal Login shows an enrollment QR code. Alternatively create an enrollment
   ticket (`POST /api/v2/guardian/enrollments/ticket`, or "Send enrollment invitation" on the user's
   page) and open the ticket URL.
4. In the app tap **Enroll**, grant the camera permission, and scan the QR code. If scanning isn't
   practical, tap **Enter enrollment link**, paste the content of the QR code (an `otpauth://…`
   link) and tap **Enroll** in the dialog. The button is also on the camera permission screen, so
   you can enroll after denying the camera. On a device without a camera the dialog opens
   automatically; emulators usually have a virtual camera, so tap the button there. Pasted links
   take the same path as scanned codes.
5. Log in again as the same user to trigger an MFA push.
   - With the app open (visible), the authentication request opens directly.
   - With the app in the background or closed, a notification is shown: tap it. This needs the
     notification permission; without it background pushes are dropped.
6. Tap **Allow** or **Deny**, then check the login result in the browser.
7. Tap **Unenroll** to delete the enrollment.

## Integration test hooks

- **Tenant URL**: also passed to instrumentation as the runner argument `tenantUrl`
  (`InstrumentationRegistry.getArguments().getString("tenantUrl")`), and available as
  `BuildConfig.TENANT_URL`. The module has no `androidTest` setup yet; the integration tests will
  need to add `testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"` and the
  `androidx.test` dependencies. Gradle only injects the argument for `connectedAndroidTest`; with a
  raw `adb shell am instrument` pass `-e tenantUrl <url>`.
- **Skip QR scanning**: start `EnrollActivity` with the extra
  `com.auth0.guardian.sample.EnrollActivity.ENROLLMENT_DATA` set to the enrollment URI, which is the
  content of the QR code. Enrollment then starts immediately without the camera, the successful
  enrollment is saved (so the next `MainActivity` launch is enrolled and handles pushes), and the
  activity finishes with `RESULT_OK` and the enrollment as a result extra. On failure it shows the
  error and finishes with `RESULT_CANCELED`. The device name and FCM token extras are still
  required. Use `EnrollActivity.getStartIntent(context, deviceName, fcmToken, enrollmentData)` from
  tests in the same package (the extra keys other than `ENROLLMENT_DATA` are private). For manual
  testing use **Enter enrollment link** instead. `EnrollActivity` is **not exported**, so
  `adb shell am start` can't launch it as a normal user; this hook is for instrumentation only.
- **Views**: each control has an ID, and the interactive ones also a content description. Prefer the
  IDs as the locator; the descriptions are for tests that locate views by accessibility label. Each
  description contains the visible label of its button (for example "Deny authentication request"
  for **Deny**), so they also work with Voice Access.

  | Screen | Control | ID | Content description |
  | --- | --- | --- | --- |
  | Main | Enroll button | `enrollButton` | Enroll |
  | Main | Unenroll button | `unenrollButton` | Unenroll |
  | Main | FCM token | `fcmTokenText` | (none, uses its text) |
  | Main | TOTP code | `otpView` | One-time code |
  | Enroll | QR scanner | `scanner` | Guardian QR code scanner |
  | Enroll | Scan hint (also the no-camera message) | `scanHintText` | (none, uses its text) |
  | Enroll | Enter enrollment link button | `enterLinkButton` | Enter enrollment link |
  | Enroll | Same button on the camera permission screen | `enterLinkPermissionButton` | Enter enrollment link |
  | Enroll | Enrollment link input (dialog) | `enrollmentLinkInput` | Enrollment link |
  | Enroll | Camera permission explanation | `permissionMessageText` | (none, uses its text) |
  | Enroll | Request camera permission | `requestPermissionButton` | Request permission to use the camera |
  | Notification | Allow button (label "Allow") | `allowButton` | Allow authentication request |
  | Notification | Reject button (label "Deny") | `rejectButton` | Deny authentication request |
