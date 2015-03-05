package software.sham.http

import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientResponse
import org.junit.Before
import org.junit.Test

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response

import static software.sham.http.matchers.HttpMatchers.*

class MockHttpsServerFunctionalTest {

    Client sslClient

    @Before
    void initSslClient() {
        sslClient = ClientBuilder.newBuilder()
                .sslContext(MockHttpsServer.clientSslContext)
                .build()
    }

    @Test
    void sameFeaturesShouldWorkWithSsl() {
        def server = new MockHttpsServer()
        server.respondTo(path('/bar')).withStatus(302) // shouldn't get hit
        server.respondTo(path('/foo').and(queryParam('bar', 'baz'))).withBody('success')


        Response response = sslClient.target("https://localhost:${server.port}/foo?bar=baz").request().get(Response.class)

        server.stop()

        assert 200 == response.status
        assert 'success' == response.readEntity(String.class)
    }
}
