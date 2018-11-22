// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;

////////////////////////////////
///// HTTP Client Endpoint /////
////////////////////////////////

# The HTTP client provides the capability for initiating contact with a remote HTTP service. The API it
# provides includes functions for the standard HTTP methods, forwarding a received request and sending requests
# using custom HTTP verbs.

# + config - The configurations associated with the client
public type Client client object {

    public ClientEndpointConfig config = {};

    public new(ClientEndpointConfig c) {
        self.config = c;
        self.init(self.config);
    }

    # Gets invoked to initialize the endpoint. During initialization, configurations provided through the `config`
    # record is used to determine which type of additional behaviours are added to the endpoint (e.g: caching,
    # security, circuit breaking).
    #
    # + c - The configurations to be used when initializing the endpoint
    public function init(ClientEndpointConfig c);

    # The `post()` function can be used to send HTTP POST requests to HTTP endpoints.
    #
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream server
    public remote function post(@sensitive string path, Request|string|xml|json|byte[]|io:ReadableByteChannel|mime:Entity[]|()
                                                            message) returns Response|error {
        Request req = buildRequest(message);
        return nativePost(self.config, path, req);
    }

    # The `head()` function can be used to send HTTP HEAD requests to HTTP endpoints.
    #
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream server
    public remote function head(@sensitive string path, Request|string|xml|json|byte[]|io:ReadableByteChannel|mime:Entity[]|()
                                                            message = ()) returns Response|error {
        Request req = buildRequest(message);
        return nativeHead(self.config, path, req);
    }

    # The `put()` function can be used to send HTTP PUT requests to HTTP endpoints.
    #
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream server
    public remote function put(@sensitive string path, Request|string|xml|json|byte[]|io:ReadableByteChannel|mime:Entity[]|()
                                                            message) returns Response|error {
        Request req = buildRequest(message);
        return nativePut(self.config, path, req);
    }

    # Invokes an HTTP call with the specified HTTP verb.
    #
    # + httpVerb - HTTP verb value
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream server
    public remote function execute(@sensitive string httpVerb, @sensitive string path, Request|string|xml|json|byte[]
                                                            |io:ReadableByteChannel|mime:Entity[]|() message) returns Response|error {
        Request req = buildRequest(message);
        return nativeExecute(self.config, httpVerb, path, req);
    }

    # The `patch()` function can be used to send HTTP PATCH requests to HTTP endpoints.
    #
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream server
    public remote function patch(@sensitive string path, Request|string|xml|json|byte[]|io:ReadableByteChannel|mime:Entity[]|()
                                                            message) returns Response|error {
        Request req = buildRequest(message);
        return nativePatch(self.config, path, req);
    }

    # The `delete()` function can be used to send HTTP DELETE requests to HTTP endpoints.
    #
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream server
    public remote function delete(@sensitive string path, Request|string|xml|json|byte[]|io:ReadableByteChannel|mime:Entity[]|()
                                                            message) returns Response|error {
        Request req = buildRequest(message);
        return nativeDelete(self.config, path, req);
    }

    # The `get()` function can be used to send HTTP GET requests to HTTP endpoints.
    #
    # + path - Request path
    # + message - An optional HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream server
    public remote function get(@sensitive string path, Request|string|xml|json|byte[]|io:ReadableByteChannel|mime:Entity[]|()
                                                            message = ()) returns Response|error {
        Request req = buildRequest(message);
        return nativeGet(self.config, path, req);
    }

    # The `options()` function can be used to send HTTP OPTIONS requests to HTTP endpoints.
    #
    # + path - Request path
    # + message - An optional HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream server
    public remote function options(@sensitive string path, Request|string|xml|json|byte[]|io:ReadableByteChannel|mime:Entity[]|()
                                                            message = ()) returns Response|error {
        Request req = buildRequest(message);
        return nativeOptions(self.config, path, req);
    }

    # The `forward()` function can be used to invoke an HTTP call with inbound request's HTTP verb
    #
    # + path - Request path
    # + request - An HTTP inbound request message
    # + return - The response for the request or an `error` if failed to establish communication with the upstream server
    public remote extern function forward(@sensitive string path, Request request) returns Response|error;

    # Submits an HTTP request to a service with the specified HTTP verb.
    # The `submit()` function does not give out a `Response` as the result,
    # rather it returns an `HttpFuture` which can be used to do further interactions with the endpoint.
    #
    # + httpVerb - The HTTP verb value
    # + path - The resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - An `HttpFuture` that represents an asynchronous service invocation, or an `error` if the submission fails
    public remote function submit(@sensitive string httpVerb, string path, Request|string|xml|json|byte[]|
                                            io:ReadableByteChannel|mime:Entity[]|() message) returns HttpFuture|error {
        Request req = buildRequest(message);
        return nativeSubmit(self.config, httpVerb, path, req);
    }

    # Retrieves the `Response` for a previously submitted request.
    #
    # + httpFuture - The `HttpFuture` related to a previous asynchronous invocation
    # + return - An HTTP response message, or an `error` if the invocation fails
    public remote extern function getResponse(HttpFuture httpFuture) returns Response|error;

    # Checks whether a `PushPromise` exists for a previously submitted request.
    #
    # + httpFuture - The `HttpFuture` relates to a previous asynchronous invocation
    # + return - A `boolean` that represents whether a `PushPromise` exists
    public remote extern function hasPromise(HttpFuture httpFuture) returns (boolean);

    # Retrieves the next available `PushPromise` for a previously submitted request.
    #
    # + httpFuture - The `HttpFuture` relates to a previous asynchronous invocation
    # + return - An HTTP Push Promise message, or an `error` if the invocation fails
    public remote extern function getNextPromise(HttpFuture httpFuture) returns PushPromise|error;

    # Retrieves the promised server push `Response` message.
    #
    # + promise - The related `PushPromise`
    # + return - A promised HTTP `Response` message, or an `error` if the invocation fails
    public remote extern function getPromisedResponse(PushPromise promise) returns Response|error;

    # Rejects a `PushPromise`. When a `PushPromise` is rejected, there is no chance of fetching a promised
    # response using the rejected promise.
    #
    # + promise - The Push Promise to be rejected
    public remote extern function rejectPromise(PushPromise promise);
};

