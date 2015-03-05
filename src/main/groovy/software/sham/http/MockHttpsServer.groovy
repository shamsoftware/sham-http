package software.sham.http

import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.springframework.core.io.ClassPathResource

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore

class MockHttpsServer extends MockHttpServer {
    MockHttpsServer() {
        super()
    }

    MockHttpsServer(int port) {
        super(port)
    }

    @Override
    protected Server initJettyServer(final int port) {
        def server = new Server()
        def connector = new ServerConnector(server, createSslContextFactory(), createConnectionFactory(port))
        connector.port = port
        connector.idleTimeout = 120000
        server.addConnector(connector)
        return server
    }

    private SslContextFactory createSslContextFactory() {
        SslContextFactory factory = new SslContextFactory()
        factory.keyStoreResource = Resource.newClassPathResource('sham-mock.keystore')
        factory.keyStorePassword = 'password'
        factory.keyManagerPassword = 'password'
        factory.trustStoreResource = Resource.newClassPathResource('sham-mock.truststore')
        factory.trustStorePassword = 'password'
        return factory
    }

    private HttpConnectionFactory createConnectionFactory(final int port) {
        def config = new HttpConfiguration()
        config.setSecurePort(port)
        config.addCustomizer(new SecureRequestCustomizer())
        return new HttpConnectionFactory(config)
    }

    static SSLContext getClientSslContext() {
        SSLContext sslContext = SSLContext.getInstance('SSL')

        sslContext.init(null, getTrustManagerFactory().getTrustManagers(), null)
        return sslContext
    }

    static TrustManagerFactory getTrustManagerFactory() {
        KeyStore truststore = KeyStore.getInstance('JKS')
        truststore.load(new ClassPathResource('sham-mock.keystore').inputStream, 'password'.toCharArray())
        TrustManagerFactory tmf = TrustManagerFactory.getInstance('SunX509')
        tmf.init(truststore)
        return tmf
    }
}
