package software.sham.http

import org.junit.After
import org.junit.Before
import org.junit.Test

import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.awt.PageAttributes

import static javax.servlet.http.HttpServletResponse.*
import static software.sham.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

import static org.junit.Assert.*

class MockHttpServerFunctionalTest {
    private MockHttpServer server

    @Before
    void initServer() {
        server = new MockHttpServer()
    }

    @After
    void stopServer() {
        server.stop()
    }

    @Test
    void newServerShouldBeListening() {
        Response response = ClientBuilder.newClient().target("http://localhost:${server.port}/").request().get(Response.class)
        assert 404 == response.status
    }

    @Test
    void newServerShouldListenOnSpecifiedPort() {
        server = new MockHttpServer(8123)
        Response response = ClientBuilder.newClient().target("http://localhost:8123/").request().get(Response.class)
        assert 404 == response.status
    }

    @Test
    void stopShouldStopListeningOnPort() {
        server.stop()
        try {
            ClientBuilder.newClient().target("http://localhost:${server.port}/").request().get(Response.class)
            fail("Unexpected listener on port ${server.port} after server.stop()")
        } catch (ProcessingException e) {
            if (! e.cause instanceof ConnectException) {
                throw e
            }
        }
    }

    @Test
    void shouldRespond404WhenCreated() {
        Response response = ClientBuilder.newClient().target("http://localhost:${server.port}/").request().get(Response.class)
        assert 404 == response.status
        assert "Resource not found" == response.readEntity(String)
        assert MediaType.TEXT_PLAIN_TYPE == response.getMediaType()
    }

    @Test
    void shouldRespondOkWithEmptyBodyByDefault() {
        server.respondTo(anyRequest())
        Response response = ClientBuilder.newClient().target("http://localhost:${server.port}/").request().get(Response.class)
        assert SC_OK == response.status
        assert "" == response.readEntity(String.class)
    }

    @Test
    void differentPathsShouldRespondDifferently() {
        server.respondTo(path('/1')).withBody('one')
        server.respondTo(path('/2')).withResource('/http/responses/two.txt')

        String responseOne = ClientBuilder.newClient().target("http://localhost:${server.port}/1").request().get(String.class)
        String responseTwo = ClientBuilder.newClient().target("http://localhost:${server.port}/2").request().get(String.class)

        assert 'one' == responseOne
        assert 'two' == responseTwo
    }

    @Test
    void matchersShouldSupercedePriorMatchers() {
        server.respondTo(get(startsWith('/washington'))).withBody('State')
        server.respondTo(get(startsWith('/washington/seattle'))).withBody('City')
        server.respondTo(get(startsWith('/washington/seattle/acme'))).withBody('Company')

        def resource =  ClientBuilder.newClient().target("http://localhost:${server.port}/washington")

        assert 'Company' == resource.path('/seattle/acme').request().get(String.class)
        assert 'City' == resource.path('/seattle').request().get(String.class)
        assert 'State' == resource.request().get(String.class)
    }

    @Test
    void differentMethodsShouldRespondDifferently() {
        server.respondTo(get('/restfulResource')).withBody('GET')
        server.respondTo(put(startsWith('/restfulResource/'))).withBody('PUT')
        server.respondTo(post('/restfulResource')).withBody('POST')
        server.respondTo(delete(startsWith('/restfulResource/'))).withBody('DELETE')
        server.respondTo(method('OPTIONS').and(path('/restfulResource'))).withBody('OPTIONS')

        def resource = ClientBuilder.newClient().target("http://localhost:${server.port}/restfulResource")

        assert 'GET' == resource.request().get(String.class)
        assert 'PUT' == resource.path('/1').request().put(Entity.entity('', MediaType.TEXT_PLAIN_TYPE), String.class)
        assert 'POST' == resource.request().post(Entity.entity('', MediaType.TEXT_PLAIN_TYPE), String.class)
        assert 'DELETE' == resource.path('/1').request().delete(String.class)
        assert 'OPTIONS' == resource.request().method('OPTIONS', String.class)
    }

    @Test
    void shouldAcceptClosureForBody() {
        server.respondTo(path('/')).withBody { ClientRequest request ->
            request.queryParams['chipmunk']
        }

        assert 'simon' == ClientBuilder.newClient().target("http://localhost:${server.port}/")
                .queryParam('chipmunk', 'simon')
                .request()
                .get(String)
        assert 'theodore' == ClientBuilder.newClient().target("http://localhost:${server.port}/")
                .queryParam('chipmunk', 'theodore')
                .request()
                .get(String)
    }

    @Test
    void shouldCaptureRequestInformation() {
        ClientBuilder.newClient().target("http://localhost:${server.port}/cool-api/")
                .request()
                .header('Accept', 'application/json')
                .post(Entity.entity('{"foo": "bar"}', MediaType.APPLICATION_JSON_TYPE), Response.class)
        ClientBuilder.newClient().target("http://localhost:${server.port}/wicked-api/search?query=foo")
                .request()
                .header('Accept', 'application/html')
                .header('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:22.0) Gecko/20100101 Firefox/22.0')
                .get(Response.class)

        assert 2 == server.requests.size()
        assert 1 == server.requests.findAll { it.method == 'GET' }.size()
        assert server.requests.find { it.url =~ /wicked/ }.headers['User-Agent'] =~ 'Macintosh'
        assert server.requests.find { it.headers['Accept'] == 'application/json'}.method == 'POST'

// TODO: make java programmers' lives easier - use the same HttpMatchers we use to set up the server and to waitFor things
//        assert server.receivedRequest(path('/cool-api/'))
//        assert server.receivedRequest(header('Accept', 'application/html'))
//        assert server.receivedRequest(header('User-Agent', matching('Mozilla.*')))
//        assert server.receivedRequest(path(matching('wicked.*')))
    }

    @Test
    void waitForShouldBlockUntilRequestCompleted() {
        def finished = false
        Thread.start {
            server.waitFor(path('/go'), 1000)
            finished = true
        }

        assert ! finished
        ClientBuilder.newClient().target("http://localhost:${server.port}/ready").request().get(Response.class)
        Thread.sleep(100)
        assert ! finished
        ClientBuilder.newClient().target("http://localhost:${server.port}/steady").request().get(Response.class)
        Thread.sleep(100)
        assert ! finished
        ClientBuilder.newClient().target("http://localhost:${server.port}/go").request().get(Response.class)
        Thread.sleep(100)
        assert finished
    }

    @Test
    void waitForShouldSupportMultipleRequestsAndConsiderPriorRequests() {
        def areWeThereYet = { ClientBuilder.newClient().target("http://localhost:${server.port}/mom").request() }

        2.times {
            areWeThereYet().post(Entity.entity('Are we there yet?', MediaType.TEXT_PLAIN_TYPE), Response.class)
        }

        def enoughAlready = false

        Thread.start {
            enoughAlready = server.waitFor(path('/mom').and(body('Are we there yet?')), 10, 2000)
        }

        7.times {
            areWeThereYet().post(Entity.entity('Are we there yet?', MediaType.TEXT_PLAIN_TYPE), Response.class)
            Thread.sleep(100)
            assert ! enoughAlready
        }

        areWeThereYet().post(Entity.entity('Are we there yet?', MediaType.TEXT_PLAIN_TYPE), Response.class)
        Thread.sleep(100)
        assert enoughAlready
    }
}
