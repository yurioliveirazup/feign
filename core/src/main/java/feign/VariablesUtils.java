package feign;

import feign.template.BodyTemplate;
import feign.template.HeaderTemplate;
import feign.template.QueryTemplate;
import feign.template.UriTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class VariablesUtils {

    private Map<String, QueryTemplate> queries;
    private final UriTemplate uriTemplate;
    private final Map<String, HeaderTemplate> headers;
    private BodyTemplate bodyTemplate;

    public VariablesUtils(Map<String, QueryTemplate> queries, UriTemplate uriTemplate, Map<String, HeaderTemplate> headers, BodyTemplate bodyTemplate) {
        this.queries = queries;
        this.uriTemplate = uriTemplate;
        this.headers = headers;
        this.bodyTemplate = bodyTemplate;
    }

    public List<String> findAllVariables() {
        /* combine the variables from the uri, query, header, and body templates */
        List<String> variables = new ArrayList<>(this.uriTemplate.getVariables());

        /* queries */
        for (QueryTemplate queryTemplate : this.queries.values()) {
            variables.addAll(queryTemplate.getVariables());
        }

        /* headers */
        for (HeaderTemplate headerTemplate : this.headers.values()) {
            variables.addAll(headerTemplate.getVariables());
        }

        /* body */
        if (this.bodyTemplate != null) {
            variables.addAll(this.bodyTemplate.getVariables());
        }

        return variables;
    }
}
