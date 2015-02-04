package software.sham.http.matchers

import software.sham.http.ClientRequest

class HttpRequestMatcher {

    private Closure matcher

    public HttpRequestMatcher(Closure closure) {
        this.matcher = closure
    }

    boolean matches(ClientRequest request) {
        matcher(request)
    }

    HttpRequestMatcher and(HttpRequestMatcher otherMatcher) {
        def left = this
        def right = otherMatcher
        new HttpRequestMatcher({ ClientRequest request ->
            left.matches(request) && right.matches(request)
        })
    }
}
