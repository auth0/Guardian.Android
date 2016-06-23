# Guardian SDK

This is a high level client for the Guardian MFA Server

## Usage

### Create instance, enroll an account and approve an auth request

```java
// the API client works for only one guardian domain
String baseUrl = "https://tenant.guardian.auth0.com";

// create the API client
Guardian guardian = new Guardian.Builder()
        .baseUrl(baseUrl)
        .build();

// example: enroll an account
String data = "data from the QR code";

guardian.enroll(data, new Callback<Account> {
        @Override
        void onSuccess(Account account) {
            // we have the enrolled account
        }

        @Override
        void onFailure(Throwable exception) {
            // something failed
        }
});

// at some point we receive a push notification
Bundle pushPayload = ...;

// parse the data
AuthChallenge challenge = guardian.parsePushNotification(pushPayload);

// allow auth request
guardian.allowLogin(account, challenge, new Callback<Void> {
        @Override
        void onSuccess(Void ignore) {
            // the auth request was successfuly allowed
        }

        @Override
        void onFailure(Throwable exception) {
            // something failed
        }
});

// or maybe reject auth request
String rejectReason = "mistake";
guardian.rejectLogin(account, challenge, rejectReason, new Callback<Void> {
        @Override
        void onSuccess(Void ignore) {
            // the auth request was successfuly rejected
        }

        @Override
        void onFailure(Throwable exception) {
            // something failed
        }
});
```

## Methods

### Enroll

```java
/**
 * Enrolls an account
 *
 * @param data the data scanned from a QR code
 * @param callback where the resulting Account (or a failure) will be received
 */
public void enroll(String data, Callback<Account> callback)
```

### Disenroll

```java
/**
 * Disenrolls the given account
 *
 * @param account the account to unenroll
 * @param callback where the result (success or failure) will be notified
 */
public void unenroll(Account account, Callback<Void> callback)
```

### Allow an auth request

```java
/**
 * Allows the authorization request for the provided account and challenge
 *
 * @param account the account that will be used to allow the auth challenge
 * @param challenge the AuthChallenge received in the push notification
 * @param callback where the result (success or failure) will be notified
 */
public void allowLogin(Account account,
                       AuthChallenge challenge,
                       Callback<Void> callback)
```

### Reject an auth request

```java
/**
 * Rejects the authorization request for the provided account and challenge, possibly specifying a reject reason
 *
 * @param account the account that will be used to allow the auth challenge
 * @param challenge the AuthChallenge received in the push notification
 * @param reason the reject reason
 * @param callback where the result (success or failure) will be notified
 */
public void rejectLogin(Account account,
                        AuthChallenge challenge, 
                        String reason, 
                        Callback<Void> callback)
```

### Parse push notification payload

```java
/**
 * Parses the push notification payload
 *
 * @param pushPayload the bundle received in the push notification sent by guardian server
 * @return an AuthChallenge
 */
public AuthChallenge parsePushNotification(Bundle pushPayload)
```
