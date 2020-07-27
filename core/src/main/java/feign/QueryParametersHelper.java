package feign;

import feign.template.QueryTemplate;

import java.nio.charset.Charset;
import java.util.*;

class QueryParametersHelper {

    private Map<String, QueryTemplate> queries;
    private Charset charset;
    private boolean decodeSlash;

    public QueryParametersHelper(Map<String, QueryTemplate> queries, Charset charset, boolean decodeSlash) {
        this.queries = queries;
        this.charset = charset;
        this.decodeSlash = decodeSlash;
    }


    public Map<String, Collection<String>> getQueryMap() {
        Map<String, Collection<String>> queryMap = new LinkedHashMap<>();
        this.queries.forEach((key, queryTemplate) -> {
            List<String> values = new ArrayList<>(queryTemplate.getValues());

            /* add the expanded collection, but lock it */
            queryMap.put(key, Collections.unmodifiableList(values));
        });

        return queryMap;
    }

    public Map<String, QueryTemplate> makeQueryParametesToRequest(String name, Iterable<String> values, CollectionFormat collectionFormat) {
        if (!values.iterator().hasNext()) {
            /* empty value, clear the existing values */
            this.queries.remove(name);
            return queries;
        }

        /* create a new query template out of the information here */
        this.queries.compute(name, (key, queryTemplate) -> {
            if (queryTemplate == null) {
                return QueryTemplate.create(name, values, this.charset, collectionFormat, this.decodeSlash);
            } else {
                return QueryTemplate.append(queryTemplate, values, collectionFormat, this.decodeSlash);
            }
        });

        return queries;
    }
}
