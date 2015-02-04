package software.sham.http

import software.sham.http.event.MatchingEventLatch
import software.sham.http.matchers.HttpMatchers
import software.sham.http.matchers.HttpRequestMatcher
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.mortbay.jetty.handler.AbstractHandler

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.CopyOnWriteArrayList

@Slf4j
@ToString(includeNames = true, includes = "mappings, currentMapping")
class MockHttpRequestHandler extends AbstractHandler {
    List<HttpRequestMatcher> matchers = []
    Map<HttpRequestMatcher, HttpResponder> responders = [:]
    List<ClientRequest> requests = new CopyOnWriteArrayList<ClientRequest>()
    List<MatchingEventLatch> latches = []

    public MockHttpRequestHandler() {
        respondTo(HttpMatchers.anyRequest()).withStatus(404).withHeader('Content-Type', 'text/plain').withBody("Resource not found")
    }

    void handle(String uri, HttpServletRequest request, HttpServletResponse response, int dispatch) {
        def clientRequest = new ClientRequest(request)
        log.debug "Handling request $clientRequest"
        requests << clientRequest
        HttpRequestMatcher matcher = matchers.find { matcher ->
            matcher.matches(clientRequest)
        }
        responders[matcher]?.render(clientRequest, response)
        synchronized(latches) {
            latches.each {
                it.addEvent(clientRequest)
            }
        }
    }

    HttpResponderBuilder respondTo(HttpRequestMatcher matcher) {
        matchers.add 0, matcher
        HttpResponderBuilder builder = new HttpResponderBuilder()
        responders[matcher] = builder.responder
        return builder
    }

    boolean waitFor(HttpRequestMatcher matcher, int expected, long timeoutMs) {
        def latch
        synchronized(latches) {
            latch = new MatchingEventLatch(matcher, expected)
            requests.each { latch.addEvent(it) }
            latches << latch
        }
        latch.await(timeoutMs)
    }
}