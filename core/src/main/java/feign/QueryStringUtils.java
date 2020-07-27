package feign;

import feign.template.QueryTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

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

    public static Map<String, List<String>> extractQueryParameters(String queryString) {
        return Arrays.stream(queryString.split("&"))
                        .map(QueryStringUtils::splitQueryParameter)
                        .collect(Collectors.groupingBy(
                                 AbstractMap.SimpleImmutableEntry::getKey,
                                 LinkedHashMap::new,
                                 Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(String pair) {
        int eq = pair.indexOf("=");
        final String name = (eq > 0) ? pair.substring(0, eq) : pair;
        final String value = (eq > 0 && eq < pair.length()) ? pair.substring(eq + 1) : null;
        return new AbstractMap.SimpleImmutableEntry<>(name, value);
    }
}
