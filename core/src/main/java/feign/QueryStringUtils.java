package feign;

import feign.template.QueryTemplate;

import java.util.Iterator;
import java.util.Map;

class QueryStringUtils {
    public static String getQueryStrings(Map<String, QueryTemplate> queries) {

        StringBuilder queryString = new StringBuilder();

        if (!queries.isEmpty()) {
            Iterator<QueryTemplate> iterator = queries.values().iterator();
            while (iterator.hasNext()) {
                QueryTemplate queryTemplate = iterator.next();
                String query = queryTemplate.toString();
                if (query != null && !query.isEmpty()) {
                    queryString.append(query);
                    if (iterator.hasNext()) {
                        queryString.append("&");
                    }
                }
            }
        }
        /* remove any trailing ampersands */
        String result = queryString.toString();
        if (result.endsWith("&")) {
            result = result.substring(0, result.length() - 1);
        }

        if (!result.isEmpty()) {
            result = "?" + result;
        }

        return result;
    }
}
