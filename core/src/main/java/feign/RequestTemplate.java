/**
 * Copyright 2012-2020 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package feign;

import feign.Request.HttpMethod;
import feign.template.BodyTemplate;
import feign.template.UriTemplate;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;

import static feign.Util.checkNotNull;

/**
 * Request Builder for an HTTP Target.
 * <p>
 * This class is a variation on a UriTemplate, where, in addition to the uri, Headers and Query
 * information also support template expressions.
 * </p>
 */
@SuppressWarnings("UnusedReturnValue")
public final class RequestTemplate implements Serializable {

  private RequestHeaderTemplate requestHeaderTemplate;
  private RequestBodyTemplate requestBodyTemplate;
  private boolean resolved = false;
  private UriTemplate uriTemplate;
  private transient Charset charset = Util.UTF_8;
  private CollectionFormat collectionFormat = CollectionFormat.EXPLODED;
  private MethodMetadata methodMetadata;
  private Target<?> feignTarget;

  /**
   * Create a new Request Template.
   */
  public RequestTemplate() {
    super();
    this.requestHeaderTemplate = new RequestHeaderTemplate();
    this.requestBodyTemplate = new RequestBodyTemplate();
  }

  /**
   * Create a new Request Template.
   *
   * @param fragment part of the request uri.
   * @param target for the template.
   * @param uriTemplate for the template.
   * @param bodyTemplate for the template, may be {@literal null}
   * @param method of the request.
   * @param charset for the request.
   * @param body of the request, may be {@literal null}
   * @param decodeSlash if the request uri should encode slash characters.
   * @param collectionFormat when expanding collection based variables.
   * @param feignTarget this template is targeted for.
   * @param methodMetadata containing a reference to the method this template is built from.
   */
  private RequestTemplate(String target,
      String fragment,
      UriTemplate uriTemplate,
      BodyTemplate bodyTemplate,
      HttpMethod method,
      Charset charset,
      Request.Body body,
      boolean decodeSlash,
      CollectionFormat collectionFormat,
      MethodMetadata methodMetadata,
      Target<?> feignTarget) {
    this.requestHeaderTemplate = new RequestHeaderTemplate(target, fragment, method, decodeSlash);
    this.requestBodyTemplate = new RequestBodyTemplate(bodyTemplate, body);
    this.uriTemplate = uriTemplate;
    this.charset = charset;
    this.collectionFormat = (collectionFormat != null) ? collectionFormat : CollectionFormat.EXPLODED;
    this.methodMetadata = methodMetadata;
    this.feignTarget = feignTarget;
  }

  /**
   * Create a Request Template from an existing Request Template.
   *
   * @param requestTemplate to copy from.
   * @return a new Request Template.
   */
  public static RequestTemplate from(RequestTemplate requestTemplate) {
    RequestTemplate template =
        new RequestTemplate(
            requestTemplate.requestHeaderTemplate.getTarget(),
            requestTemplate.requestHeaderTemplate.getFragment(),
            requestTemplate.uriTemplate,
            requestTemplate.requestBodyTemplate.getBodyTemplate(),
            requestTemplate.requestHeaderTemplate.getMethod(),
            requestTemplate.charset,
            requestTemplate.requestBodyTemplate.getBody(),
            requestTemplate.decodeSlash(),
            requestTemplate.collectionFormat,
            requestTemplate.methodMetadata,
            requestTemplate.feignTarget);

    template.requestHeaderTemplate.updateQueries(requestTemplate.requestHeaderTemplate.getQueries());
    template.requestHeaderTemplate.updateHeaders(requestTemplate.requestHeaderTemplate.getHeaders());

    return template;
  }

  /**
   * Create a Request Template from an existing Request Template.
   *
   * @param toCopy template.
   * @deprecated replaced by {@link RequestTemplate#from(RequestTemplate)}
   */
  @Deprecated
  public RequestTemplate(RequestTemplate toCopy) {
    checkNotNull(toCopy, "toCopy");
    this.requestHeaderTemplate = toCopy.requestHeaderTemplate;
    this.requestBodyTemplate = toCopy.requestBodyTemplate;
    this.charset = toCopy.charset;
    this.collectionFormat = (toCopy.collectionFormat != null) ? toCopy.collectionFormat : CollectionFormat.EXPLODED;
    this.uriTemplate = toCopy.uriTemplate;
    this.resolved = false;
    this.methodMetadata = toCopy.methodMetadata;
    this.feignTarget = toCopy.feignTarget;
  }

