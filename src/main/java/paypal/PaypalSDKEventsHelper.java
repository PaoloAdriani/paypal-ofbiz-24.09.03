package paypal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PaypalSDKEventsHelper {

    public static final String module = PaypalSDKEventsHelper.class.getName();

    /**
     * Returns an the orderId in the request body.
     * Method used for the JavaScript SDK integration to read JSON-ified data
     * incoming from interface ajax requests.
     * @param request
     * @return
     */
    public static String getOrderIdFromRequestBody(HttpServletRequest request) {

        String orderId = null;

        Map<String, Object> requestAttributeMap = UtilHttp.getAttributeMap(request);
        Debug.logWarning("(getOrderIdFromRequestBody) request attribute map =>\n" + UtilMisc.printMap(requestAttributeMap), module);

/* Not working in OFBiz-16+, since JSON data in requests are
stripped by ContextFilter and put directly into the request as attributes.
        try {
            BufferedReader reader = request.getReader();
            StringBuilder buffer = new StringBuilder();
            JsonObject responseData = null;
            String line;
            while ((line = reader.readLine()) != null) {

                buffer.append(line);
                buffer.append(System.lineSeparator());
            }
            responseData = (JsonObject) new JsonParser().parse(buffer.toString());
            if (responseData != null) {
                orderId = (responseData.get("orderId") != null) ? responseData.get("orderId").getAsString() : null;

            }
        } catch (IOException e) {
            String msg = "Error in reading order id in request body. Returning null. Msg => " + e.getMessage();
            Debug.logError(msg, module);
            request.setAttribute("_ERROR_MESSAGE_", msg);
            return null;
        }

 */

        orderId = request.getAttribute("orderId") != null ? (String) request.getAttribute("orderId") : null;

        return orderId;
    }
    /**
     *
     * @param request
     * @return
     */
    public static Map<String, String> getOrderIdsFromRequestBody(HttpServletRequest request) {

        Map<String, String> paypalOrderIdMap = null;
        String paypalOrderId = null;
        String orderId = null;

        Map<String, Object> requestAttributeMap = UtilHttp.getAttributeMap(request);
        Debug.logWarning("(getOrderIdsFromRequestBody) request attribute map =>\n" + UtilMisc.printMap(requestAttributeMap), module);

/*
        try {
            BufferedReader reader = request.getReader();
            StringBuilder buffer = new StringBuilder();
            JsonObject responseData = null;
            String line;
            while ((line = reader.readLine()) != null) {

                buffer.append(line);
                buffer.append(System.lineSeparator());
            }
            responseData = (JsonObject) new JsonParser().parse(buffer.toString());
            if (responseData != null) {
                paypalOrderIdMap = new HashMap<>();
                paypalOrderId = (responseData.get("paypalOrderId") != null) ? responseData.get("paypalOrderId").getAsString() : null;
                orderId = (responseData.get("orderId") != null) ? responseData.get("orderId").getAsString() : null;
                paypalOrderIdMap.put("paypalOrderId", paypalOrderId);
                paypalOrderIdMap.put("orderId", orderId);
            }
        } catch (IOException e) {
            String msg = "Error in reading PayPal order id in request body. Returning null. Msg => " + e.getMessage();
            Debug.logError(msg, module);
            request.setAttribute("_ERROR_MESSAGE_", msg);
            return null;
        }

 */
        paypalOrderIdMap = new HashMap<>();
        paypalOrderId = (request.getAttribute("paypalOrderId") != null) ? (String) request.getAttribute("paypalOrderId") : null;
        orderId = (request.getAttribute("orderId") != null) ? (String) request.getAttribute("orderId") : null;
        paypalOrderIdMap.put("paypalOrderId", paypalOrderId);
        paypalOrderIdMap.put("orderId", orderId);


        return paypalOrderIdMap;
    }

    /**
     * Get order parameters when hosted-fields payment method is used
     * @param request
     * @return
     */
    public static Map<String, String> getHostedFieldsOrderParametersFromRequestBody(HttpServletRequest request) {

        Map<String, String> hostedCardFieldsMap = new HashMap<>();

        Map<String, Object> requestAttributeMap = UtilHttp.getAttributeMap(request);
        Debug.logWarning("(getHostedFieldsOrderParametersFromRequestBody) request attribute map =>\n" + UtilMisc.printMap(requestAttributeMap), module);

        /*
        try {
            BufferedReader reader = request.getReader();
            StringBuilder buffer = new StringBuilder();
            JsonObject responseData = null;
            String line;
            while ((line = reader.readLine()) != null) {

                buffer.append(line);
                buffer.append(System.lineSeparator());
            }
            responseData = (JsonObject) new JsonParser().parse(buffer.toString());
            if (responseData != null) {
                String orderId = (String) responseData.get("orderId").getAsString();
                String paymentMethod = (String) responseData.get("paymentMethod").getAsString();
                String cardHolderName = (responseData.get("cardHolderName") != null) ? responseData.get("cardHolderName").getAsString() : "";
                String cardStreetAddress = (responseData.get("streetAddress") != null) ? responseData.get("streetAddress").getAsString() : "";
                String cardExtendedAddress = (responseData.get("extendedAddress") != null) ? responseData.get("extendedAddress").getAsString() : "";
                String cardAddressState = (responseData.get("state") != null) ? responseData.get("state").getAsString() : "";
                String cardAddressCity = (responseData.get("city") != null) ? responseData.get("city").getAsString() : "";
                String cardAddressPostalCode = (responseData.get("postalCode") != null) ? responseData.get("postalCode").getAsString() : "";
                String cardAddressCountryCodeAlpha2 = (responseData.get("countryCodeAlpha2") != null) ? responseData.get("countryCodeAlpha2").getAsString() : "";

                hostedCardFieldsMap.put("orderId", orderId);
                hostedCardFieldsMap.put("paymentMethod", paymentMethod);
                hostedCardFieldsMap.put("cardHolderName", cardHolderName);
                hostedCardFieldsMap.put("cardStreetAddress", cardStreetAddress);
                hostedCardFieldsMap.put("cardExtendedAddress", cardExtendedAddress);
                hostedCardFieldsMap.put("cardAddressState", cardAddressState);
                hostedCardFieldsMap.put("cardAddressCity", cardAddressCity);
                hostedCardFieldsMap.put("cardAddressPostalCode", cardAddressPostalCode);
                hostedCardFieldsMap.put("cardAddressCountryCodeAlpha2", cardAddressCountryCodeAlpha2);

            }
        } catch (IOException e) {
            String msg = "Error in reading PayPal order id in request body. Returning null. Msg => " + e.getMessage();
            Debug.logError(msg, module);
            request.setAttribute("_ERROR_MESSAGE_", msg);
            return null;
        }

         */

        String orderId = (String) request.getAttribute("orderId");
        String paymentMethod = (String) request.getAttribute("paymentMethod");
        String cardHolderName = (request.getAttribute("cardHolderName") != null) ? (String) request.getAttribute("cardHolderName") : "";
        String cardStreetAddress = (request.getAttribute("streetAddress") != null) ? (String) request.getAttribute("streetAddress") : "";
        String cardExtendedAddress = (request.getAttribute("extendedAddress") != null) ? (String) request.getAttribute("extendedAddress") : "";
        String cardAddressState = (request.getAttribute("state") != null) ? (String) request.getAttribute("state") : "";
        String cardAddressCity = (request.getAttribute("city") != null) ? (String) request.getAttribute("city") : "";
        String cardAddressPostalCode = (request.getAttribute("postalCode") != null) ? (String) request.getAttribute("postalCode") : "";
        String cardAddressCountryCodeAlpha2 = (request.getAttribute("countryCodeAlpha2") != null) ? (String) request.getAttribute("countryCodeAlpha2") : "";

        hostedCardFieldsMap.put("orderId", orderId);
        hostedCardFieldsMap.put("paymentMethod", paymentMethod);
        hostedCardFieldsMap.put("cardHolderName", cardHolderName);
        hostedCardFieldsMap.put("cardStreetAddress", cardStreetAddress);
        hostedCardFieldsMap.put("cardExtendedAddress", cardExtendedAddress);
        hostedCardFieldsMap.put("cardAddressState", cardAddressState);
        hostedCardFieldsMap.put("cardAddressCity", cardAddressCity);
        hostedCardFieldsMap.put("cardAddressPostalCode", cardAddressPostalCode);
        hostedCardFieldsMap.put("cardAddressCountryCodeAlpha2", cardAddressCountryCodeAlpha2);


        return hostedCardFieldsMap;
    }

}
