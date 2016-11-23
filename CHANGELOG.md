## [0.1.0](https://github.com/auth0/Guardian.Android/tree/0.1.0) (2016-11-23)

First release of Guardian for Android

### Requirements

Android API level 15+ is required in order to use Guardian.

### Install

Add these lines to your `build.gradle` dependencies file:

```gradle
compile 'com.auth0.android:guardian:0.1.0'
```

### Usage

Create an instance of `Guardian` using either an url or domain:

```java
Uri url = Uri.parse("https://<TENANT>.guardian.auth0.com/");

Guardian guardian = new Guardian.Builder()
        .url(url)
        .build();
```

or

```java
String domain = "<TENANT>.guardian.auth0.com";

Guardian guardian = new Guardian.Builder()
        .domain(domain)
        .build();
```

To create an enroll, create a pair of RSA keys, obtain the Guardian enrollment data from a Guardian QR code and use it like this:

```java
KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
keyPairGenerator.initialize(2048); // you MUST use at least 2048 bit keys
KeyPair keyPair = keyPairGenerator.generateKeyPair();

CurrentDevice device = new CurrentDevice(context, "gcmToken", "deviceName");

String enrollmentUriFromQr = ...; // the data from a Guardian QR code

guardian
        .enroll(enrollmentUriFromQr, device, keyPair)
        .start(new Callback<Enrollment> {
            @Override
            void onSuccess(Enrollment enrollment) {
               // we have the enrollment data
            }

            @Override
            void onFailure(Throwable exception) {
               // something failed
            }
        });
```

To unenroll just call:

```java
guardian
        .delete(enrollment)
        .execute(); // or start(new Callback<> ...) asynchronously
```

To allow or reject a login request you first need to get the Guardian `Notification`:

```java
// at the GCM listener you receive a Bundle
@Override
public void onMessageReceived(String from, Bundle data) {
    Notification notification = Guardian.parseNotification(data);
    if (notification != null) {
        handleGuardianNotification(notification);
        return;
    }

    /* Handle other push notifications you might be using ... */
}
```

Then, to allow the login request, call:

```java
guardian
        .allow(notification, enrollment)
        .execute(); // or start(new Callback<> ...) asynchronously
```

or to reject it:

```java
guardian
        .reject(notification, enrollment) 
        .execute(); // or start(new Callback<> ...) asynchronously
```
 
 or optionally, to indicate a reject reason:
 
 ```java
guardian
        .reject(notification, enrollment, reason)
        .execute(); // or start(new Callback<> ...) asynchronously
```
