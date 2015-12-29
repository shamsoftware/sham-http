package software.sham.http

import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientResponse
import org.junit.After
import org.junit.Before
import org.junit.Test

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response

import static software.sham.http.matchers.HttpMatchers.*

class MockHttpsServerFunctionalTest {

    Client sslClient
    MockHttpsServer server

    @Before
    void initSslClient() {
        sslClient = ClientBuilder.newBuilder()
                .sslContext(MockHttpsServer.clientSslContext)
                .build()
    }

    @Before
    void initMockServer() {
        server = new MockHttpsServer()
    }

    @After
    void stopMockServer() {
        server.stop()
    }

    @Test
    void sameFeaturesShouldWorkWithSsl() {
        server.respondTo(path('/bar')).withStatus(302) // shouldn't get hit
        server.respondTo(path('/foo').and(queryParam('bar', 'baz'))).withBody('success')


        Response response = sslClient.target("https://localhost:${server.port}/foo?bar=baz").request().get(Response.class)

        assert 200 == response.status
        assert 'success' == response.readEntity(String.class)
    }
}
