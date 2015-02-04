package software.sham.http.expectations

import software.sham.http.ClientRequest

interface Expectation {
    /**
     * Inspect the request and decide if it is valid
     *
     * @param request captured resutls from http request
     * @return true if request is valid
     */
    Boolean verify(ClientRequest request)

}
