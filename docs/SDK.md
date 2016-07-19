# Guardian SDK

We'll have a single class that implements almost all methods required to enroll, update/delete an
enrollment, parse a push notification and allow/reject it.

An instance of this class will be created for a specific tenant/url.

```java
Uri url = Uri.parse("tenant.guardian.auth0.com");

Guardian guardian = new Guardian.Builder()
        .url(url) // or .domain("tenant.guardian.auth0.com")
        .deviceName("user visible name for this device")
        .gcmToken("token")
        .build();
```

## Enroll

Once we have the guardian instance you can create an enrollment using the data obtained from a
Guardian QR code:

```java
Uri enrollmentUriFromQr = ...; // obtain the data from a QR code

Enrollment enrollment = guardian
        .enroll(enrollmentUriFromQr)
        .execute();

// or start async
guardian
        .enroll(enrollmentUriFromQr)
        .start(new Callback<Enrollment> {
            @Override
            void onSuccess(Enrollment enrollment) {
               // we have the complete enrollment data
            }

            @Override
            void onFailure(Throwable exception) {
               // something failed
            }
        });
```

A successful enroll will return an object implementing the following interface

```java
public interface Enrollment /* extends Parcelabl? */ {
    // we'll need to generate something so we can match push notifications with the enrollment
    // or we could use the device id since this will be for single tenants?
    // anyway, we only need to be sure we can obtain the same from the notification
    String getId();

    // Guardian server url (just in case, but if we use a enrollment.id != server id then we can include the url in the id itself, in case we want to avoid possible collisions)
    String getUrl();

    // Issuer/tenant (useless? maybe just call it label?)
    String getTenant();

    // User name/email
    String getUser();

    // TOTP data, all data we require to generate the code
    // maybe create a class for this? so we have everything in one place?
    int getPeriod(); // maybe we can leave this out if we will always use the default
    int getDigits(); // maybe we can leave this out if we will always use the default
    String getAlgorithm(); // maybe we can leave this out if we will always use the default
    String getSecret(); // base32 encoded secret, as it is on the QR

    //
    // Data from Device class (API client) includes id, name, localIdentifier and gcmToken
    //

    /**
     * This is the actual id of the enrollment on guardian server
     */
    String getDeviceId();

    /**
     * The identifier of the physical device, for debug/tracking purposes
     */
    String getDeviceLocalIdentifier();

    /**
     * The name to display to the user whenever it has to choose where to send the push notification
     * or at the admin interface for example if the user want's to delete one enrollment
     */
    String getDeviceName();

    /**
     * The GCM token for this physical device, required to check against the current token and
     * update in case it's not the same. Needs to be up-to-data for the push notifications to work.
     */
    String getGCMToken();

    /**
     * The token used to authenticate when updating the device data or deleting the enrollment
     */
    String getDeviceToken();
}
```

> We might require this to be parcelable so we can send/receive it to/from other activity. For
> example the enroll activity, or the activity to display info about a notification

### Unenroll

```java
guardian
        .delete(enrollment)
        .execute(); // or start(new Callback<> ...)
```

### Allow login

```java
guardian
        .allow(enrollment, notification)
        .execute(); // or start(new Callback<> ...)
```

### Reject login

```java
guardian
        .reject(enrollment, notification) // or reject(enrollment, notification, reason)
        .execute(); // or start(new Callback<> ...)
```

### Update name, local identifier or GCM token

```java
guardian
        .getAPIClient()
        .device(enrollment.getDeviceId(), enrollment.getDeviceToken())
        .update("newIdentifier", "newName", "newGcmToken") // any value can be null (is optional, won't change the server value)
        .execute(); // or start(new Callback<> ...)
```

## Guardian notification

Guardian will also have a method to parse the `Bundle` received from GCM and return a guardian
notification instance ready to be used.

```java
Notification notification = Guardian.parseNotification(bundle); // static method
```

Then we use this instance to display info on the UI, or create an android notification or whatever.
And it should be finally used to approve/reject a login.

We'll need some way to establish a relation with the enrollment since the SDK user's app could let
their users connect with more than one account. This object will have (at least) these methods:

```java
public class Notification {

    /**
     * The id of the enrollment
     */
    String getEnrollmentId();

    /**
     * The transaction token, used to identify the authentication request
     */
    String getTransactionToken();

    /**
     * Just information about the authentication request
     */
    Date getDate();
    String getOsName();
    String getOsVersion();
    String getBrowserName();
    String getBrowserVersion();
    String getLocation();
    Double getLatitude();
    Double getLongitude();
}
```

> It should also be `Parcelable` so we can send them to the activity, the same as the enrollment.
> The UI component/activity to display request information must receive both the enrollment and the
notification.

### Usage

```java
// at the GCM listener we receive a Bundle
Notification notification = Guardian.parseNotification(bundle);

// get corresponding enrollment from wherever we have them stored
Enrollment enrollment = myEnrollmentsStorage.get(notification.getEnrollmentId());

/* 
 * show info to user, ask what to do with it 
 */

// finally do something
guardian
        .allow(enrollment, notification)
        .execute(); // or start(new Callback<> ...)
// or
guardian
        .reject(enrollment, notification)
        .execute(); // or start(new Callback<> ...)
```

# UI components

## EnrollActivity

Super easy way of enrolling (for example for POCs or for lazy devs).

A simple themable activity with the QR scanner and some label at the bottom with a message. Also we
need something to display errors. Will handle runtime permissions.

```java
GuardianUI.enroll(this, ENROLL_REQUEST);
```

then receive the `Enrollment` as:

```java
@Override
public void onActivityResult(requestCode, result, data) {
    /* 
     * first should check if request is ours and result is success
     */

    // get enrollment directly from the bundle
    Enrollment enrollment = data.getParcelableExtra(GuardianUI.ENROLLMENT);
}
```

## EnrollView

We might want do provide a view, so we facilitate the enroll but give the user more flexibility to
design its own UI. The view itself will be just the camera preview (maybe with a square so the final
user know where to frame the QR) but will also have all enroll functionality.

Some methods will be `start()`, `stop()`, `resume()`, `pause()` and the user would have to set a
listener to receive the `Enrollment` or the errors that might happen.

The user will be responsible of handling the runtime permission.

> Maybe we should only provide a QRScannerView ? And leave the user the enroll calls itself? <br/>
> Anyway, the QRScannerView will have to include the methods start/stop/pause/resume so not much
difference.

## NotificationView/Activity

Displays the notification info like tenant, username, date, location, browser.
Will require both the enrollment and the notification. Will have buttons to allow/reject.

> If we make only a view, we might leave the allow/deny to be handled by the app itself so the
NotificationView is only informative, not actionable.

## EnrollmentView/Activity

IMO we should not need this. The app would already have it's own UI where there's info about the
user. Why repeat that?

> See OTPCodeView

## OTPCodeView

We could provide a view so the user doesn't have to know how to generate the code, but is still able
to display it to allow offline access. We will need the Enrollment or OTP data as a parameter.
