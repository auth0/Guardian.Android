# Guardian SDK for Android

[![CircleCI](https://img.shields.io/circleci/project/github/auth0/Guardian.Android.svg)](https://circleci.com/gh/auth0/Guardian.Android)
[![Coverage Status](https://img.shields.io/codecov/c/github/auth0/Guardian.Android/master.svg)](https://codecov.io/github/auth0/Guardian.Android)
[![License](http://img.shields.io/:license-mit-blue.svg)](http://doge.mit-license.org)
[![Maven Central](https://img.shields.io/maven-central/v/com.auth0.android/guardian.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.auth0.android%22%20AND%20a%3A%22guardian%22)
[![Download](https://api.bintray.com/packages/auth0/android/guardian/images/download.svg)](https://bintray.com/auth0/android/guardian/_latestVersion)

[Guardian](https://auth0.com/docs/multifactor-authentication/guardian) is Auth0's multi-factor
authentication (MFA) service that provides a simple, safe way for you to implement MFA.

[Auth0](https://auth0.com) is an authentication broker that supports social identity providers as
well as enterprise identity providers such as Active Directory, LDAP, Google Apps and Salesforce.

This SDK allows you to integrate Auth0's Guardian multi-factor service in your own app, transforming
it in the second factor itself. Your users will get all the benefits of our frictionless
multi-factor authentication from your app.

## Requirements

Android API level 15+ is required in order to use Guardian.

## Before getting started

To use this SDK you have to configure your tenant's Guardian service with your own push notification
credentials, otherwise you would not receive any push notifications. Please read the
[docs](https://auth0.com/docs/multifactor-authentication/guardian) about how to accomplish that.

## Install

GuardianSDK is available both in [Maven Central](http://search.maven.org) and
[JCenter](https://bintray.com/bintray/jcenter).
To start using _GuardianSDK_ add these lines to your `build.gradle` dependencies file:

```gradle
implementation 'com.auth0.android:guardian:0.8.1'
```

## Usage

`Guardian` is the core of the SDK. You'll need to create an instance of this class for your specific
tenant/url.

```java
Uri url = Uri.parse("https://<AUTH0_TENANT_DOMAIN>/appliance-mfa");

Guardian guardian = new Guardian.Builder()
        .url(url)
        .build();
```

alternatively you can use the custom domain if you configured one

```java
Uri url = Uri.parse("https://<CUSTOM_DOMAIN>/appliance-mfa");

Guardian guardian = new Guardian.Builder()
        .url(url)
        .build();
```

That's all you need to setup your own instance of `Guardian`

### Enroll

An enrollment is a link between the second factor and an Auth0 account. When an account is enrolled
you'll need the enrollment data to provide the second factor required to verify the identity. You
can create an enrolment using the guardian instance you just created.

First you'll need to obtain the enrollment info by scanning a Guardian QR code or obtaining an
enrollment ticket by email for example.

Next you'll have to create a new pair of RSA keys for the new enrollment. The private key will be
used to sign the requests to allow or reject a login. The public key will be sent during the enroll
process so the server can later verify the request's signature.

```java
KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
keyPairGenerator.initialize(2048); // you should use at least 2048 bit keys
KeyPair keyPair = keyPairGenerator.generateKeyPair();
```

Then you just use the `enroll` method like this:

```java
CurrentDevice device = new CurrentDevice(context, "fcmToken", "deviceName");

String enrollmentUriFromQr = ...; // the data from a Guardian QR code or enrollment ticket

Enrollment enrollment = guardian
        .enroll(enrollmentUriFromQr, device, keyPair)
        .execute();
```

or you can also execute the request in a background thread

```java
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

The `deviceName` and `fcmToken` are data that you must provide:

- The `deviceName` is the name that you want for the enrollment. It will be displayed to the user
  when the second factor is required.

- The FCM token is the token for Firebase Cloud Messaging push notification service. In case your app
  is not yet using FCM or you're not familiar with it, you should check their
  [docs](https://firebase.google.com/docs/cloud-messaging/android/client#sample-register).

#### A note about key generation

The Guardian SDK does not provide methods for generating and storing cryptographic keys used for enrollment
as this is an application specific concern and could vary between targeted versions of Android and 
OEM-specific builds. The example given above and that used in the sample application is a naive implementation 
which may not be suitable for production applications. It is recommended that you follow [OWASP guidelines
for Android Cryptographic APIs](https://mas.owasp.org/MASTG/0x05e-Testing-Cryptography/) for your implementation.

As of version 0.9.0 the public key used for enrollment was added to the Enrollment Interface as it is
required for [fetching rich-consent details](#fetch-rich-consent-details). For new installs,
this is not a a concern. For enrollments created prior to this version, depending on implementation, 
this key may or may not have been stored with the enrollment information. If this key was discarded, 
it may be possible to reconstruct from the stored signing key. The sample app provides 
[an example](app/src/main/java/com/auth0/guardian/sample/ParcelableEnrollment.java#L188) of this. If 
this is not possible, devices will require re-enrollment to make use of this functionality.

### Unenroll

If you want to delete an enrollment -for example if you want to disable MFA- you can make the
following request:

```java
guardian
        .delete(enrollment)
        .execute(); // or start(new Callback<> ...) asynchronously
```

### Allow a login request

Once you have the enrollment in place, you will receive a FCM push notification every time the user
has to validate his identity with MFA.

Guardian provides a method to parse the `Map<String, String>` data inside the
[RemoteMessage](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/RemoteMessage)
received from FCM and return a `Notification` instance ready to be used.

```java
// at your FCM listener you receive a RemoteMessage
@Override
public void onMessageReceived(RemoteMessage message) {
    Notification notification = Guardian.parseNotification(message.getData());
    if (notification != null) {
        handleGuardianNotification(notification);
        return;
    }

    /* Handle other push notifications you might be using ... */
}
```

> If the `RemoteMessage` you receive is not from a Guardian notification this method will return null,
> so you should always check before using it.

Once you have the notification instance, you can easily allow the authentication request by using the
`allow` method. You'll also need the enrollment that you obtained previously. In case you have more
than one enrollment, you'll have to find the one that has the same id as the notification (you can
get the enrollment id with `getEnrollmentId()`.

```java
guardian
  .allow(notification, enrollment)
  .execute(); // or start(new Callback<> ...) asynchronously
```

### Reject a login request

To deny an authentication request just call `reject` instead. You can also send a reject reason if you want.
The reject reason will be available in the guardian logs.

```java
guardian
  .reject(notification, enrollment) // or reject(notification, enrollment, reason)
  .execute(); // or start(new Callback<> ...) asynchronously
```

### Fetch rich consent details

When you receive a push notification, the presence of the property `transactionLinkingId` indicates a
rich consent record may be associated to the transaction.

To fetch the rich consent details, you can use the `fetchConsent` method.

```java
if (notification.getTransctionLinkingId() != null) {
    guardian
      .fetchConsent(notification, enrollment)
      .start(new Callback<Enrollment> {
        @Override
        void onSuccess(RichConsent consentDetails) {
          // we have the consent details 
        }

        @Override
        void onFailure(Throwable exception) {
          if (exception instanceof GuardianException) {
            GuardianException guardianException = (GuardianException) exception;
            if (guardianException.isResourceNotFound()) {
              // there is no consent associated with the transaction
            }
          }
          // something went wrong 
        }
      });
}
```

## What is Auth0?

Auth0 helps you to:

- Add authentication with [multiple authentication sources](https://docs.auth0.com/identityproviders),
  either social like **Google, Facebook, Microsoft Account, LinkedIn, GitHub, Twitter, Box, Salesforce,
  among others**, or enterprise identity systems like **Windows Azure AD, Google Apps, Active Directory,
  ADFS or any SAML Identity Provider**.
- Add authentication through more traditional
  **[username/password databases](https://docs.auth0.com/mysql-connection-tutorial)**.
- Add support for **[linking different user accounts](https://docs.auth0.com/link-accounts)** with
  the same user.
- Support for generating signed [Json Web Tokens](https://docs.auth0.com/jwt) to call your APIs and
  **flow the user identity** securely.
- Analytics of how, when and where users are logging in.
- Pull data from other sources and add it to the user profile, through
  [JavaScript rules](https://docs.auth0.com/rules).

## Create a free account in Auth0

1. Go to [Auth0](https://auth0.com) and click Sign Up.
2. Use Google, GitHub or Microsoft Account to login.

## Issue Reporting

If you have found a bug or if you have a feature request, please report them at this repository
issues section. Please do not report security vulnerabilities on the public GitHub issue tracker.
The [Responsible Disclosure Program](https://auth0.com/whitehat) details the procedure for
disclosing security issues.

## Author

[Auth0](https://auth0.com)

## License

This project is licensed under the MIT license. See the [LICENSE](LICENSE) file for more info.
