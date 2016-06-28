# Guardian API client

## Begin enroll, request token to create device

```java
// Start enroll, need to obtain the device account token from the transaction id included in the QR
// so we can later create (update) the device account
String token = apiClient
        .getDeviceToken(transactionCode)
        .execute();

// or start async
apiClient
        .getDeviceToken(transactionCode)
        .start(new Callback<String> {
            @Override
            void onSuccess(String token) {
               // we have the device account token to continue with the enroll flow
            }

            @Override
            void onFailure(Throwable exception) {
               // something failed
            }
       }
```

## Create (update) all data of device

```java
// create device account
Device device = apiClient
        .device(identifier, token)
        .create(deviceName, gcmToken) // always use android identifier and 'GCM' as push service
        .execute(); // or start() async
```

## Update a parameter of the device

```java
Device updatedDevice = apiClient
        .device(identifier, token)
        .updateDeviceName(deviceName) // optional
        .updateGCMToken(gcmToken) // optional
        .execute(); // or start() async
```

## Delete device account (unenroll)

```java
// delete device account
apiClient
        .device(identifier, token)
        .delete()
        .execute(); // or start() async
```

## Allow login

```java
apiClient
        .allow(txToken, otpCode)
        .execute(); // or start() async
```

## Reject login

```java
apiClient
        .reject(txToken, otpCode)
        .execute(); // or start() async

// optionally if we have a reject reason
apiClient
        .reject(txToken, otpCode, rejectReason)
        .execute(); // or start() async
```

## Get tenant info (picture url, friendly name)

```java
Tenant tenant = apiClient
        .getTenant()
        .execute(); // or start() async
```

## Get possible reject reason

```java
List<RejectReason> rejectReasons = apiClient
        .getRejectReasons()
        .execute(); // or start() async
```

# POJOs/VOs (only getters)

## Device

Includes:

- **String id**: the device account id
- **String localIdentifier**: an identifier for debug purposes, useful to track devices (should be unique per device, like `Settings.Secure.ANDROID_ID`)
- **String localName**: the name that will be displayed to the user on the web widget so he knows which device the push
    notification has been sent to (and to select the one to use in case we allow multiple devices)
- **String pushService**: always `GCM` for android, its the id of the push notification sender in guardian server
- **String gcmToken**: the GCM token of the device, so the server can send notifications to this device

## Tenant

Includes:

- **String name**: the tenant name (kind of an identifier of the tenant)
- **String friendlyName**: a friendly name to display to the user for example
- **String pictureUrl**: the url of a tenant's picture/logo

## RejectReason

Includes:

- **String id**: the id used to send when rejecting an auth request
- **String description**: a description to display to display to the user
