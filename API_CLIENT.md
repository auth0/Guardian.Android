# Guardian API Client

## Usage

### Get enrollment info example

```java
// the API client works for only one guardian domain
String baseUrl = "https://tenant.guardian.auth0.com";

// create the API client
GuardianAPI client = new GuardianAPI(baseUrl);

// get the enrollment info to start an enroll flow
client.getEnrollmentInfo("enrollmentTransactionId")
    .start(new WebServiceCallback<EnrollmentInfo> {
        @Override
        void onSuccess(EnrollmentInfo enrollmentInfo) {
            // we have the enrollment info to continue with the enroll flow
        }

        @Override
        void onFailure(Throwable exception) {
            // something failed
        }
});
```

## Methods

### Get enrollment info

```java
/**
 * Returns a "device_account_token" that can be used to update the push notification settings
 * and also to un-enroll the device account
 * This endpoint should only be called once (when starting the enroll)
 *
 * @param enrollmentTxId the enrollment transaction id
 *
 * @return
 */
WebServiceCall<EnrollmentInfo> getEnrollmentInfo(String enrollmentTxId);
```

### Update device account

```java
/**
 * Associate a device account that belongs to a user with a pair of device credentials, or
 * update these credentials. Confirms the device account if it was not confirmed
 *
 * @param id                 the device account id
 * @param deviceAccountToken the jwt to use in authorization header
 * @param identifier         a unique identifier for the device
 * @param name               the visible name of the device
 * @param pushService        the push service to use
 * @param token              the push service token
 * @return
 */
WebServiceCall<DeviceAccount> updateDeviceAccount(String id, String deviceAccountToken,
                                                  String identifier, String name,
                                                  String pushService, String token);
```

### Delete device account

```java
/**
 * Deletes (un-enrolls) a device account
 *
 * @param id                 the device account id
 * @param deviceAccountToken the jwt to use in authorization header
 *
 * @return
 */
WebServiceCall<Void> deleteDeviceAccount(String id, String deviceAccountToken);
```

### Get tenant info

```java
/**
 * Returns tenant information (friendly_name / picture_url)
 *
 * @return
 */
WebServiceCall<TenantInfo> getTenantInfo()
```

### Allow login

```java
/**
 * Accepts/verifies a login transaction using the OTP code
 *
 * @param txToken the jwt to use in authorization header
 * @param code    the OTP code
 *
 * @return
 */
WebServiceCall<Void> allowLogin(String txToken, String code);
```

### Reject login

```java
/**
 * Rejects a login, possibly indicating the reason to be rejected
 *
 * @param txToken the jwt to use in authorization header
 * @param code    the OTP code
 * @param reason  the reject reason
 *
 * @return
 */
WebServiceCall<Void> rejectLogin(String txToken, String code, String reason);
```

### Get reject reasons

```java
/**
 * Obtain a list of reject reasons
 *
 * @return
 */
WebServiceCall<List<RejectReason>> getRejectReasons();
```

## WebServiceCall<T>

All methods returns an object that allows the request to be executed sync or async and get the result.

### Sync

```java
T execute() throws IOException, WebServiceException
```

### Async

```java
void start(WebServiceCallback<T> callback)
```

where `WebServiceCallback<T>` is an interface like this:

```java
public interface WebServiceCallback<T> {

    void onSuccess(T response);

    void onFailure(Throwable exception);
}
```