# Represents a single service and its related configurations.
#
# + url - URL of the target service
# + secureSocket - Configurations for secure communication with the remote HTTP endpoint
public type TargetService record {
    string url = "";
    SecureSocket? secureSocket = ();
    !...
};

# Provides a set of configurations for controlling the behaviours when communicating with a remote HTTP endpoint.
#
# + url - URL of the target service
# + circuitBreaker - Configurations associated with Circuit Breaker behaviour
# + timeoutMillis - The maximum time to wait (in milliseconds) for a response before closing the connection
# + keepAlive - Specifies whether to reuse a connection for multiple requests
# + chunking - The chunking behaviour of the request
# + httpVersion - The HTTP version understood by the client
# + forwarded - The choice of setting `forwarded`/`x-forwarded` header
# + followRedirects - Configurations associated with Redirection
# + retryConfig - Configurations associated with Retry
# + proxy - Proxy server related options
# + connectionThrottling - Configurations for connection throttling
# + secureSocket - SSL/TLS related options
# + cache - HTTP caching related configurations
# + compression - Specifies the way of handling compression (`accept-encoding`) header
# + auth - HTTP authentication related configurations
public type ClientEndpointConfig record {
    string url = "";
    CircuitBreakerConfig? circuitBreaker = ();
    int timeoutMillis = 60000;
    KeepAlive keepAlive = KEEPALIVE_AUTO;
    Chunking chunking = "AUTO";
    string httpVersion = "1.1";
    string forwarded = "disable";
    FollowRedirects? followRedirects = ();
    RetryConfig? retryConfig = ();
    ProxyConfig? proxy = ();
    ConnectionThrottling? connectionThrottling = ();
    SecureSocket? secureSocket = ();
    CacheConfig cache = {};
    Compression compression = COMPRESSION_AUTO;
    AuthConfig? auth = ();
    !...
};

