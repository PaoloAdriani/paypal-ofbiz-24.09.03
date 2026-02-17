package paypal;

import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class PaypalClientHelper {

    public static final String module = PaypalClientHelper.class.getName();

    public static final String PAYPAL_SYSTEM_RESOURCE = "paypal";
    public static final String PAYPAL_ENV_TEST = "test";
    public static final String PAYPAL_ENV_PROD = "prod";

    /**
     * Method that returns the environment of the PayPal APIs as stored
     * in the SystemProperty entity.
     * @param delegator
     * @return test/prod
     */
    public static String getPaypalEnvironment(Delegator delegator) {
        return EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "api.environment", delegator);
    }
    /**
     * Return the PayPal APP-ID
     * @param delegator
     * @return
     */
    public static String getPaypalAppId(Delegator delegator) {
        return getPaypalAppId(getPaypalEnvironment(delegator), delegator);
    }

    /**
     * Get PayPal Account ClientId
     * @param delegator
     * @return
     */
    public static String getPaypalClientId(Delegator delegator) {
        return getPaypalClientId(getPaypalEnvironment(delegator), delegator);
    }
    /**
     * Get PayPal Account Secret hash
     * @param delegator
     * @return
     */
    public static String getPaypalClientSecret(Delegator delegator) {
        return getPaypalClientSecret(getPaypalEnvironment(delegator), delegator);
    }
    /**
     * Returns the PayPal Merchant Business Email (Account ID)
     * @param delegator
     * @return
     */
    public static String getPaypalMerchantBusinessEmail(Delegator delegator) {
        return getPaypalMerchantBusinessEmail(getPaypalEnvironment(delegator), delegator);
    }
    /**
     * Get PayPal Order API URL
     * @param delegator
     * @return
     */
    public static String getPaypalAPIUrl(Delegator delegator) {
        return getPaypalAPIUrl(getPaypalEnvironment(delegator), delegator);
    }
    /**
     *
     * @param delegator
     * @return
     */
    public static String getOrderAPIEndPoint(Delegator delegator) {
        return EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "api.createOrderEP", delegator);
    }
    /**
     *
     * @param delegator
     * @return
     */
    public static String showOrderDetailAPIEndPoint(Delegator delegator) {
        return EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "api.showOrderDetails", delegator);
    }
    /**
     *
     * @param delegator
     * @return
     */
    public static String getOAuthAccessTokenEndPoint(Delegator delegator) {
        return EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "api.authTokenEP", delegator);
    }
    /**
     * This token is required to show hosted card fields (returns account eligibility infos)
     * @param delegator
     * @return
     */
    public static String getClientTokenEndPoint(Delegator delegator) {
        return EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "api.clientTokenEP", delegator);
    }
    /**
     *
     * @param delegator
     * @return
     */
    public static String getVerifyWebhookSignatureEP(Delegator delegator) {
        return EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "api.verifyWebHookSigEP", delegator);
    }
    /**
     *
     * @param delegator
     * @return
     */
    public static String getPaypalWebHookId(Delegator delegator) {
        return getPaypalWebHookId(getPaypalEnvironment(delegator), delegator);
    }

    /**
     * Get request header data.
     * @param request
     * @return
     */
    public static HashMap<String, String> getRequestHeaderData(HttpServletRequest request) {
        HashMap<String, String> headersMap = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            headersMap.put(key.toLowerCase(), value);
        }
        return headersMap;
    }
    /**
     *
     * Get request body data
     * @param request
     * @return
     * @since OFBiz v18 - JSON data in requests in OFBiz v18 is extracted and
     * set as request attributes.

    public static HashMap<String, Object> getRequestBodyData(HttpServletRequest request) {
    //For JSON requests, ContextFilter reads the Json data in the body and set it as request attributes
    Map<String, Object> requestAttributeMap = UtilHttp.getAttributeMap(request);
    HashMap<String, Object> paypalBodyDataMap = new HashMap<>();

    paypalBodyDataMap.put("id", requestAttributeMap.get("id"));
    paypalBodyDataMap.put("links", requestAttributeMap.get("links"));
    paypalBodyDataMap.put("event_type", requestAttributeMap.get("event_type"));
    paypalBodyDataMap.put("event_version", requestAttributeMap.get("event_version"));
    paypalBodyDataMap.put("resource_version", requestAttributeMap.get("resource_version"));
    paypalBodyDataMap.put("summary", requestAttributeMap.get("summary"));
    paypalBodyDataMap.put("create_time", requestAttributeMap.get("create_time"));
    paypalBodyDataMap.put("resource", requestAttributeMap.get("resource"));
    paypalBodyDataMap.put("resource_type", requestAttributeMap.get("resource_type"));

    return paypalBodyDataMap;
    }
     */

    /**
     *
     * @param request
     * @return
     * @since OFBiz v13 - JSON data in request are kept in the body and not extracted
     */
    public static String getRequestRawBodyData(HttpServletRequest request) {


        StringBuilder responseBuilder = new StringBuilder();

        try {
            //Reading raw request body
            BufferedReader reader = request.getReader();

            String resLine;
            while((resLine = reader.readLine()) != null) {
                responseBuilder.append(resLine);
            }

            reader.close();
        } catch (IOException ex) {
            Debug.logError(ex.getMessage(), module);
            return null;
        }

        return responseBuilder.toString();

    }
    /**
     *
     * @param request
     * @return
     */
    public static HashMap<String, Object> getRequestBodyData(HttpServletRequest request) {
        try {
            return extractMapFromRequestBody(request);
        } catch (IOException ex) {
            String msg = "Error while reading JSON body response. Error is => " + ex.getMessage();
            Debug.logError(msg, module);
            return null;
        }
    }

    /**
     *
     * @param apiEnv
     * @param delegator
     * @return
     */
    public static String getPaypalMerchantAccountId(String apiEnv, Delegator delegator) {
        return (PaypalClientHelper.PAYPAL_ENV_TEST.equals(apiEnv)) ?
                EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "merchantAccountId.test", delegator) : EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "merchantAccountId.prod", delegator);
    }


    /* ########## PRIVATE METHODS ########## */

    private static LinkedHashMap<String, Object> extractMapFromRequestBody(ServletRequest request) throws IOException {
        return UtilGenerics.<LinkedHashMap<String, Object>>cast(JSON.from(request.getInputStream()).toObject(LinkedHashMap.class));
    }

    /**
     *
     * @param apiEnv
     * @param delegator
     * @return
     */
    private static String getPaypalAppId(String apiEnv, Delegator delegator) {
        return (PaypalClientHelper.PAYPAL_ENV_TEST.equals(apiEnv)) ?
                EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "api.appID.test", delegator) : EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "api.appID.prod", delegator);
    }

    /**
     *
     * @param apiEnv
     * @param delegator
     * @return
     */
    private static String getPaypalClientId(String apiEnv, Delegator delegator) {
        return (PaypalClientHelper.PAYPAL_ENV_TEST.equals(apiEnv)) ?
                EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "clientID.test", delegator) : EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "clientID.prod", delegator);
    }

    /**
     *
     * @param apiEnv
     * @param delegator
     * @return
     */
    private static String getPaypalClientSecret(String apiEnv, Delegator delegator) {
        return (PaypalClientHelper.PAYPAL_ENV_TEST.equals(apiEnv)) ?
                EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "clientSECRET.test", delegator) : EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "clientSECRET.prod", delegator);
    }

    /**
     *
     * @param apiEnv
     * @param delegator
     * @return
     */
    private static String getPaypalMerchantBusinessEmail(String apiEnv, Delegator delegator) {
        return (PaypalClientHelper.PAYPAL_ENV_TEST.equals(apiEnv)) ?
                EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "merchantBusinessEmail.test", delegator) : EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "merchantBusinessEmail.prod", delegator);
    }

    /**
     *
     * @param apiEnv
     * @param delegator
     * @return
     */
    private static String getPaypalAPIUrl(String apiEnv, Delegator delegator) {
        return (PaypalClientHelper.PAYPAL_ENV_TEST.equals(apiEnv)) ?
                EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "apiURL.test", delegator) : EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "apiURL.prod", delegator);
    }
    /**
     *
     * @param apiEnv
     * @param delegator
     * @return
     */
    private static String getPaypalWebHookId(String apiEnv, Delegator delegator) {
        return (PaypalClientHelper.PAYPAL_ENV_TEST.equals(apiEnv)) ?
                EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "webHookId.test", delegator) : EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "webHookId.prod", delegator);
    }

}
