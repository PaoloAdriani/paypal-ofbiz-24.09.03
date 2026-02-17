package paypal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.ofbiz.base.util.Debug;

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
        return paypalOrderIdMap;
    }

    /**
     * Get order parameters when hosted-fields payment method is used
     * @param request
     * @return
     */
    public static Map<String, String> getHostedFieldsOrderParametersFromRequestBody(HttpServletRequest request) {

        Map<String, String> hostedCardFieldsMap = new HashMap<>();

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

        return hostedCardFieldsMap;
    }

}
