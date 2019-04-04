# GuardianSDK sample app

## Running

Before running the sample app please configure your Firebase app and Guardian URL properly.

You should place the `google-services.json` file (required for FCM push notifications) inside your Android app module root. Typically at` at `MyApplication/app/google-services.json`.

The Guardian URL can be changed at `src/main/res/values/guardian.xml`. It should look like
`https://<YOUR_TENANT>.guardian.auth0.com`.