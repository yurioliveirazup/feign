package feign;

import feign.template.QueryTemplate;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class QueryParamsResolver {

    private Pattern queryStringPattern;
    private Map<String, QueryTemplate> queries;
    private Map<String, ?> variables;

    public QueryParamsResolver(Pattern queryStringPattern, Map<String, QueryTemplate> queries, Map<String, ?> variables) {

        this.queryStringPattern = queryStringPattern;
        this.queries = queries;
        this.variables = variables;
    }

    public StringBuilder resolve(RequestHeaderTemplate requestHeaderTemplate, String expanded) {
        StringBuilder uri = new StringBuilder();

        if (expanded != null) {
            uri.append(expanded);
        }

        /*
         * for simplicity, combine the queries into the uri and use the resulting uri to seed the
         * resolved template.
         */
        if (!this.queries.isEmpty()) {
            /*
             * since we only want to keep resolved query values, reset any queries on the resolved copy
             */
            requestHeaderTemplate.updateQueries(Collections.emptyMap());
            StringBuilder query = new StringBuilder();
            Iterator<QueryTemplate> queryTemplates = this.queries.values().iterator();

            while (queryTemplates.hasNext()) {
                QueryTemplate queryTemplate = queryTemplates.next();
                String queryExpanded = queryTemplate.expand(variables);
                if (Util.isNotBlank(queryExpanded)) {
                    query.append(queryExpanded);
                    if (queryTemplates.hasNext()) {
                        query.append("&");
                    }
                }
            }

            String queryString = query.toString();
            if (!queryString.isEmpty()) {
                Matcher queryMatcher = queryStringPattern.matcher(uri);
                if (queryMatcher.find()) {
                    /* the uri already has a query, so any additional queries should be appended */
                    uri.append("&");
                } else {
                    uri.append("?");
                }
                uri.append(queryString);
            }
        }

        return uri;
    }
}