extern function createHttpClient(string uri, ClientEndpointConfig config) returns Client;

extern function createSimpleHttpClient(string uri, ClientEndpointConfig config) returns Client;

# Provides configurations for controlling the retry behaviour in failure scenarios.
#
# + count - Number of retry attempts before giving up
# + interval - Retry interval in milliseconds
# + backOffFactor - Multiplier of the retry interval to exponentailly increase retry interval
# + maxWaitInterval - Maximum time of the retry interval in milliseconds
# + statusCodes - HTTP response status codes which are considered as failures
public type RetryConfig record {
    int count = 0;
    int interval = 0;
    float backOffFactor = 0.0;
    int maxWaitInterval = 0;
    int[] statusCodes = [];
    !...
};

# Provides configurations for facilitating secure communication with a remote HTTP endpoint.
#
# + trustStore - Configurations associated with TrustStore
# + keyStore - Configurations associated with KeyStore
# + certFile - A file containing the certificate of the client
# + keyFile - A file containing the private key of the client
# + keyPassword - Password of the private key if it is encrypted
# + trustedCertFile - A file containing a list of certificates or a single certificate that the client trusts
# + protocol - SSL/TLS protocol related options
# + certValidation - Certificate validation against CRL or OCSP related options
# + ciphers - List of ciphers to be used
#             eg: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA
# + verifyHostname - Enable/disable host name verification
# + shareSession - Enable/disable new SSL session creation
# + ocspStapling - Enable/disable OCSP stapling
public type SecureSocket record {
    TrustStore? trustStore = ();
    KeyStore? keyStore = ();
    string certFile = "";
    string keyFile = "";
    string keyPassword = "";
    string trustedCertFile = "";
    Protocols? protocol = ();
    ValidateCert? certValidation = ();
    string[] ciphers = [];
    boolean verifyHostname = true;
    boolean shareSession = true;
    boolean ocspStapling = false;
    !...
};

# Provides configurations for controlling the endpoint's behaviour in response to HTTP redirect related responses.
#
# + enabled - Enable/disable redirection
# + maxCount - Maximum number of redirects to follow
public type FollowRedirects record {
    boolean enabled = false;
    int maxCount = 5;
    !...
};

# Proxy server configurations to be used with the HTTP client endpoint.
#
# + host - Host name of the proxy server
# + port - Proxy server port
# + userName - Proxy server username
# + password - proxy server password
public type ProxyConfig record {
    string host = "";
    int port = 0;
    string userName = "";
    string password = "";
    !...
};

# Provides configurations for throttling connections of the endpoint.
#
# + maxActiveConnections - Maximum number of active connections allowed for the endpoint. The default value, -1,
#                          indicates that the number of connections are not restricted.
# + waitTime - Maximum waiting time for a request to grab an idle connection from the client
# + maxActiveStreamsPerConnection - Maximum number of active streams allowed per an HTTP/2 connection
public type ConnectionThrottling record {
    int maxActiveConnections = -1;
    int waitTime = 60000;
    int maxActiveStreamsPerConnection = -1;
    !...
};

