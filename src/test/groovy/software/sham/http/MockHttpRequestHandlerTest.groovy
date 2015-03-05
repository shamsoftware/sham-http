package software.sham.http

import groovy.util.logging.Slf4j
import org.junit.Before
import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

import static software.sham.http.matchers.HttpMatchers.*

@Slf4j
class MockHttpRequestHandlerTest {
    MockHttpRequestHandler handler

    @Before
    void createHandler() {
        handler = new MockHttpRequestHandler()
    }

    @Test
    void shouldHandleConcurrentRequestsWhileWaitingForLatch() {
        def uri = "/test/concurrent"
        def requestCount = 1000
        def threads = []
        def errors = []
        def matcher = anyRequest()
        handler.matchers << matcher
        requestCount.times {
            threads << Thread.start {
                def request = new MockHttpServletRequest()
                def response = new MockHttpServletResponse()
                try {
                    handler.handle(uri, null, request, response)
                } catch (e) {
                    log.debug "Captured error $e"
                    errors << e
                }
            }

        }

        assert handler.waitFor(matcher, requestCount, 10000)
    }
}
