package feign;

import feign.template.UriUtils;

class UriRequestHelper {

    public String makeUri(String uri) {
        /* validate and ensure that the url is always a relative one */
        if (UriUtils.isAbsolute(uri)) {
            throw new IllegalArgumentException("url values must be not be absolute.");
        }

        if (uri == null) {
            uri = "/";
        } else if ((!uri.isEmpty() && !uri.startsWith("/") && !uri.startsWith("{")
                && !uri.startsWith("?") && !uri.startsWith(";"))) {
            /* if the start of the url is a literal, it must begin with a slash. */
            uri = "/" + uri;
        }

        return uri;
    }
}
