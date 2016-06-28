# Guardian API client

## Begin enroll, request token to update device account

```java
// Start enroll, need to obtain the device account token from the transaction id included in the QR
// so we can later create/update the device account
String deviceAccountToken = apiClient
        .getEnrollmentInfo(enrollmentTransactionIdFromQR)
        .execute();

// or start async
apiClient
        .getEnrollmentInfo(enrollmentTransactionIdFromQR)
        .start(new Callback<String> {
            @Override
            void onSuccess(String deviceAccountToken) {
               // we have the device account token to continue with the enroll flow
            }

            @Override
            void onFailure(Throwable exception) {
               // something failed
            }
       }
```

## Create/update all data of device account

```java
// update device account
DeviceAccount deviceAccount = apiClient
        .deviceAccount(deviceAccountId, deviceAccountToken)
        .update(deviceName, pushServiceToken) // use android identifier and GCM for push service
        .execute(); // or start() async
```

## Update any parameter of the device account

```java
DeviceAccount updatedDeviceAccount = apiClient
        .deviceAccount(deviceAccountId, deviceAccountToken)
        .updateDeviceIdentifier(deviceIdentifier) // optional
        .updateDeviceName(deviceName) // optional
        .updatePushCredentials(pushServiceName, pushServiceToken) // optional
        .execute(); // or start() async
```

## Delete device account (unenroll)

```java
// delete device account
apiClient
        .deviceAccount(deviceAccountId, deviceAccountToken)
        .delete()
        .execute(); // or start() async
```

## Allow login

```java
apiClient
        .authenticationRequest(txToken)
        .allow(otpCode)
        .execute(); // or start() async
```

## Reject login

```java
apiClient
        .authenticationRequest(txToken)
        .reject(otpCode)
        .execute(); // or start() async

// optionally if we have a reject reason
apiClient
        .authenticationRequest(txToken)
        .reject(otpCode, rejectReason)
        .execute(); // or start() async
```

## Get tenant info (picture url, friendly name)

```java
TenantInfo tenantInfo = apiClient
        .getTenantInfo()
        .execute(); // or start() async
```

## Get possible reject reason

```java
List<RejectReason> rejectReasons = apiClient
        .getRejectReasons()
        .execute(); // or start() async
```

## DeviceAccount

Includes:

- **String id**: the device account id
- **String identifier**: an identifier for debug purposes, useful to track devices (should be unique per device, like `Settings.Secure.ANDROID_ID`)
- **String name**: the name that will be displayed to the user on the widget so he knows which device the push
    notification has been sent (and to select the one to use in case we allow multiple devices)
- **String pushService**: always `GCM` for android, its the id of the push notification sender in guardian server
- **String pushToken**: the GCM token of the device, so the server can send notifications to this device

## TenantInfo

Includes:

- **String name**: the tenant name (kind of an identifier of the tenant)
- **String friendlyName**: a friendly name to display to the user for example
- **String pictureUrl**: the url of a tenant's picture/logo

## RejectReason

Includes:

- **String id**: the id used to send when rejecting an auth request
- **String description**: a description to display to display to the user
