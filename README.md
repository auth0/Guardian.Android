# Guardian SDK for Android
============
[![CI Status](https://travis-ci.com/auth0/GuardianSDK.Android.svg?token=R3xUbi1dnaoneyhnspcr&branch=master)](https://travis-ci.com/auth0/GuardianSDK.Android)
[![License](http://img.shields.io/:license-mit-blue.svg?style=flat)](http://doge.mit-license.org)

[Guardian](https://auth0.com/docs/multifactor-authentication/guardian) is Auth0's multifactor
authentication (MFA) service that provides a simple, safe way for you to implement MFA.

[Auth0](https://auth0.com) is an authentication broker that supports social identity providers as
well as enterprise identity providers such as Active Directory, LDAP, Google Apps and Salesforce.

## Requirements

Android API level 15+ is required in order to use Guardian.

## Before getting started

This SDK allows you to integrate Auth0's Guardian multifactor service in your own app, transforming it in the second factor itself.

For this to work you have to configure your tenant's Guardian service with your push notification settings, otherwise you would not receive any push notifications. Please read the [docs](https://auth0.com/docs/multifactor-authentication/guardian) about how you can accomplish that.

##Install

GuardianSDK is available both in [Maven Central](http://search.maven.org) and
[JCenter](https://bintray.com/bintray/jcenter).
To start using *GuardianSDK* add these lines to your `build.gradle` dependencies file:

```gradle
compile 'com.auth0.android:guardian-sdk:0.1.0'
```

## Usage

`Guardian` is the core of the SDK. You'll need to create an instance of this class for your specific
tenant/url.

```java
Uri url = Uri.parse("https://tenant.guardian.auth0.com/");

Guardian guardian = new Guardian.Builder()
        .url(url)
        .build();
```

or

```java
String domain = "tenant.guardian.auth0.com";

Guardian guardian = new Guardian.Builder()
        .domain(domain)
        .build();
```

That's all you need to setup your own instance of `Guardian`

### Enroll

An enrollment is a link between the second factor and an Auth0 account. When an account is enrolled
you'll need the enrollment data to provide the second factor required to verify the
identity. You can create an enrolment using the guardian instance you just created.

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
String enrollmentDataFromQrOrTicket = ...; // the data from a Guardian QR code or enrollment ticket

Enrollment enrollment = guardian
        .enroll(enrollmentDataFromQrOrTicket, "deviceName", "gcmToken", keyPair)
        .execute();
```

or you can also execute the request in a background thread

```java
guardian
        .enroll(enrollmentUriFromQr, "deviceName", "gcmToken", keyPair)
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

The `deviceName` and `gcmToken` are data that you must provide. The `deviceName` is the name that
you want for the enrollment. It will be displayed to the user when the second factor is required.

The GCM token is the token for Google's GCM push notification service. In case your app is not yet
using GCM or you're not familiar with it, you should check their
[docs](https://developers.google.com/cloud-messaging/android/client#sample-register).

### Unenroll

If you want to delete an enrollment -for example if you want to disable MFA- you can make the
following request:

```java
guardian
        .delete(enrollment)
        .execute(); // or start(new Callback<> ...)
```

### Allow a login request

Once you have the enrollment in place, you will receive a GCM push notification every time the user
has to validate his identity with MFA.

Guardian provides a method to parse the `Bundle` received from GCM and return a `Notification`
instance ready to be used.

```java
// at the GCM listener you receive a Bundle
Notification notification = Guardian.parseNotification(bundle);
```

> If the `Bundle` you receive is not a Guardian notification this method will return null, so you
> should check before using it.

Once you have the notification instance, you can easily allow the authentication request by using
the `allow` method. You'll also need the enrollment that you obtained previously.
In case you have more than one enrollment, you'll have to find the one that has the same id as the
notification (you can get the enrollment id with `getEnrollmentId()`.

```java
guardian
        .allow(notification, enrollment)
        .execute(); // or start(new Callback<> ...)
```

### Reject a login request

To deny an authentication request just call `reject` instead. You can also send a reject reason if
you want. The reject reason will be available in the guardian logs.

```java
guardian
        .reject(notification, enrollment) // or reject(notification, enrollment, reason)
        .execute(); // or start(new Callback<> ...)
```

## What is Auth0?

Auth0 helps you to:

* Add authentication with [multiple authentication sources](https://docs.auth0.com/identityproviders),
either social like **Google, Facebook, Microsoft Account, LinkedIn, GitHub, Twitter, Box, Salesforce,
amont others**, or enterprise identity systems like **Windows Azure AD, Google Apps, Active Directory,
ADFS or any SAML Identity Provider**.
* Add authentication through more traditional
**[username/password databases](https://docs.auth0.com/mysql-connection-tutorial)**.
* Add support for **[linking different user accounts](https://docs.auth0.com/link-accounts)** with
the same user.
* Support for generating signed [Json Web Tokens](https://docs.auth0.com/jwt) to call your APIs and
**flow the user identity** securely.
* Analytics of how, when and where users are logging in.
* Pull data from other sources and add it to the user profile, through
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
