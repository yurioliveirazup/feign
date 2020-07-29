package feign;

import feign.template.BodyTemplate;

import java.nio.charset.Charset;
import java.util.Map;

import static java.util.Objects.isNull;

class RequestBodyTemplate {

    private BodyTemplate bodyTemplate;
    private Request.Body body = Request.Body.empty();

    public RequestBodyTemplate(BodyTemplate bodyTemplate, Request.Body body) {
        this.bodyTemplate = bodyTemplate;
        this.body = body;
    }

    public RequestBodyTemplate() { }

    public BodyTemplate getBodyTemplate() {
        return bodyTemplate;
    }

    public Request.Body getBody() {
        return body;
    }

    public Charset requestCharset(Charset charset) {
        if (this.body != null) {
            return this.body.getEncoding()
                    .orElse(charset);
        }
        return charset;
    }

    public void updateBody(Request.Body body) {
        this.body = body;

        /* body template must be cleared to prevent double processing */
        this.bodyTemplate = null;
    }

    public void specifyBodyTemplate(String bodyTemplate, Charset charset) {
        this.bodyTemplate = BodyTemplate.create(bodyTemplate, charset);
    }

    public String unresolvedBodyTemplate() {
        if (this.bodyTemplate != null) {
            return this.bodyTemplate.toString();
        }
        return null;
    }

    public String resolve(Map<String, ?> variables) {
        if (!isNull(bodyTemplate)) {
            return bodyTemplate.expand(variables);
        }

        return "";
    }
}
