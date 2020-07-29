package feign;

import feign.template.UriTemplate;

import java.nio.charset.Charset;

class UriHelper {

    private UriTemplate uriTemplate;

    public UriHelper(UriTemplate uriTemplate) {
        this.uriTemplate = uriTemplate;
    }


    public UriTemplate resolveUri(String uri, boolean append, RequestHeaderTemplate requestHeaderTemplate, Charset charset, CollectionFormat collectionFormat) {
        String requestUri = requestHeaderTemplate.makeRequestUri(uri, append, charset, collectionFormat);
        /* replace the uri template */
        if (append && this.uriTemplate != null) {
            this.uriTemplate = UriTemplate.append(this.uriTemplate, requestUri);
        } else {
            this.uriTemplate = UriTemplate.create(requestUri, !requestHeaderTemplate.decodeSlash(), charset);
        }
        return uriTemplate;
    }

    public UriTemplate generateTemplate(RequestHeaderTemplate requestHeaderTemplate, Charset charset) {
        if (this.uriTemplate == null) {
            /* create a new uri template using the default root */
            return UriTemplate.create("", !requestHeaderTemplate.decodeSlash(), charset);
        }

        return uriTemplate;
    }
}