# AuthConfig record can be used to configure the authentication mechanism used by the HTTP endpoint.
#
# + scheme - Scheme of the configuration (Basic, OAuth2, JWT etc.)
# + username - Username for Basic authentication
# + password - Password for Basic authentication
# + accessToken - Access token for OAuth2 authentication
# + refreshToken - Refresh token for OAuth2 authentication
# + refreshUrl - Refresh token URL for OAuth2 authentication
# + consumerKey - Consumer key for OAuth2 authentication
# + consumerSecret - Consumer secret for OAuth2 authentication
# + tokenUrl - Token URL for OAuth2 authentication
# + clientId - Clietnt ID for OAuth2 authentication
# + clientSecret - Client secret for OAuth2 authentication
# + credentialBearer - How client authentication is sent to refresh access token (AuthHeaderBearer, PostBodyBearer)
# + scopes - Scope of the access request
public type AuthConfig record {
    AuthScheme scheme;
    string username = "";
    string password = "";
    string accessToken = "";
    string refreshToken = "";
    string refreshUrl = "";
    string consumerKey = "";
    string consumerSecret = "";
    string tokenUrl = "";
    string clientId = "";
    string clientSecret = "";
    CredentialBearer credentialBearer = AUTH_HEADER_BEARER;
    string[] scopes = [];
    !...
};

function Client.init(ClientEndpointConfig c) {
    self = createSimpleHttpClient(c.url, c);
    //boolean httpClientRequired = false;
    //string url = c.url;
    //if (url.hasSuffix("/")) {
    //    int lastIndex = url.length() - 1;
    //    url = url.substring(0, lastIndex);
    //}
    //self.config = c;
    //var cbConfig = c.circuitBreaker;
    //if (cbConfig is CircuitBreakerConfig) {
    //    if (url.hasSuffix("/")) {
    //        int lastIndex = url.length() -1;
    //        url = url.substring(0, lastIndex);
    //    }
    //    httpClientRequired = false;
    //} else {
    //    httpClientRequired = true;
    //}
    //
    //if (httpClientRequired) {
    //    var redirectConfigVal = c.followRedirects;
    //    if (redirectConfigVal is FollowRedirects) {
    //        self.httpClient = createRedirectClient(url, c);
    //    } else {
    //        self.httpClient = checkForRetry(url, c);
    //    }
    //} else {
    //    self.httpClient = createCircuitBreakerClient(url, c);
    //}
}

function createRedirectClient(string url, ClientEndpointConfig configuration) returns Client|error {
    var redirectConfig = configuration.followRedirects;
    if (redirectConfig is FollowRedirects) {
        if (redirectConfig.enabled) {
            var retryClient = createRetryClient(url, configuration);
            if (retryClient is Client) {
                return <Client>new RedirectClient(url, configuration, redirectConfig, retryClient);
            } else {
                return retryClient;
            }
        } else {
            return createRetryClient(url, configuration);
        }
    } else {
        return createRetryClient(url, configuration);
    }
}

function checkForRetry(string url, ClientEndpointConfig config) returns Client|error {
    var retryConfigVal = config.retryConfig;
    if (retryConfigVal is RetryConfig) {
        return createRetryClient(url, config);
    } else {
        if (config.cache.enabled) {
            return createHttpCachingClient(url, config, config.cache);
        } else {
            return createHttpSecureClient(url, config);
        }
    }
}

