package software.sham.http.expectations

import software.sham.http.ClientRequest
import groovy.transform.ToString

@ToString(includeNames = true)
class HeaderExpectation implements Expectation {

    String key
    String value

    HeaderExpectation(String key, String value) {
        this.key = key
        this.value = value
    }

    @Override
    Boolean verify(ClientRequest request) {
        return request.headers[key] == value
    }
}
