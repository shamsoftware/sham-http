package software.sham.http

import org.junit.Before
import org.junit.Test

import javax.ws.rs.client.Client

import static software.sham.http.matchers.HttpMatchers.*

class MockHttpsServerFunctionalTest {

    Client sslClient

    @Before
    void initSslClient() {
        ClientConfig config = new DefaultClientConfig()
        config.properties.put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, MockHttpsServer.clientSslContext))
        sslClient = Client.create(config)
    }

    @Test
    void sameFeaturesShouldWorkWithSsl() {
        def server = new MockHttpsServer()
        server.respondTo(path('/bar')).withStatus(302) // shouldn't get hit
        server.respondTo(path('/foo').and(queryParam('bar', 'baz'))).withBody('success')


        ClientResponse response = sslClient.resource("https://localhost:${server.port}/foo?bar=baz").get(ClientResponse.class)

        server.stop()

        assert 200 == response.status
        assert 'success' == response.getEntity(String.class)
    }
}