function createCircuitBreakerClient(string uri, ClientEndpointConfig configuration) returns Client|error {
    Client cbHttpClient = new(configuration);
    var cbConfig = configuration.circuitBreaker;
    if (cbConfig is CircuitBreakerConfig) {
        validateCircuitBreakerConfiguration(cbConfig);
        boolean [] statusCodes = populateErrorCodeIndex(cbConfig.statusCodes);
        var redirectConfig = configuration.followRedirects;
        if (redirectConfig is FollowRedirects) {
            var redirectClient = createRedirectClient(uri, configuration);
            if (redirectClient is Client) {
                cbHttpClient = redirectClient;
            } else {
                return redirectClient;
            }
        } else {
            var retryClient = checkForRetry(uri, configuration);
            if (retryClient is Client) {
                cbHttpClient = retryClient;
            } else {
                return retryClient;
            }
        }

        time:Time circuitStartTime = time:currentTime();
        int numberOfBuckets = (cbConfig.rollingWindow.timeWindowMillis/ cbConfig.rollingWindow.bucketSizeMillis);
        Bucket[] bucketArray = [];
        int bucketIndex = 0;
        while (bucketIndex < numberOfBuckets) {
            bucketArray[bucketIndex] = {};
            bucketIndex = bucketIndex + 1;
        }

        CircuitBreakerInferredConfig circuitBreakerInferredConfig = {
                                                            failureThreshold:cbConfig.failureThreshold,
                                                            resetTimeMillis:cbConfig.resetTimeMillis,
                                                            statusCodes:statusCodes,
                                                            noOfBuckets:numberOfBuckets,
                                                            rollingWindow:cbConfig.rollingWindow
                                                        };
        CircuitHealth circuitHealth = {
                                        startTime:circuitStartTime,
                                        lastRequestTime:circuitStartTime,
                                        lastErrorTime:circuitStartTime,
                                        lastForcedOpenTime:circuitStartTime,
                                        totalBuckets: bucketArray
                                      };
        return <Client>(new CircuitBreakerClient(uri, configuration,
            circuitBreakerInferredConfig, cbHttpClient, circuitHealth));
    } else {
        //remove following once we can ignore
        if (configuration.cache.enabled) {
            return createHttpCachingClient(uri, configuration, configuration.cache);
        } else {
            return createHttpSecureClient(uri, configuration);
        }
    }
}

function createRetryClient(string url, ClientEndpointConfig configuration) returns Client|error {
    var retryConfig = configuration.retryConfig;
    if (retryConfig is RetryConfig) {
        boolean[] statusCodes = populateErrorCodeIndex(retryConfig.statusCodes);
        RetryInferredConfig retryInferredConfig = {
            count: retryConfig.count,
            interval: retryConfig.interval,
            backOffFactor: retryConfig.backOffFactor,
            maxWaitInterval: retryConfig.maxWaitInterval,
            statusCodes: statusCodes
        };
        if (configuration.cache.enabled) {
            var httpCachingClient = createHttpCachingClient(url, configuration, configuration.cache);
            if (httpCachingClient is Client) {
                return <Client>new RetryClient(url, configuration, retryInferredConfig, httpCachingClient);
            } else {
                return httpCachingClient;
            }
        } else{
            var httpSecureClient = createHttpSecureClient(url, configuration);
            if (httpSecureClient is Client) {
                return <Client>new RetryClient(url, configuration, retryInferredConfig, httpSecureClient);
            } else {
                return httpSecureClient;
            }
        }
    } else {
        //remove following once we can ignore
        if (configuration.cache.enabled) {
            return createHttpCachingClient(url, configuration, configuration.cache);
        } else {
            return createHttpSecureClient(url, configuration);
        }
    }
}

//Since the struct equivalency doesn't work with private keyword, following functions are defined outside the object
extern function nativePost(ClientEndpointConfig config, @sensitive string path, Request req) returns Response|error;

extern function nativeHead(ClientEndpointConfig config, @sensitive string path, Request req) returns Response|error;

extern function nativePut(ClientEndpointConfig config, @sensitive string path, Request req) returns Response|error;

extern function nativeExecute(ClientEndpointConfig config, @sensitive string httpVerb, @sensitive string path,
                                                                        Request req) returns Response|error;

extern function nativePatch(ClientEndpointConfig config, @sensitive string path, Request req) returns Response|error;

extern function nativeDelete(ClientEndpointConfig config, @sensitive string path, Request req) returns Response|error;

extern function nativeGet(ClientEndpointConfig config, @sensitive string path, Request req) returns Response|error;

extern function nativeOptions(ClientEndpointConfig config, @sensitive string path, Request req) returns Response|error;

extern function nativeSubmit(ClientEndpointConfig config, @sensitive string httpVerb, string path, Request req)
                                                                        returns HttpFuture|error;
