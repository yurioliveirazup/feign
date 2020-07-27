package feign;

import feign.template.UriTemplate;

class PathFactory {

    private String target;
    private UriTemplate uriTemplate;

    public PathFactory(String target, UriTemplate uriTemplate) {
        this.target = target;
        this.uriTemplate = uriTemplate;
    }

    public String makePath() {
        /* build the fully qualified url with all query parameters */
        StringBuilder path = new StringBuilder();
        if (this.target != null) {
            path.append(this.target);
        }
        if (this.uriTemplate != null) {
            path.append(this.uriTemplate.toString());
        }
        if (path.length() == 0) {
            /* no path indicates the root uri */
            path.append("/");
        }

        return path.toString();
    }
}