  /**
   * Resolve all expressions using the variable value substitutions provided. Variable values will
   * be pct-encoded, if they are not already.
   *
   * @param variables containing the variable values to use when resolving expressions.
   * @return a new Request Template with all of the variables resolved.
   */
  public RequestTemplate resolve(Map<String, ?> variables) {

    /* create a new template form this one, but explicitly */
    RequestTemplate resolved = RequestTemplate.from(this);

    this.uriTemplate = new UriHelper(uriTemplate).generateTemplate(requestHeaderTemplate, charset);

    String expanded = this.uriTemplate.expand(variables);
    String uri = requestHeaderTemplate.resolveUri(variables, expanded);
    /* add the uri to result */
    resolved.uri(uri);

    resolved.requestHeaderTemplate.resolveHeaders(variables);

    String resolvedBody = requestBodyTemplate.resolve(variables);
    resolved.body(resolvedBody);

    /* mark the new template resolved */
    resolved.resolved = true;
    return resolved;
  }

  /**
   * Resolves all expressions, using the variables provided. Values not present in the {@code
   * alreadyEncoded} map are pct-encoded.
   *
   * @param unencoded variable values to substitute.
   * @param alreadyEncoded variable names.
   * @return a resolved Request Template
   * @deprecated use {@link RequestTemplate#resolve(Map)}. Values already encoded are recognized as
   *             such and skipped.
   */
  @SuppressWarnings("unused")
  @Deprecated
  RequestTemplate resolve(Map<String, ?> unencoded, Map<String, Boolean> alreadyEncoded) {
    return this.resolve(unencoded);
  }

  /**
   * Creates a {@link Request} from this template. The template must be resolved before calling this
   * method, or an {@link IllegalStateException} will be thrown.
   *
   * @return a new Request instance.
   * @throws IllegalStateException if this template has not been resolved.
   */
  public Request request() {
    if (!this.resolved) {
      throw new IllegalStateException("template has not been resolved.");
    }
    return Request.create(requestHeaderTemplate.getMethod(), this.url(), this.headers(), requestBodyTemplate.getBody(), this);
  }

  /**
   * Set the Http Method.
   *
   * @param method to use.
   * @return a RequestTemplate for chaining.
   * @deprecated see {@link RequestTemplate#method(HttpMethod)}
   */
  @Deprecated
  public RequestTemplate method(String method) {
    checkNotNull(method, "method");

    requestHeaderTemplate.setMethod(method);
    return this;
  }

  /**
   * Set the Http Method.
   *
   * @param method to use.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate method(HttpMethod method) {
    checkNotNull(method, "method");
    requestHeaderTemplate.setMethod(method);
    return this;
  }

  /**
   * The Request Http Method.
   *
   * @return Http Method.
   */
  public String method() {
    return requestHeaderTemplate.method();
  }

  /**
   * Set whether do encode slash {@literal /} characters when resolving this template.
   *
   * @param decodeSlash if slash literals should not be encoded.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate decodeSlash(boolean decodeSlash) {
    this.uriTemplate = requestHeaderTemplate.generateUriTemplate(decodeSlash, charset, collectionFormat, uriTemplate);
    return this;
  }

  /**
   * If slash {@literal /} characters are not encoded when resolving.
   *
   * @return true if slash literals are not encoded, false otherwise.
   */
  public boolean decodeSlash() {
    return requestHeaderTemplate.decodeSlash();
  }

  /**
   * The Collection Format to use when resolving variables that represent {@link Iterable}s or
   * {@link Collection}s
   *
   * @param collectionFormat to use.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate collectionFormat(CollectionFormat collectionFormat) {
    this.collectionFormat = collectionFormat;
    return this;
  }

  /**
   * The Collection Format that will be used when resolving {@link Iterable} and {@link Collection}
   * variables.
   *
   * @return the collection format set
   */
  @SuppressWarnings("unused")
  public CollectionFormat collectionFormat() {
    return collectionFormat;
  }

  /**
   * Append the value to the template.
   * <p>
   * This method is poorly named and is used primarily to store the relative uri for the request. It
   * has been replaced by {@link RequestTemplate#uri(String)} and will be removed in a future
   * release.
   * </p>
   *
   * @param value to append.
   * @return a RequestTemplate for chaining.
   * @deprecated see {@link RequestTemplate#uri(String, boolean)}
   */
  @Deprecated
  public RequestTemplate append(CharSequence value) {
    /* proxy to url */
    if (this.uriTemplate != null) {
      return this.uri(value.toString(), true);
    }
    return this.uri(value.toString());
  }

