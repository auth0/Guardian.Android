# JSON based webservice request creator/builder

The library is just some utility classes that help with the creation of requests to be executed against a JSON web server. It handles the serialization/deserialization of the body/response using Gson, and uses OkHttp for the actual http requests.

It also lets the users to configure the `Executor` where the response callbacks should be executed. This is specially useful for Android users, since they probably want the callbacks to be executed on the main/ui thread.

In addition the user can also set a custom server error parser. This will be used to (try to) parse the error responses and obtain them as already deserialized objects.

## Usage

```java
// The lib will be only java, without anything related to android. So we might find this useful:
// Create simple executor for android that allows callbacks to be executed on the main thread.
Executor callbackExecutor = new Executor() {
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Override
    public void execute(Runnable r) {
        handler.post(r);
    }
};

// Create OkHttp client. Set whatever options/cache/interceptors/loggers you want
OkHttpClient client = new OkHttpClient.Builder()
    .build();

// Create Gson instance. Set whatever options you want. Guardian API client will do something like this:
Gson gson = new GsonBuilder()
    .registerTypeAdapterFactory(new JsonRequiredTypeAdapterFactory())
    // set default date format, etc
    .create();

// Create parser for server errors. This allows abstracting the error response handling and return an actual exception (the user should define it's own exception including the error codes/message/etc)
WebServiceErrorParser webServiceErrorParser 
    = new GuardianWebServiceErrorParser(gson);

// It might be useful to use a 'builder' pattern here
RequestFactory requestFactory 
    = new RequestFactory(callbackExecutor, client, gson, webServiceErrorParser);

// Example of a request
// create body, can be any object that can be serialized by Gson
Object body = new EnrollmentInfoBody(param1, param2, etc);

WebServiceCall<EnrollmentInfo> request = requestFactory.newRequestBuilder() // creates and returns a "request builder"
    .baseUrl(baseUrl) // sets the domain of the server
    .post("api/enrollment-info", body) // we want to make a POST request to this endpoint with the serialized object as body
    .build(EnrollmentInfo.class); // we need to pass the response object's `Class<T>` or `Type` somewhere

// execute asynchronously and get the result on the callback (executed on the android main thread if we use the executor created above)
request.start(new WebServiceCallback<EnrollmentInfo> {
        @Override
        void onSuccess(EnrollmentInfo enrollmentInfo) {
            // we have the enrollment info to continue with the enroll flow
        }

        @Override
        void onFailure(Throwable exception) {
            // something failed
        }
});

// or execute synchronously
try {
    EnrollmentInfo enrollmentInfo = request.execute();
} catch (IOException exception) {
    // there's no internet?
} catch (WebServiceException exception) {
    // an exception that is parsed from the server error response
}
```

## Methods of the request builder

### baseUrl

```java
/**
 * Sets the base url, the domain of the server
 *
 * @param baseUrl the domain of the server
 * @return itself
 */
public RequestBuilder baseUrl(String baseUrl);
```

### addParameter

```java
/**
 * Add a parameter and its value to the body to send when executing the request.
 * You cannot mix name/value parameters and a body object to serialize
 *
 * @param name the name of the parameter
 * @param value the value of the parameter
 * @return itself
 */
public RequestBuilder addParameter(String name, Object value);
```

### addQueryParameter

```java
/**
 * Add a parameter to the query
 *
 * @param name the name of the parameter
 * @param value the value of the parameter
 * @return itself
 */
public RequestBuilder addQueryParameter(String name, String value);
```

### addHeader

```java
/**
 * Add a header parameter
 *
 * @param name the name of the parameter
 * @param value the value of the parameter
 * @return itself
 */
public RequestBuilder addHeader(String name, String value);
```

### setBearer

```java
/**
 * Set the Authorization header. Equivalent to `addHeader("Authorization", "Bearer " + jwt)`
 *
 * @param jwt the JWT token
 * @return itself
 */
public RequestBuilder setBearer(String jwt);
```

### post(path)

```java
/**
 * Sets the path for the request and the method to 'POST'.
 * Useful for cases where the body was set adding individual parameters
 *
 * @param path the path of the endpoint
 * @return itself
 */
public RequestBuilder post(String path);
```

### post(path, body)

```java
/**
 * Sets the path for the request, the method to 'POST' and will include the serialized object
 * as the body of the request
 *
 * @param path the path of the endpoint
 * @param body the object to be serialized and included as the body of the request
 * @return itself
 */
public RequestBuilder post(String path, Object body);
```

### patch(path)

```java
/**
 * Sets the path for the request and the method to 'PATCH'.
 * Useful for cases where the body was set adding individual parameters
 *
 * @param path the path of the endpoint
 * @return itself
 */
public RequestBuilder patch(String path);
```

### patch(path, body)

```java
/**
 * Sets the path for the request, the method to 'PATCH' and will include the serialized object
 * as the body of the request
 *
 * @param path the path of the endpoint
 * @param body the object to be serialized and included as the body of the request
 * @return itself
 */
public RequestBuilder patch(String path, Object body);
```

### delete(path)

```java
/**
 * Sets the path for the request and the method to 'DELETE'
 *
 * @param path the path of the endpoint
 * @return itself
 */
public RequestBuilder delete(String path);
```

### get(path)

```java
/**
 * Sets the path for the request and the method to 'GET'
 *
 * @param path the path of the endpoint
 * @return itself
 */
public RequestBuilder get(String path);
```

### build(class)

```java
/**
 * Builds the request and sets the `Class` to use for the deserialization of the response
 *
 * @param classOfT the `Class` to use for the deserialization of the response
 * @return a request to start or execute
 */
public <T> WebServiceCall<T> build(Class<T> classOfT);
```

### build(type)

```java
/**
 * Builds the request and sets the `Type` to use for the deserialization of the response.
 * Useful in case the response should be deserialized as a `Map<String, Object>` (or something
 * similar) instead of a specific class.
 *
 * @param typeOfT the `Type` to use for the deserialization of the response
 * @return a request to start or execute
 */
public <T> WebServiceCall<T> build(Type typeOfT);
```

### build()

```java
/**
 * Builds a Void request. Useful for cases where the response is not really required and a
 * success/failure is enough
 *
 * @return a request to start or execute
 */
public WebServiceCall<Void> build();
```

## WebServiceCall<T>

The object that's built allows the request to be executed sync or async and get the result.

### Sync

```java
T execute() throws IOException, WebServiceException
```

### Async

```java
void start(WebServiceCallback<T> callback)
```

where `WebServiceCallback<T>` is an interface:

```java
public interface WebServiceCallback<T> {

    void onSuccess(T response);

    void onFailure(Throwable exception);
}
```

## WebServiceErrorParser

It's just an interface that the user must implement to parse the server error responses. Can use Gson or whatever other thing that allows deserializing data from a `java.io.Reader`

```java
public interface WebServiceErrorParser {
    Exception parseServerError(Reader reader);
}
```
