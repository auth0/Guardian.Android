# Guardian SDK

We'll have a single class that implements almost all methods required to enroll, update/delete an
enrollment, parse a push notification and allow/reject it.

An instance of this class will be created for a specific tenant/url.

```java
Guardian guardian = new Guardian.Builder()
        .url("tenant.guardian.auth0.com")
        .build();

// or we could also have a .tenant("mytenant") for cloud users
Guardian guardian = new Guardian.Builder()
        .tenant("mytenant") // will use "mytenant.guardian.auth0.com"
        .build();
```

## Enroll

Once we have the guardian instance you can create an enrollment using the data obtained from a
Guardian QR code:

```java
String uriFromQr = ...; // obtain the data from a QR code

Enrollment enrollment = guardian
        .enroll(uriFromQr)
        .execute();

// or start async
guardian
        .enroll(uriFromQr)
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

    // Guardian server url (just in case)
    String getUrl();

    // Issuer/tenant (just in case)
    String getTenant();

    // User name/email
    String getUser();

    // TOTP data, all data we require to generate the code
    // maybe create a class for this? so we have everything in one place?
    int getPeriod(); // maybe we can leave this out if we will always use the default
    int getDigits(); // maybe we can leave this out if we will always use the default
    String getAlgorithm(); // maybe we can leave this out if we will always use the default
    String getSecret(); // base32 encoded secret, as it is on the QR

    // Device class is the same from API client, includes id, name, localIdentifier and gcmToken
    Device getDevice();

    // Token used to authenticate when updating or deleting (unenrolling) the device data
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
        .update(enrollment) // we will just return the update request from the api client
        .name("newName")
        .GCMToken("newGcmToken") // optional, can update both or the name or token only
        .execute(); // or start(new Callback<> ...)
```

## Guardian notification

Guardian will also have a method to parse the `Bundle` received from GCM and return a guardian
notification instance ready to be used.

```java
Notification notification = guardian.getNotification(bundle);
```

Then we use this instance to display info on the UI, or create an android notification or whatever.
And it should be finally used to approve/reject a login.

We'll need some way to establish a relation with the enrollment since the SDK user's app could let
their users connect with more than one account. This object will have (at least) these methods:

```java
public class Notification /* implements Parcelabl? */ {
    String getEnrollmentId(); // to establish the relation with an enrollment
    String getTransactionToken(); // the token used to allow/reject together with the totp code

    String getBrowser();
    String getBrowserVersion();
    String getOS();
    String getOSVersion();
    String getLocation(); // getLatitude/Longitude also? maybe the user wants to display it on map
    Date getDate();
}
```

> It should also be `Parcelable` so we can send them to the activity, the same as the enrollment.
> The UI component/activity to display request information must receive both the enrollment and the
notification.

### Usage

```java
// at the GCM listener we receive a Bundle
Notification notification = guardian.getNotification(bundle);

// get corresponding enrollment from wherever we have them stored
Enrollment enrollment = myEnrollmentsStorage.get(notification.getEnrollmentId());

/* 
 * show info to user, ask what to do with it 
 */

// finally do something
guardian.allow(enrollment, notification).execute();
// or
guardian.reject(enrollment, notification).execute();
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
