# Library to easily create HTTP requests

It's based on OkHttp but allows more flexibility when creating requests. It also (de)serializes the
data automatically so you receive an object instance directly.

By default it assumes the server uses JSON formatted data and tries to serialize/parse it using
Gson, but you can easily implement and use your own (de)serializer.

You can even create your own parser for the server errors, so you can create and return exceptions
that are more useful.

## Basic usage

```java
RequestFactory requestFactory = new RequestFactory.Builder()
        .build();

// we want the response deserialized to a User class instance
Request<User> request = requestFactory.newRequest(User.class)
        .baseUrl("tenant.auth0.com")
        .get(String.format("api/v2/users/%s", userId))
        .addQueryParameter("fields", "email")
        .addQueryParameter("include_fields", false)
        .setBearer(jwt);

// execute blocking
User user = null;
try {
    user = request.execute();
} catch (IOException exception) {
    // could not connect to server, no internet, etc
} catch (ServerErrorException exception) {
    // the server returned an error (http code != 2xx)
}

// or execute async
request.start(new Callback<User> {
        @Override
        onSuccess(User user) {
            //
        }

        @Override
        onFailure(Throwable error) {
            // no internet or server error
        }
});

// we want to execute a request without worrying about the response data
Request<Void> request = requestFactory.newRequest()
        .baseUrl("tenant.auth0.com")
        .delete(String.format("api/v2/users/%s", userId))
        .setBearer(jwt);

// execute blocking
try {
    request.execute();
} catch (IOException exception) {
    // could not connect to server, no internet, etc
} catch (ServerErrorException exception) {
    // the server returned an error (http code != 2xx)
}

// or execute async
request.start(new Callback<Void> {
        @Override
        onSuccess(Void response) {
            //
        }

        @Override
        onFailure(Throwable error) {
            // no internet or server error
        }
});
```

## Advanced usage

### Configure the callbacks to be executed on the Android main/UI thread

```java
RequestFactory requestFactory = new RequestFactory.Builder()
        .callbackExecutor(new Executor {
            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void execute(Runnable r) {
                handler.post(r);
            }
        }
        .build();
```

### Configure your own parser for server error responses

```java
RequestFactory requestFactory = new RequestFactory.Builder()
        .errorParser(new ServerErrorParser {
            @Override
            public ServerErrorException parse(Reader reader, int statusCode) {
                return new ServerErrorException("Server error", statusCode);
            }
        })
        .build();
```
