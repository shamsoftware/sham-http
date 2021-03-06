

# sham.software Mock HTTP API

Mock HTTP testing library for stubbing HTTP responses from text or resources on the classpath (xml files, etc.) and
verification of client behavior and state.

This library is still under development. Feel free to use, contribute but there could be changes to
the API until it reaches 1.0 status. Of course, we'll try to keep breaking changes to a minimum.

**Table of Contents**

* [Example Usage] (#example-usage)
* [Maven Information] (#maven-information)
* [License] (#License)

**Groovy Examples**

Most of the examples documented here are using Groovy instead of Java. Feel free to use Java if you wish. There is
no Groovy requirement (Groovy is great and you should really check it out though!).

## Getting it on your classpath

### Maven

```xml
<dependency>
  <groupId>software.sham</groupId>
  <artifactId>sham-http</artifactId>
  <version>1.0.0</version>
</dependency>
```

The artifacts are available in the [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22sham-http%22).

### Manual

If you need to add this to your project manually, you can download it directly from the maven central repository:

[Download Jar](http://search.maven.org/remotecontent?filepath=software/sham/sham-http/1.0.0/sham-http-1.0.0.jar)


## Example Usage

### Useful Imports

To achieve a fluid, readable API, [Hamcrest matchers](http://hamcrest.org/) are used. There are a few static imports
which you'll likely want to use to maintain readability in your tests:

```groovy

import static javax.servlet.http.HttpServletResponse.*
import static software.sham.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

```


### Hello World

The simplest usage of this library is to create a MockHttpServer, and set it up to respond the same regardless of
what requests it receives.  Once you have instantiated the MockHttpServer, it is listening on localhost on the port
you provided.

```groovy

import software.sham.http.MockHttpServer
import static software.sham.http.matchers.HttpMatchers.*

MockHttpServer server = new MockHttpServer(8080)
server.respondTo(anyRequest()).withBody('Hello World!')

// ... clients can now connect to port 8080 and send HTTP requests.

server.stop()

```

### Handling different methods and paths

You can instruct the MockHttpServer to respond to specific paths.  When a requests does not match anything, the
server responds with a 404 status code.  When you use respondTo to match HTTP requests, the server responds with status
code 200 and an empty response body unless you instruct it to do otherwise.

```groovy

MockHttpServer server = new MockHttpServer()
server.respondTo(get('/about-us')).withBody('We are awesome')
server.respondTo(post('/blog/create-post.php')).withStatus(201).withBody('Created')

```

### Matching other request details

You can also match on the request body, and combine matchers using "and"

```groovy

server.respondTo(path('/profile').and(body('name=Ryan'))).withStatus('401')
server.respondTo(path('/query').and(queryParam('q', 'cartoon+robot'))

```

You can also use [Hamcrest Matchers] (http://code.google.com/p/hamcrest/wiki/Tutorial) if you need more control.

```groovy

import static org.hamcrest.Matchers.*

server.respondTo(
    path(startsWith('/cgi-bin'))
    .and(body(containsString('ccNumber')))
).withStatus('302')
.withHeader('Location', 'http://phishing-site.com')

```

### Matcher Priority

Each time you call server.respondTo, your matcher takes priority over previous matchers.  This means you should configure
more general matchers first, and follow on with more specific ones

```groovy

server.respondTo(get()).withStatus(404).withBody("huh?")
server.respondTo(get().and(header("Accept", "text/xml")).withStatus(404).withBody("<huh />")
server.respondTo(get("/images")).withHeader("Content-Type", "text/html").withResource("/images/index.html")
server.respondTo(get("/images/logo.png")).withHeader("Content-Type", "image/png").withResource("/images/logo.png")

```

### Doing more with the response

You can control the response status, body, and headers

```groovy

server.respondTo(path('/not-found'))
    .withStatus(404)
    .withBody('Sorry, buddy')
    .withHeader('Content-Type', 'text/plain')
    .withHeader('Last-Modified', 'Tue, 15 Jan 2017 12:45:26 GMT')

```

### Dynamic responses

Your response can use information from the request to generate the response body

``` groovy

server.respondTo(path('query.cgi'))
    .withBody() { ClientRequest request ->
        return doSomethingComplicatedWith(request.getBody())
    }

```

### Asserting on request information

In order to make sure the HTTP client sent the requests you expected, you can find out what requests the server received.

```groovy

assertEquals(3, server.requests.size())

ClientRequest request = server.requests.find() { it.method == 'GET' && it.path == '/widget/inventory' }

assertNotNull(request)
assertEquals('application/json', request.header['Content-Type'])

```

### Waiting for requests to be received


You can also delay some processing until after a request has been received.

```groovy

system.startLongProcessThatCallsMyWebService()

server.waitFor(anyRequest())

system.startOtherProcessThatCallsSeveralWebServices()

boolean completed = server.waitFor(path('/step/3'), 1000) // timeout in milliseconds

```

### Finding an available port

If you don't particularly care what port the HTTP server listens to, you can allow it to find an available port.

```groovy

MockHttpServer server = new MockHttpServer()
int thePortItChose = server.port

```

### Supporting SSL
To mock an HTTPS server, use MockHttpsServer in the place of MockHttpServer.

```groovy

MockHttpsServer server = new MockHttpsServer(443)

```

Your HTTPS client will need to use a keystore that contains the mock server's certificate.  
sham-mock.keystore and sham-mock.truststore is available on the classpath, and its password 
is password.

The commands used to generate the key, certificate, and stores is as follows:

```
openssl genrsa 2048 > sham-mock.key
openssl genrsa -des3 -out sham-mock.key 2048

openssl req -new -x509 -nodes -sha1 -subj '/O=Sham/CN=localhost' -days 3650 -key sham-mock.key > sham-mock.crt

openssl pkcs12 -inkey sham-mock.key -in sham-mock.crt -export -out cert-key.pkcs12
keytool -importkeystore -srckeystore cert-key.pkcs12 -srcstoretype pkcs12 -destkeystore sham-mock.keystore

keytool -import -alias 1 -file sham-mock.crt -keystore sham-mock.truststore
```

# License

   Copyright 2015 Ryan Hoegg.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