  /**
   * Insert the value at the specified point in the template uri.
   * <p>
   * This method is poorly named has undocumented behavior. When the value contains a fully
   * qualified http request url, the value is always inserted at the beginning of the uri.
   * </p>
   * <p>
   * Due to this, use of this method is not recommended and remains for backward compatibility. It
   * has been replaced by {@link RequestTemplate#target(String)} and will be removed in a future
   * release.
   * </p>
   *
   * @param pos in the uri to place the value.
   * @param value to insert.
   * @return a RequestTemplate for chaining.
   * @deprecated see {@link RequestTemplate#target(String)}
   */
  @SuppressWarnings("unused")
  @Deprecated
  public RequestTemplate insert(int pos, CharSequence value) {
    return target(value.toString());
  }

  /**
   * Set the Uri for the request, replacing the existing uri if set.
   *
   * @param uri to use, must be a relative uri.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate uri(String uri) {
    return this.uri(uri, false);
  }

  /**
   * Set the uri for the request.
   *
   * @param uri to use, must be a relative uri.
   * @param append if the uri should be appended, if the uri is already set.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate uri(String uri, boolean append) {
    this.uriTemplate = new UriHelper(uriTemplate).resolveUri(uri, append, requestHeaderTemplate, charset, collectionFormat);

    return this;
  }

  /**
   * Set the target host for this request.
   *
   * @param target host for this request. Must be an absolute target.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate target(String target) {
    requestHeaderTemplate.target(target, charset, collectionFormat);
    return this;
  }

  /**
   * The URL for the request. If the template has not been resolved, the url will represent a uri
   * template.
   *
   * @return the url
   */
  public String url() {
    return requestHeaderTemplate.requestUrl(uriTemplate);
  }

  /**
   * The Uri Path.
   *
   * @return the uri path.
   */
  public String path() {
    return requestHeaderTemplate.path(uriTemplate);
  }

  /**
   * List all of the template variable expressions for this template.
   *
   * @return a list of template variable names
   */
  public List<String> variables() {
    return requestHeaderTemplate.findVariablesIn(uriTemplate, requestBodyTemplate.getBodyTemplate());
  }

  /**
   * @see RequestTemplate#query(String, Iterable)
   */
  public RequestTemplate query(String name, String... values) {
    if (values == null) {
      return query(name, Collections.emptyList());
    }
    return query(name, Arrays.asList(values));
  }


  /**
   * Specify a Query String parameter, with the specified values. Values can be literals or template
   * expressions.
   *
   * @param name of the parameter.
   * @param values for this parameter.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate query(String name, Iterable<String> values) {
    requestHeaderTemplate.addQueryParametersToRequest(charset, name, values, collectionFormat);
    return this;
  }

  /**
   * Specify a Query String parameter, with the specified values. Values can be literals or template
   * expressions.
   *
   * @param name of the parameter.
   * @param values for this parameter.
   * @param collectionFormat to use when resolving collection based expressions.
   * @return a Request Template for chaining.
   */
  public RequestTemplate query(String name,
                               Iterable<String> values,
                               CollectionFormat collectionFormat) {
    requestHeaderTemplate.addQueryParametersToRequest(charset, name, values, collectionFormat);
    return this;
  }


  /**
   * Sets the Query Parameters.
   *
   * @param queries to use for this request.
   * @return a RequestTemplate for chaining.
   */
  @SuppressWarnings("unused")
  public RequestTemplate queries(Map<String, Collection<String>> queries) {
    requestHeaderTemplate.updateQueryString(charset, queries, collectionFormat);
    return this;
  }

  /**
   * Return an immutable Map of all Query Parameters and their values.
   *
   * @return registered Query Parameters.
   */
  public Map<String, Collection<String>> queries() {
    return requestHeaderTemplate.queries(charset);
  }

  /**
   * @see RequestTemplate#header(String, Iterable)
   */
  public RequestTemplate header(String name, String... values) {
    return header(name, Arrays.asList(values));
  }

  /**
   * Specify a Header, with the specified values. Values can be literals or template expressions.
   *
   * @param name of the header.
   * @param values for this header.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate header(String name, Iterable<String> values) {
    this.requestHeaderTemplate.updateHeader(name, values);
    return this;
  }

  /**
   * Clear on reader from {@link RequestTemplate}
   *
   * @param name of the header.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate removeHeader(String name) {
    requestHeaderTemplate.removeHeader(name);
    return this;
  }


  /**
   * Headers for this Request.
   *
   * @param headers to use.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate headers(Map<String, Collection<String>> headers) {
    requestHeaderTemplate.updateHeadersForRequest(headers);
    return this;
  }

  /**
   * Returns an immutable copy of the Headers for this request.
   *
   * @return the currently applied headers.
   */
  public Map<String, Collection<String>> headers() {
    return requestHeaderTemplate.headers();
  }

