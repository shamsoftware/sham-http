package software.sham.http.expectations

import software.sham.http.ClientRequest
import groovy.transform.ToString

@ToString(includeNames = true)
class MethodExpectation implements Expectation {


    public static final MethodExpectation GET = new MethodExpectation("GET")
    public static final MethodExpectation PUT = new MethodExpectation("PUT")
    public static final MethodExpectation DELETE = new MethodExpectation("DELETE")
    public static final MethodExpectation POST = new MethodExpectation("POST")

    String method


    MethodExpectation(String method) {
        this.method = method.toUpperCase()
    }

    Boolean verify(ClientRequest request) {
        return request.method?.toUpperCase() == method
    }

}
