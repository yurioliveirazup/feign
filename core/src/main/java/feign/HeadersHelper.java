package feign;

import feign.template.HeaderTemplate;
import feign.template.TemplateChunk;

import java.util.*;

class HeadersHelper {
    private Map<String, HeaderTemplate> headers;

    public HeadersHelper(Map<String, HeaderTemplate> headers) {

        this.headers = headers;
    }

    public Map<String, HeaderTemplate> makeHeaderTemplate(String name, TemplateChunk ...chunks) {
        if (chunks == null) {
            throw new IllegalArgumentException("chunks are required.");
        }

        List<TemplateChunk> templateChunks = Arrays.asList(chunks);

        if (templateChunks.isEmpty()) {
            this.headers.remove(name);
            return headers;
        }

        this.headers.compute(name, (headerName, headerTemplate) -> {
            if (headerTemplate == null) {
                return HeaderTemplate.from(name, templateChunks);
            } else {
                return HeaderTemplate.appendFrom(headerTemplate, templateChunks);
            }
        });

        return headers;
    }
    public Map<String, HeaderTemplate> makeHeaderTemplate(String name, Iterable<String> values) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is required.");
        }

        if (values == null) {
            values = Collections.emptyList();
        }

        if (!values.iterator().hasNext()) {
            /* empty value, clear the existing values */
            this.headers.remove(name);
            return headers;
        }
        if (name.equals("Content-Type")) {
            // a client can only produce content of one single type, so always override Content-Type and
            // only add a single type
            this.headers.remove(name);
            this.headers.put(name,
                    HeaderTemplate.create(name, Collections.singletonList(values.iterator().next())));
            return headers;
        }

        ArrayList<String> headerValues = new ArrayList<>();
        values.forEach(headerValues::add);
        headers.compute(name, (headerName, headerTemplate) -> {
            if (headerTemplate == null) {
                return HeaderTemplate.create(headerName, headerValues);
            } else {
                return HeaderTemplate.append(headerTemplate, headerValues);
            }
        });

        return headers;
    }

    public Map<String, Collection<String>> getHeadersMap() {
        TreeMap<String, Collection<String>> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.headers.forEach((key, headerTemplate) -> {
            List<String> values = new ArrayList<>(headerTemplate.getValues());

            /* add the expanded collection, but only if it has values */
            if (!values.isEmpty()) {
                headerMap.put(key, Collections.unmodifiableList(values));
            }
        });

        return headerMap;
    }
}