  /**
   * Sets the Body and Charset for this request.
   *
   * @param data to send, can be null.
   * @param charset of the encoded data.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate body(byte[] data, Charset charset) {
    this.body(Request.Body.create(data, charset));
    return this;
  }

  /**
   * Set the Body for this request. Charset is assumed to be UTF_8. Data must be encoded.
   *
   * @param bodyText to send.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate body(String bodyText) {
    this.body(Request.Body.create(bodyText.getBytes(this.charset), this.charset));
    return this;
  }

  /**
   * Set the Body for this request.
   *
   * @param body to send.
   * @return a RequestTemplate for chaining.
   * @deprecated use {@link #body(byte[], Charset)} instead.
   */
  @Deprecated
  public RequestTemplate body(Request.Body body) {
    requestBodyTemplate.updateBody(body);
    requestHeaderTemplate.setContentLengthTo(body);

    return this;
  }

  /**
   * Charset of the Request Body, if known.
   *
   * @return the currently applied Charset.
   */
  public Charset requestCharset() {
    return requestBodyTemplate.requestCharset(charset);
  }

  /**
   * The Request Body.
   *
   * @return the request body.
   */
  public byte[] body() {
    return requestBodyTemplate.getBody().asBytes();
  }

  /**
   * The Request.Body internal object.
   *
   * @return the internal Request.Body.
   * @deprecated this abstraction is leaky and will be removed in later releases.
   */
  @Deprecated
  public Request.Body requestBody() {
    return requestBodyTemplate.getBody();
  }


  /**
   * Specify the Body Template to use. Can contain literals and expressions.
   *
   * @param bodyTemplate to use.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate bodyTemplate(String bodyTemplate) {
    requestBodyTemplate.specifyBodyTemplate(bodyTemplate, charset);
    return this;
  }

  /**
   * Specify the Body Template to use. Can contain literals and expressions.
   *
   * @param bodyTemplate to use.
   * @return a RequestTemplate for chaining.
   */
  public RequestTemplate bodyTemplate(String bodyTemplate, Charset charset) {
    requestBodyTemplate.specifyBodyTemplate(bodyTemplate, charset);
    this.charset = charset;
    return this;
  }

  /**
   * Body Template to resolve.
   *
   * @return the unresolved body template.
   */
  public String bodyTemplate() {
    return requestBodyTemplate.unresolvedBodyTemplate();
  }

  @Override
  public String toString() {
    return request().toString();
  }

  /**
   * Return if the variable exists on the uri, query, or headers, in this template.
   *
   * @param variable to look for.
   * @return true if the variable exists, false otherwise.
   */
  public boolean hasRequestVariable(String variable) {
    return this.getRequestVariables().contains(variable);
  }

  /**
   * Retrieve all uri, header, and query template variables.
   *
   * @return a List of all the variable names.
   */
  public Collection<String> getRequestVariables() {
    VariablesUtils util = new VariablesUtils(requestHeaderTemplate.getQueries(),
                                             uriTemplate,
                                             requestHeaderTemplate.getHeaders(),
                                             requestBodyTemplate.getBodyTemplate());

    return util.findAllVariables();
  }

  /**
   * If this template has been resolved.
   *
   * @return true if the template has been resolved, false otherwise.
   */
  @SuppressWarnings("unused")
  public boolean resolved() {
    return this.resolved;
  }

  /**
   * The Query String for the template. Expressions are not resolved.
   *
   * @return the Query String.
   */
  public String queryLine() {
    return requestHeaderTemplate.queryString();
  }


  @Experimental
  public RequestTemplate methodMetadata(MethodMetadata methodMetadata) {
    this.methodMetadata = methodMetadata;
    return this;
  }

  @Experimental
  public RequestTemplate feignTarget(Target<?> feignTarget) {
    this.feignTarget = feignTarget;
    return this;
  }

  @Experimental
  public MethodMetadata methodMetadata() {
    return methodMetadata;
  }

  @Experimental
  public Target<?> feignTarget() {
    return feignTarget;
  }

  /**
   * Factory for creating RequestTemplate.
   */
  interface Factory {

    /**
     * create a request template using args passed to a method invocation.
     */
    RequestTemplate create(Object[] argv);
  }

}
