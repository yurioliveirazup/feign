package feign;

import feign.template.*;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static feign.Util.CONTENT_LENGTH;
import static java.util.Objects.isNull;

class RequestHeaderTemplate {

    private static final Pattern QUERY_STRING_PATTERN = Pattern.compile("(?<!\\{)\\?");

    private Map<String, QueryTemplate> queries = new LinkedHashMap<>();
    private Map<String, HeaderTemplate> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private String target;
    private String fragment;
    private Request.HttpMethod method;
    private boolean decodeSlash = true;

    public RequestHeaderTemplate(String target, String fragment, Request.HttpMethod method, boolean decodeSlash) {
        this.target = target;
        this.fragment = fragment;
        this.method = method;
        this.decodeSlash = decodeSlash;
    }

    public RequestHeaderTemplate() { }

    public String getTarget() {
        return target;
    }

    public String getFragment() {
        return fragment;
    }

    public Request.HttpMethod getMethod() {
        return method;
    }

    public boolean decodeSlash() {
        return this.decodeSlash;
    }

    public void setDecodeSlash(boolean decodeSlash) {
        this.decodeSlash = decodeSlash;
    }

    public Map<String, QueryTemplate> getQueries() {
        return queries;
    }

    public Map<String, HeaderTemplate> getHeaders() {
        return headers;
    }

    public void setMethod(String method) {
        try {
            this.method = Request.HttpMethod.valueOf(method);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid HTTP Method: " + method);
        }
    }

    public void setMethod(Request.HttpMethod method) {
        this.method = method;
    }

    public void updateQueries(Map<String, QueryTemplate> queries) {
        if (!this.queries.isEmpty()) {
            this.queries.putAll(queries);
        }
    }

    public Map<String, Collection<String>> queries(Charset charset) {
        Map<String, Collection<String>> queryMap = new QueryParametersHelper(queries, charset, decodeSlash).getQueryMap();

        return Collections.unmodifiableMap(queryMap);
    }

    public void updateHeaders(Map<String, HeaderTemplate> headers) {
        if (!headers.isEmpty()) {
            this.headers.putAll(headers);
        } else {
            this.headers.clear();
        }
    }

    public Map<String, Collection<String>> headers() {
        Map<String, Collection<String>> headerMap = new HeadersHelper(headers).getHeadersMap();

        return Collections.unmodifiableMap(headerMap);
    }

    public String method() {
        if (isNull(method)) {
            return null;
        }

        return method.name();
    }

    public UriTemplate generateUriTemplate(boolean decodeSlash, Charset charset, CollectionFormat collectionFormat, UriTemplate uriTemplate) {
        this.setDecodeSlash(decodeSlash);
        this.updateQueries(new QueryParametersHelper(queries, charset, decodeSlash).replaceQueryTemplate(collectionFormat));
        return UriTemplate.create(uriTemplate.toString(), !this.decodeSlash(), charset);
    }

    public String requestUrl(UriTemplate uriTemplate) {

        /* build the fully qualified url with all query parameters */
        StringBuilder url = new StringBuilder(this.path(uriTemplate));
        if (!this.queries.isEmpty()) {
            url.append(queryString());
        }
        if (fragment != null) {
            url.append(fragment);
        }

        return url.toString();
    }

    public String path(UriTemplate uriTemplate) {
        return new PathFactory(target, uriTemplate).makePath();
    }

    public String queryString() {
        return QueryStringUtils.getQueryStrings(queries);
    }

    public List<String> findVariablesIn(UriTemplate uriTemplate, BodyTemplate bodyTemplate) {
        return new VariablesUtils(queries, uriTemplate, headers, bodyTemplate).findAllVariables();
    }

    public void addQueryParametersToRequest(Charset charset, String name, Iterable<String> values, CollectionFormat collectionFormat) {
        this.queries = new QueryParametersHelper(queries, charset, decodeSlash).makeQueryParametesToRequest(name, values, collectionFormat);
    }

    public void updateQueryString(Charset charset, Map<String, Collection<String>> queries, CollectionFormat collectionFormat) {
        if (queries == null || queries.isEmpty()) {
            this.queries.clear();
        } else {
            queries.forEach((String name, Iterable<String> values) -> addQueryParametersToRequest(charset, name, values, collectionFormat));
        }
    }

    private void appendQueryString() {

    }

    public void addHeaderUsingChunks(String name, TemplateChunk... chunks) {
        this.headers = new HeadersHelper(headers).makeHeaderTemplate(name, chunks);
    }

