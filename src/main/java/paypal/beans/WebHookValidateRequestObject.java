package paypal.beans;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class WebHookValidateRequestObject {
    /**
     * The algorithm that PayPal uses to generate the signature and that you can use to verify the signature.
     * Extract this value from the PAYPAL-AUTH-ALGO response header, which is received with the webhook notification.
     */
    private String auth_algo;
    /**
     * The X.509 public key certificate. Download the certificate from this URL and use it to verify the signature.
     * Extract this value from the PAYPAL-CERT-URL response header, which is received with the webhook notification.
     */
    private String cert_url;
    /**
     * The ID of the HTTP transmission. Contained in the PAYPAL-TRANSMISSION-ID header of the notification message.
     */
    private String transmission_id;
    /**
     * The PayPal-generated asymmetric signature. Appears in the PAYPAL-TRANSMISSION-SIG header of the notification message.
     */
    private String transmission_sig;
    /**
     * The date and time of the HTTP transmission, in Internet date and time format. Appears in the PAYPAL-TRANSMISSION-TIME header of the notification message.
     */
    private String transmission_time;
    /**
     * A webhook event notification.
     */
    private WebHookValidateRequestObject.WebHookEvent webhook_event;
    /**
     * The ID of the webhook as configured in your Developer Portal account.
     */
    private String webhook_id;

    public String getAuth_algo() {
        return auth_algo;
    }

    public void setAuth_algo(String auth_algo) {
        this.auth_algo = auth_algo;
    }

    public String getCert_url() {
        return cert_url;
    }

    public void setCert_url(String cert_url) {
        this.cert_url = cert_url;
    }

    public String getTransmission_id() {
        return transmission_id;
    }

    public void setTransmission_id(String transmission_id) {
        this.transmission_id = transmission_id;
    }

    public String getTransmission_sig() {
        return transmission_sig;
    }

    public void setTransmission_sig(String transmission_sig) {
        this.transmission_sig = transmission_sig;
    }

    public String getTransmission_time() {
        return transmission_time;
    }

    public void setTransmission_time(String transmission_time) {
        this.transmission_time = transmission_time;
    }

    public WebHookValidateRequestObject.WebHookEvent getWebhook_event() {
        return webhook_event;
    }

    public void setWebhook_event(WebHookValidateRequestObject.WebHookEvent webhook_event) {
        this.webhook_event = webhook_event;
    }

    public String getWebhook_id() {
        return webhook_id;
    }

    public void setWebhook_id(String webhook_id) {
        this.webhook_id = webhook_id;
    }

    /**
     * WebHook event object.
     * @author equake58
     *
     */
    public static class WebHookEvent {
        /**
         * The date and time when the webhook event notification was created, in Internet date and time format.
         */
        private String create_time;
        /**
         * The event that triggered the webhook event notification.
         */
        private String event_type;
        /**
         * The event version in the webhook notification.
         */
        private String event_version;
        /**
         * The ID of the webhook event notification.
         */
        private String id;
        /**
         * An array of request-related HATEOAS links.
         */
        private ArrayList<WebHookValidateRequestObject.LinkDescription> links;
        /**
         * The resource that triggered the webhook event notification.
         */
        private LinkedHashMap<String, String> resource;
        /**
         * The name of the resource related to the webhook notification event.
         */
        private String resource_type;
        /**
         * The resource version in the webhook notification.
         */
        private String resource_version;
        /**
         * A summary description for the event notification.
         */
        private String summary;

        public String getCreate_time() {
            return create_time;
        }
        public void setCreate_time(String create_time) {
            this.create_time = create_time;
        }
        public String getEvent_type() {
            return event_type;
        }
        public void setEvent_type(String event_type) {
            this.event_type = event_type;
        }
        public String getEvent_version() {
            return event_version;
        }
        public void setEvent_version(String event_version) {
            this.event_version = event_version;
        }
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public ArrayList<WebHookValidateRequestObject.LinkDescription> getLinks() {
            if (this.links == null) {
                this.links = new ArrayList<>();
            }
            return this.links;
        }
        public void setLinks(ArrayList<LinkDescription> links) {
            this.links = links;
        }
        public LinkedHashMap<String, String> getResource() {
            return resource;
        }
        public void setResource(LinkedHashMap<String, String> resource) {
            this.resource = resource;
        }
        public String getResource_type() {
            return resource_type;
        }
        public void setResource_type(String resource_type) {
            this.resource_type = resource_type;
        }
        public String getResource_version() {
            return resource_version;
        }
        public void setResource_version(String resource_version) {
            this.resource_version = resource_version;
        }
        public String getSummary() {
            return summary;
        }
        public void setSummary(String summary) {
            this.summary = summary;
        }
    }
    /**
     * HATEOAS links structure.
     * @author equake58
     *
     */
    public static class LinkDescription {
        /**
         * The complete target URL. To make the related call, combine the method with this URI Template-formatted link. For pre-processing, include the $, (, and ) characters.
         * The href is the key HATEOAS component that links a completed call with a subsequent call.
         */
        private String href;
        /**
         * The link relation type, which serves as an ID for a link that unambiguously describes the semantics of the link. See Link Relations.
         */
        private String rel;
        /**
         * The HTTP method required to make the related call.
         */
        private String method;

        public String getHref() {
            return href;
        }
        public void setHref(String href) {
            this.href = href;
        }
        public String getRel() {
            return rel;
        }
        public void setRel(String rel) {
            this.rel = rel;
        }
        public String getMethod() {
            return method;
        }
        public void setMethod(String method) {
            this.method = method;
        }
    }

    public static class Resource {}

}
