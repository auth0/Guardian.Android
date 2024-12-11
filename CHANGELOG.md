# Change Log

## [0.8.1](https://github.com/auth0/Guardian.Android/tree/0.8.1) (2024-12-11)
[Full Changelog](https://github.com/auth0/Guardian.Android/compare/0.8.0...0.8.1)

**Changed**
- Fix for JsonIOException when making request


## [0.8.0](https://github.com/auth0/Guardian.Android/tree/0.8.0) (2024-03-15)
[Full Changelog](https://github.com/auth0/Guardian.Android/compare/0.7.0...0.8.0)

**Changed**
- Add telemetry to track usage


## [0.7.0](https://github.com/auth0/Guardian.Android/tree/0.6.0) (2023-11-01)
[Full Changelog](https://github.com/auth0/Guardian.Android/compare/0.6.0...0.7.0)

**Changed**
- Add support for transaction linking id
- Add support for custom domain

## [0.6.0](https://github.com/auth0/Guardian.Android/tree/0.6.0) (2021-06-01)
[Full Changelog](https://github.com/auth0/Guardian.Android/compare/0.5.0...0.6.0)

**Changed**
- Use JWT to authenticate /api/device-accounts [SDK-2561] [\#95](https://github.com/auth0/Guardian.Android/pull/95) ([lbalmaceda](https://github.com/lbalmaceda))

**Deprecated**
- Deprecate the 'device' method and document the alternative [\#96](https://github.com/auth0/Guardian.Android/pull/96) ([lbalmaceda](https://github.com/lbalmaceda))

**Breaking changes**
- Migrate to AndroidX  [\#94](https://github.com/auth0/Guardian.Android/pull/94) ([lbalmaceda](https://github.com/lbalmaceda))

## [0.5.0](https://github.com/auth0/Guardian.Android/tree/0.5.0) (2020-07-29)
[Full Changelog](https://github.com/auth0/Guardian.Android/compare/0.4.0...0.5.0)

## [0.4.0](https://github.com/auth0/Guardian.Android/tree/0.4.0) (2019-04-05)
This release improves the compatibility with Firebase Cloud Messaging (FCM) which has replaced Google Cloud Messaging (GCM) and should no longer been used as April 11. More information on the Firebase blog https://firebase.googleblog.com/2018/04/time-to-upgrade-from-gcm-to-fcm.html

[Full Changelog](https://github.com/auth0/Guardian.Android/compare/0.3.0...0.4.0)

**Added**
- Allow to parse notifications from a Map instance [\#84](https://github.com/auth0/Guardian.Android/pull/84) ([lbalmaceda](https://github.com/lbalmaceda))

**Changed**
- Migrate sample from GCM to FCM [\#87](https://github.com/auth0/Guardian.Android/pull/87) ([lbalmaceda](https://github.com/lbalmaceda))
- Update gradle and dependencies version [\#82](https://github.com/auth0/Guardian.Android/pull/82) ([lbalmaceda](https://github.com/lbalmaceda))

**Deprecated**
- Deprecate methods to parse a notification payload from a Bundle [\#85](https://github.com/auth0/Guardian.Android/pull/85) ([lbalmaceda](https://github.com/lbalmaceda))

## [0.3.0](https://github.com/auth0/Guardian.Android/tree/0.3.0) (2017-06-01)
[Full Changelog](https://github.com/auth0/Guardian.Android/compare/0.2.0...0.3.0)

**Added**
- Use Auth0 OSS release plugin [\#70](https://github.com/auth0/Guardian.Android/pull/70) ([nikolaseu](https://github.com/nikolaseu))
- Add support for appliance [\#69](https://github.com/auth0/Guardian.Android/pull/69) ([nikolaseu](https://github.com/nikolaseu))

## [0.2.0](https://github.com/auth0/Guardian.Android/tree/0.2.0) (2016-12-07)
[Full Changelog](https://github.com/auth0/Guardian.Android/compare/0.1.0...0.2.0)

**Added**
- Add method to get errorCode and isLoginTransactionNotFound() [#62](https://github.com/auth0/Guardian.Android/pull/62) ([nikolaseu](https://github.com/nikolaseu))

## [0.1.0](https://github.com/auth0/Guardian.Android/tree/0.1.0) (2016-11-23)

First release of Guardian for Android

### Install

Add these lines to your `build.gradle` dependencies file:

```gradle
compile 'com.auth0.android:guardian:0.1.0'
```

### Usage

Create an instance of `Guardian`:

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

Then, to allow the login request:

```java
guardian
        .allow(notification, enrollment)
        .execute(); // or start(new Callback<> ...) asynchronously
```