    public void updateHeader(String name, Iterable<String> values) {
        this.headers = new HeadersHelper(headers).makeHeaderTemplate(name, values);
    }

    public void removeHeader(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is required.");
        }
        this.headers.remove(name);
    }

    public void updateHeadersForRequest(Map<String, Collection<String>> headers) {
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(this::updateHeader);
        } else {
            this.headers.clear();
        }
    }

    public void setContentLengthTo(Request.Body body) {
        updateHeader(CONTENT_LENGTH, Collections.emptyList());
        if (body.length() > 0) {
            updateHeader(CONTENT_LENGTH, Collections.singletonList(String.valueOf(body.length())));
        }
    }

    public String makeRequestUri(String uri, boolean append, Charset charset, CollectionFormat collectionFormat) {
        String requestUri = new UriRequestHelper().makeUri(uri);
        /*
         * templates may provide query parameters. since we want to manage those explicity, we will need
         * to extract those out, leaving the uriTemplate with only the path to deal with.
         */
        Matcher queryMatcher = QUERY_STRING_PATTERN.matcher(requestUri);
        if (queryMatcher.find()) {
            String queryString = uri.substring(queryMatcher.start() + 1);

            /* parse the query string */
            extractQueryTemplates(queryString, append, charset, collectionFormat);

            /* reduce the uri to the path */
            requestUri = requestUri.substring(0, queryMatcher.start());
        }

        int fragmentIndex = requestUri.indexOf('#');
        if (fragmentIndex > -1) {
            fragment = uri.substring(fragmentIndex);
            requestUri = requestUri.substring(0, fragmentIndex);
        }
        return requestUri;
    }

    private void extractQueryTemplates(String queryString, boolean append, Charset charset, CollectionFormat collectionFormat) {
        /* split the query string up into name value pairs */
        Map<String, List<String>> queryParameters = QueryStringUtils.extractQueryParameters(queryString);

        /* add them to this template */
        if (!append) {
            /* clear the queries and use the new ones */
            this.queries.clear();
        }
        queryParameters.forEach((String name, Iterable<String> values) -> addQueryParametersToRequest(charset, name, values, collectionFormat));
    }

    public void target(String target, Charset charset, CollectionFormat collectionFormat) {
        /* target can be empty */
        if (Util.isBlank(target)) {
            return;
        }

        /* verify that the target contains the scheme, host and port */
        if (!UriUtils.isAbsolute(target)) {
            throw new IllegalArgumentException("target values must be absolute.");
        }
        if (target.endsWith("/")) {
            target = target.substring(0, target.length() - 1);
        }
        try {
            /* parse the target */
            URI targetUri = URI.create(target);

            if (Util.isNotBlank(targetUri.getRawQuery())) {
                /*
                 * target has a query string, we need to make sure that they are recorded as queries
                 */
                this.extractQueryTemplates(targetUri.getRawQuery(), true, charset, collectionFormat);
            }

            /* strip the query string */
            this.target = targetUri.getScheme() + "://" + targetUri.getAuthority() + targetUri.getPath();
            if (targetUri.getFragment() != null) {
                this.fragment = "#" + targetUri.getFragment();
            }
        } catch (IllegalArgumentException iae) {
            /* the uri provided is not a valid one, we can't continue */
            throw new IllegalArgumentException("Target is not a valid URI.", iae);
        }
    }

    public String resolveUri(Map<String, ?> variables, String expanded) {
        QueryParamsResolver queryParamsResolver = new QueryParamsResolver(QUERY_STRING_PATTERN, queries, variables);
        StringBuilder uri = queryParamsResolver.resolve(this, expanded);

        return uri.toString();
    }

    public Map<String, Collection<String>> resolveHeaders(Map<String, ?> variables) {
        /* headers */
        if (!this.headers.isEmpty()) {
            /*
             * same as the query string, we only want to keep resolved values, so clear the header map on
             * the resolved instance
             */
            HashMap<String, HeaderTemplate> oldHeaders = new HashMap<>(this.headers);
            updateHeaders(Collections.emptyMap());
            for (HeaderTemplate headerTemplate : oldHeaders.values()) {
                /* resolve the header */
                String header = headerTemplate.expand(variables);
                if (!header.isEmpty()) {
                    /* split off the header values and add it to the resolved template */
                    String headerValues = header.substring(header.indexOf(" ") + 1);
                    if (!headerValues.isEmpty()) {
                        /* append the header as a new literal as the value has already been expanded. */
                        addHeaderUsingChunks(headerTemplate.getName(), Literal.create(headerValues));
                    }
                }
            }
        }

        return headers();
    }
}
