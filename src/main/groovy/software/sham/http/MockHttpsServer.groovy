package software.sham.http

import software.sham.http.jetty.MockSslSocketConnector
import org.mortbay.jetty.Server
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
        new Server() {{
            addConnector(new MockSslSocketConnector() {{
                setKeystore('sham-mock.keystore')
                setTruststore('sham-mock.truststore')
                setPassword('password')
                setKeyPassword('password')
                setTrustPassword('password')
                setMaxIdleTime(120000)
                setPort(port)
            }});
        }}
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
