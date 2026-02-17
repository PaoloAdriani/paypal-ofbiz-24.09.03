package paypal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.party.party.PartyHelper;
import org.apache.ofbiz.product.product.ProductContentWrapper;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import paypal.beans.OrderRequestObject;
import paypal.beans.PaypalRequestEnum;
import paypal.beans.WebHookValidateRequestObject;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

import com.google.gson.JsonObject;

public class PaypalEventsHelper {

    public static final String module = PaypalEventsHelper.class.getName();

    private static final String PAYPAL_SYSTEM_RESOURCE = "paypal";
    private static final String MP_SYSTEM_RESOURCE_ID = "mpstyle";
    private static final String MPOMNI_SYSTEM_RESOURCE_ID = "mpomni";

    /* ########################## NOTIFICATION METHODS ########################## */
    /**
     * Wrapper method to send an order confirmation email.
     *
     * @param orderId
     * @param delegator
     * @param dispatcher
     * @return
     */
    public static Map<String, Object> sendOrderConfirmationEmail(String orderId, Delegator delegator, LocalDispatcher dispatcher) {
        Map<String, Object> returnMap = null;
        if (orderId == null || UtilValidate.isEmpty(orderId)) {
            return ServiceUtil.returnError("OrderId is null or empty. Cannot send order confirmation email.");
        }
        Map<String, Object> orderConfirmEmailCtx = UtilMisc.toMap("orderId", orderId, "login.username", PaypalEventsHelper.getServiceUsername(delegator), "login.password", PaypalEventsHelper.getServicePassword(delegator));
        try {
            returnMap = dispatcher.runSync("sendOrderConfirmation", orderConfirmEmailCtx);
        } catch (GenericServiceException gse) {
            String msg = "Error in sending order confirmation email. Msg => " + gse.getMessage();
            returnMap = ServiceUtil.returnError(msg);
        }
        return returnMap;
    }

    /* ########################## DATA UPDATE METHODS ########################## */
    /**
     *
     * @param payPalOrderId
     * @param orderId
     * @param delegator
     * @return
     */
    public static boolean setOrderHeaderPayPalOrderId(String payPalOrderId, String orderId, Delegator delegator) {
        if (payPalOrderId == null || UtilValidate.isEmpty(payPalOrderId)) {
            Debug.logError("PayPal Order ID is null or empty, cannot set it on OrderHeader record.", module);
            return false;
        }
        if (orderId == null || UtilValidate.isEmpty(orderId)) {
            Debug.logError("Local Order ID is null or empty, cannot set PayPal Order ID on OrderHeader record.", module);
            return false;
        }

        GenericValue orderHeader = null;

        try {
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
            return false;
        }

        orderHeader.set("mpPayPalOrderId", payPalOrderId);

        try {
            delegator.store(orderHeader);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
        }
        return true;
    }

    /**
     * Creates a OrderHeader note.
     *
     * @param orderId
     * @param noteMsg
     * @param dispatcher
     * @param delegator
     * @return
     */
    public static boolean createOrderNote(String orderId, String noteMsg, LocalDispatcher dispatcher, Delegator delegator) {

        if (orderId == null || UtilValidate.isEmpty(orderId)) {
            Debug.logError("OrderId is null or empty. Cannot create order note.", module);
            return false;
        }

        if (noteMsg == null || UtilValidate.isEmpty(noteMsg)) {
            Debug.logError("Note message is null or empty. Cannot create order note.", module);
            return false;
        }

        String username = getServiceUsername(delegator);
        String password = getServicePassword(delegator);

        Map<String, Object> inMap = new HashMap<>();
        inMap.put("internalNote", "Y");
        inMap.put("orderId", orderId);
        inMap.put("note", noteMsg);
        inMap.put("login.username", username);
        inMap.put("login.password", password);

        Map<String, Object> returnMap = null;

        try {
            returnMap = dispatcher.runSync("createOrderNote", inMap);
        } catch (GenericServiceException gse) {
            Debug.logError(gse.getMessage(), module);
            return false;
        }

        return (returnMap != null && ServiceUtil.isSuccess(returnMap));

    }

    /* ########################## JSON RESPONSE DATA CONSTRUCTION (FOR SERVLET RESPONSES) ########################## */

    /** Builds a json object to give as a response to the servlet when
     * calling server methods from ajax (SDK JS integration)
     * @param jsonFields
     * @return
     */
    public static String buildServletJsonResponse(HashMap<String, String> jsonFields) {
        JsonObject jsonObj = new JsonObject();
        if (jsonFields == null || jsonFields.isEmpty()) {
            jsonObj.addProperty("-", "-");
        } else {
            for(Map.Entry<String, String> entry : jsonFields.entrySet()) {
                jsonObj.addProperty(entry.getKey(), entry.getValue());
            }
        }
        return jsonObj.toString();
    }

    /* ########################## JSON RESPONSE DATA EXTRACTION ########################## */
    /**
     * Get PayPal order status from PayPal Json Response
     *
     * @param jsonResponse
     * @return
     */
    public static String getOrderStatusFromJsonResponse(JsonObject jsonResponse) {
        String payPalOrderStatus = "";
        if (jsonResponse == null) {
            Debug.logError("JsonObject response is null. Cannot return PayPal Order Status.", module);
            return null;
        }
        JsonElement orderStatusEl = jsonResponse.get("status");
        payPalOrderStatus = orderStatusEl.getAsString();
        return payPalOrderStatus;
    }

    /**
     * Get PayPal OrderID from Json Response
     *
     * @param jsonResponse
     * @return
     */
    public static String getOrderIdFromJsonResponse(JsonObject jsonResponse) {
        String payPalOrderId = "";
        if (jsonResponse == null) {
            Debug.logError("JsonObject response is null. Cannot return PayPal OrderID.", module);
            return null;
        }
        JsonElement orderIdEl = jsonResponse.get("id");
        payPalOrderId = orderIdEl.getAsString();
        return payPalOrderId;
    }

    /**
     *
     * @param jsonResponse
     * @return
     */
    public static ArrayList<HashMap<String, String>> getHATEOASLinksFromJsonResponse(JsonObject jsonResponse) {

        ArrayList<HashMap<String, String>> hateoasLinksList = null;

        if (jsonResponse == null) {
            Debug.logError("JsonObject response is null. Cannot return PayPal HATEOAS Links.", module);
            return null;
        }

        hateoasLinksList = new ArrayList<>();

        JsonArray hateoasLinksArr = jsonResponse.get("links").getAsJsonArray();

        for (JsonElement _elem : hateoasLinksArr) {
            HashMap<String, String> linkMap = new HashMap<>();
            System.out.println("--- _elem: " + _elem);
            JsonObject _elemObj = _elem.getAsJsonObject();
            String href = _elemObj.get("href").getAsString();
            String rel = _elemObj.get("rel").getAsString();
            String method = _elemObj.get("method").getAsString();
            linkMap.put("href", href);
            linkMap.put("rel", rel);
            linkMap.put("method", method);
            hateoasLinksList.add(linkMap);
        }
        return hateoasLinksList;
    }

    /**
     *
     * @param requestBodyMap
     * @return
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<HashMap<String, String>> getHATEOASLinksFromNotifyResponse(HashMap<String, Object> requestBodyMap) {
        ArrayList<HashMap<String, String>> hateoasLinksList = null;
        LinkedHashMap<String, Object> resource = (LinkedHashMap<String, Object>) requestBodyMap.get("resource");
        ArrayList<LinkedHashMap<String, String>> linksStr = (ArrayList<LinkedHashMap<String, String>>) resource.get("links");
        hateoasLinksList = new ArrayList<>();
        for (LinkedHashMap<String, String> _elem : linksStr) {
            HashMap<String, String> linkMap = new HashMap<>();
            System.out.println("--- _elem: " + _elem);
            String href = _elem.get("href");
            String rel = _elem.get("rel");
            String method = _elem.get("method");
            linkMap.put("href", href);
            linkMap.put("rel", rel);
            linkMap.put("method", method);
            hateoasLinksList.add(linkMap);
        }
        return hateoasLinksList;
    }

    /**
     *
     * @param hateoasLinksList
     * @param relType
     * @return
     */
    public static String getHATEOASByRelType(ArrayList<HashMap<String, String>> hateoasLinksList, String relType) {
        String link = null;
        if (hateoasLinksList == null || hateoasLinksList.isEmpty()) {
            Debug.logError("HATEOAS link list is null or empty. Cannot return link.", module);
            return null;
        }
        if (relType == null || UtilValidate.isEmpty(relType)) {
            Debug.logError("Link rel type is null or empty. Cannot return link.", module);
            return null;
        }
        for (HashMap<String, String> _linkMap : hateoasLinksList) {
            String _linkRelType = _linkMap.get("rel");
            if (link == null && relType.equals(_linkRelType)) {
                link = _linkMap.get("href");
            }
        }
        return link;
    }

    /**
     *
     * @param hateoasLinksList
     * @param relType
     * @return
     */
    public static String getHATEOASLinkMethodByRelType(ArrayList<HashMap<String, String>> hateoasLinksList, String relType) {
        String method = null;
        if (hateoasLinksList == null || hateoasLinksList.isEmpty()) {
            Debug.logError("HATEOAS link list is null or empty. Cannot return link.", module);
            return null;
        }
        if (relType == null || UtilValidate.isEmpty(relType)) {
            Debug.logError("Link rel type is null or empty. Cannot return link method.", module);
            return null;
        }
        for (HashMap<String, String> _linkMap : hateoasLinksList) {
            String _linkRelType = _linkMap.get("rel");
            if (method == null && relType.equals(_linkRelType)) {
                method = _linkMap.get("method");
            }
        }
        return method;
    }

    /**
     *
     * @param jsonResponse
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String getPaypalOrderIdFromNotifyResponse(HashMap<String, Object> requestBodyMap, String resource_type) {
        String paypalOrderId = "";
        LinkedHashMap<String, Object> resource = (LinkedHashMap<String, Object>) requestBodyMap.get("resource");
        switch (resource_type) {
            case "checkout-order":
                paypalOrderId = (String) resource.get("id");
                break;
            case "capture":
                LinkedHashMap<String, Object> supplDataMap = (LinkedHashMap<String, Object>) resource.get("supplementary_data");
                LinkedHashMap<String, Object> relatedIdsMap = (LinkedHashMap<String, Object>) supplDataMap.get("related_ids");
                paypalOrderId = (String) relatedIdsMap.get("order_id");
                break;
            default:
                Debug.logError("Resource Type [" + resource_type + "] not handled.", module);
        }
        return paypalOrderId;
    }

    /**
     *
     * @param requestBodyMap
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String getPaypalOrderStatusFromNotifyResponse(HashMap<String, Object> requestBodyMap) {
        String paypalOrderStatus = "";
        LinkedHashMap<String, String> resource = (LinkedHashMap<String, String>) requestBodyMap.get("resource");
        paypalOrderStatus = resource.get("status");
        return paypalOrderStatus;
    }

    /**
     * Get the custom order id (e-commerce orderId) from the resource object in
     * the notify response, based on the resource type.
     *
     * @param requestBodyMap
     * @param resource_type
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String getCustomOrderIdFromNotifyResponse(HashMap<String, Object> requestBodyMap, String resource_type) {
        String customOrderId = "";
        LinkedHashMap<String, String> resource = (LinkedHashMap<String, String>) requestBodyMap.get("resource");
        switch (resource_type) {
            case "capture":
                customOrderId = (String) resource.get("custom_id");
                break;
            default:
                Debug.logError("Resource Type [" + resource_type + "] not handled.", module);
        }
        return customOrderId;
    }

    /**
     *
     * @param jsonResponse
     * @return
     */
    public static String getTransactionStatusFromJsonResponse(JsonObject jsonResponse) {

        String transactionStatus = "";

        JsonArray purchaseUnitArray = (JsonArray) jsonResponse.get("purchase_units");
        JsonObject pchPayments = (JsonObject) ((JsonObject) purchaseUnitArray.get(0)).get("payments");
        JsonArray paymCapturesArray = (JsonArray) pchPayments.get("captures");
        JsonObject paymentCapture = (JsonObject) paymCapturesArray.get(0);
        JsonElement transStatusElement = (JsonElement) paymentCapture.get("status");
        transactionStatus = transStatusElement.getAsString();

        return transactionStatus;
    }

    /**
     *
     * @param jsonResponse
     * @return
     */
    public static String getTransactionStatusDetailFromJsonResponse(JsonObject jsonResponse) {

        String transactionStatusDetails = "";

        JsonArray purchaseUnitArray = (JsonArray) jsonResponse.get("purchase_units");
        JsonObject pchPayments = (JsonObject) ((JsonObject) purchaseUnitArray.get(0)).get("payments");
        JsonArray paymCapturesArray = (JsonArray) pchPayments.get("captures");
        JsonObject paymentCapture = (JsonObject) paymCapturesArray.get(0);
        //Not always present in the response
        if (paymentCapture.get("status_details") != null) {
            JsonObject transStatusDetailsElement = (JsonObject) paymentCapture.get("status_details");
            JsonElement statusDetailReasonElement = (JsonElement) transStatusDetailsElement.get("reason");
            transactionStatusDetails = statusDetailReasonElement.getAsString();
        }
        return transactionStatusDetails;
    }

    /**
     *
     * @param jsonResponse
     * @return
     */
    public static Map<String, String> getCardTransactionProcessorResponseFromJsonResponse(JsonObject jsonResponse) {

        HashMap<String, String> cardProcessorStatusMap = new HashMap<>();

        JsonArray purchaseUnitArray = (JsonArray) jsonResponse.get("purchase_units");
        JsonObject pchPayments = (JsonObject) ((JsonObject) purchaseUnitArray.get(0)).get("payments");
        JsonArray paymCapturesArray = (JsonArray) pchPayments.get("captures");
        JsonObject paymentCapture = (JsonObject) paymCapturesArray.get(0);
        if (paymentCapture.get("processor_response") != null) {
            JsonObject processorResponseElement = (JsonObject) paymentCapture.get("processor_response");
            JsonElement avsCodeElement = (JsonElement) processorResponseElement.get("avs_code");
            JsonElement cvvCodeElement = (JsonElement) processorResponseElement.get("cvv_code");
            JsonElement responseCodeElement = (JsonElement) processorResponseElement.get("response_code");
            JsonElement paymentAdviceCodeElement = (JsonElement) processorResponseElement.get("payment_advice_code");
            if (avsCodeElement != null) {
                cardProcessorStatusMap.put("avs_code", avsCodeElement.getAsString());
            }
            if (cvvCodeElement != null) {
                cardProcessorStatusMap.put("cvv_code", cvvCodeElement.getAsString());
            }
            if (responseCodeElement != null) {
                cardProcessorStatusMap.put("response_code", responseCodeElement.getAsString());
            }
            if (paymentAdviceCodeElement != null) {
                cardProcessorStatusMap.put("payment_advice_code", paymentAdviceCodeElement.getAsString());
            }
        }

        return cardProcessorStatusMap;
    }

    /* ########################## 3DS METHODS ########################## */
    /**
     * Check 3DS authorization according to this reference
     * https://developer.paypal.com/docs/checkout/advanced/customize/3d-secure/response-parameters/
     * LiabilityShift : POSSIBLE,NO,UNKNOWN
     * @param jsonResponse
     * @return
     */
    public static Map<String, Object> verify3DSAuthorization(JsonObject jsonResponse) {

        Map<String, Object> returnMap = new HashMap<>();

        JsonObject paymentSourceElem = (JsonObject) jsonResponse.get("payment_source");
        JsonObject cardObject = (JsonObject) paymentSourceElem.get("card");

        JsonObject authenticationResultObject = (JsonObject) cardObject.get("authentication_result");
        JsonElement liabilityShiftElem = authenticationResultObject.get("liability_shift");
        JsonObject threedsecureObject = (JsonObject) authenticationResultObject.get("three_d_secure");
        JsonElement enrollStatusElem = threedsecureObject.get("enrollment_status");
        JsonElement authStatusElem = threedsecureObject.get("authentication_status");
        String liabilityShift = (liabilityShiftElem != null) ? liabilityShiftElem.getAsString() : "UNKNOWN";
        String enrollmentStatus = (enrollStatusElem != null) ? enrollStatusElem.getAsString() : "-";
        String authenticationStatus = (authStatusElem != null) ? authStatusElem.getAsString() : "-";

        if ("POSSIBLE".equals(liabilityShift)) {
            returnMap.put("3DS_VERIFIED", true);
        } else if("NO".equals(liabilityShift)) {
            //Check EnrollmentStatus
            if("N".equals(enrollmentStatus) || "B".equals(enrollmentStatus) || "U".equals(enrollmentStatus)) {
                returnMap.put("3DS_VERIFIED", true);
            } else if("Y".equals(enrollmentStatus)) {
                returnMap.put("3DS_VERIFIED", false);
            }

        } else if ("UNKNOWN".equals(liabilityShift)) {
            returnMap.put("3DS_VERIFIED", false);
        }

        returnMap.put("LIABILITY_SHIFT", liabilityShift);
        returnMap.put("ENROLLMENT_STATUS", enrollmentStatus);
        returnMap.put("AUTHENTICATION_STATUS", authenticationStatus);

        return returnMap;
    }



    /* ########################## MISC UTILITIES ########################## */
    /**
     * Returns configured PayPal Cancel URL.
     *
     * @param delegator
     * @return
     */
    public static String getPaypalCancelUrl(Delegator delegator) {
        return getPaypalCancelUrl(PaypalClientHelper.getPaypalEnvironment(delegator), delegator);
    }

    /**
     * Returns configured PayPal Return URL.
     *
     * @param delegator
     * @return
     */
    public static String getPaypalReturnUrl(Delegator delegator) {
        return getPaypalReturnUrl(PaypalClientHelper.getPaypalEnvironment(delegator), delegator);
    }

    /**
     * Returns the backoffice orderId and statusId of an order associated to the
     * input PayPal Order Id.
     *
     * @param paypalOrderId
     * @param delegator
     * @return orderId, statusId
     */
    public static Map<String, Object> getOrderDataFromPaypalOrderId(String paypalOrderId, Delegator delegator) {

        Map<String, Object> returnMap = null;
        List<GenericValue> orderHeaderList = null;

        try {
            EntityCondition cond = EntityCondition.makeCondition("mpPayPalOrderId", EntityOperator.EQUALS, paypalOrderId);
            orderHeaderList = delegator.findList("OrderHeader", cond, null, null, null, false);
        } catch (GenericEntityException gee) {
            String msg = "Error in retrieving list of Order Header for PayPal Order Id [" + paypalOrderId + "]";
            //Debug.logError(msg, module);
            returnMap = ServiceUtil.returnError(msg);
            returnMap.put("_EVENT_RETURN_", "error");
            return returnMap;
        }

        if (orderHeaderList == null || orderHeaderList.isEmpty()) {
            String msg = "No order found associated to PayPal Order Id [" + paypalOrderId + "]. Stop processing.";
            //Debug.logError(msg, module);
            returnMap = ServiceUtil.returnError(msg);
            returnMap.put("_EVENT_RETURN_", "ignore");
            return returnMap;
        }

        /*
         * Many orders found associsted to the same PayPal Order ID. Which one we should choose?
         * For now, stop processing and return an error.
         */
        if (orderHeaderList.size() > 1) {
            String msg = "Many orders found associated to the same PayPal Order ID [" + paypalOrderId + "]. Stop processing.";
            //Debug.logError(msg, module);
            returnMap = ServiceUtil.returnError(msg);
            returnMap.put("_EVENT_RETURN_", "error");
            return returnMap;
        }

        GenericValue orderHeader = orderHeaderList.get(0);
        String orderId = orderHeader.getString("orderId");
        String orderStatus = orderHeader.getString("statusId");
        returnMap = ServiceUtil.returnSuccess();
        returnMap.put("orderId", orderId);
        returnMap.put("statusId", orderStatus);
        return returnMap;
    }

    /**
     *
     * @param orderId
     * @param delegator
     * @return
     */
    public static GenericValue getOrderHeaderFromId(String orderId, Delegator delegator) {
        GenericValue orderHeader = null;
        if (orderId == null || UtilValidate.isEmpty(orderId)) {
            Debug.logError("Order Id is null or empty. Cannot return OrderHeader data.", module);
            return null;
        }
        try {
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            return null;
        }
        return orderHeader;
    }

    /**
     * Get the "system" default UserLogin record.
     *
     * @param delegator
     * @return
     */
    public static GenericValue getSystemUserLogin(Delegator delegator) {
        GenericValue systemUserLogin = null;
        try {
            systemUserLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false);
        } catch (GenericEntityException gee) {
            Debug.logError("Error in retrieving system user login record. Error is => " + gee.getMessage(), module);
            return null;
        }
        return systemUserLogin;
    }

    /**
     * Get the username stored in SystemProperties to be used for service
     * authentication.
     *
     * @param delegator
     * @return
     */
    public static String getServiceUsername(Delegator delegator) {
        //return EntityUtilProperties.getPropertyValue(MP_SYSTEM_RESOURCE_ID, "serviceUsername", "", delegator);
        return EntityUtilProperties.getPropertyValue(MPOMNI_SYSTEM_RESOURCE_ID, "serviceUsername", "", delegator);
    }

    /**
     * Get the password stored in SystemProperties to be used for service
     * authentication.
     *
     * @param delegator
     * @return
     */
    public static String getServicePassword(Delegator delegator) {
        //return EntityUtilProperties.getPropertyValue(MP_SYSTEM_RESOURCE_ID, "servicePassword", delegator);
        return EntityUtilProperties.getPropertyValue(MPOMNI_SYSTEM_RESOURCE_ID, "servicePassword", delegator);
    }

    /**
     * Trim String value to required length if necessary.
     *
     * @param fieldValue
     * @param fieldMaxLength
     * @return
     */
    public static String trimFieldToLength(String fieldValue, int fieldMaxLength) {
        return (fieldValue != null && fieldValue.length() > fieldMaxLength) ? fieldValue.substring(0, fieldMaxLength) : fieldValue;
    }

    /* ####################### BEAN OBJECT BUILDING METHODS #######################*/
    /**
     * Method that builds a Java bean Object that will be converted to a JSON
     * object specific for Webhook Validation Signature REST API Request.
     *
     * @param headersMap
     * @param requestBody
     * @param delegator
     * @return
     */
    @SuppressWarnings("unchecked")
    public static WebHookValidateRequestObject buildWebHookValidateRequestObject(HashMap<String, String> headersMap, HashMap<String, Object> requestBodyMap, Delegator delegator) {
        WebHookValidateRequestObject webHookValReqObj = null;
        if (headersMap == null || headersMap.isEmpty()) {
            Debug.logError("Request Header Map is null or empty. Cannot create WebHookValidate object.", module);
            return null;
        }
        if (requestBodyMap == null || requestBodyMap.isEmpty()) {
            Debug.logError("Request body payload map is null or empty. Cannot create WebHookValidate object.", module);
            return null;
        }

        webHookValReqObj = new WebHookValidateRequestObject();

        //Get header data required for webhook validation
        String auth_algo = headersMap.get("paypal-auth-algo");
        String cert_url = headersMap.get("paypal-cert-url");
        String transmission_id = headersMap.get("paypal-transmission-id");
        String transmission_sig = headersMap.get("paypal-transmission-sig");
        String transmission_time = headersMap.get("paypal-transmission-time");
        String webhookId = PaypalClientHelper.getPaypalWebHookId(delegator);

        webHookValReqObj.setAuth_algo(auth_algo);
        webHookValReqObj.setCert_url(cert_url);
        webHookValReqObj.setTransmission_id(transmission_id);
        webHookValReqObj.setTransmission_sig(transmission_sig);
        webHookValReqObj.setTransmission_time(transmission_time);
        webHookValReqObj.setWebhook_id(webhookId);

        WebHookValidateRequestObject.WebHookEvent wh_event = new WebHookValidateRequestObject.WebHookEvent();

        wh_event.setCreate_time((String) requestBodyMap.get("create_time"));
        wh_event.setEvent_type((String) requestBodyMap.get("event_type"));
        wh_event.setEvent_version((String) requestBodyMap.get("event_version"));
        wh_event.setId((String) requestBodyMap.get("id"));
        wh_event.setResource_type((String) requestBodyMap.get("resource_type"));
        wh_event.setResource_version((String) requestBodyMap.get("resource_version"));
        wh_event.setSummary((String) requestBodyMap.get("summary"));
        //Set the "resource" field
        wh_event.setResource((LinkedHashMap<String, String>) requestBodyMap.get("resource"));
        ArrayList<LinkedHashMap<String, Object>> linksList = (ArrayList<LinkedHashMap<String, Object>>) requestBodyMap.get("links");
        for (LinkedHashMap<String, Object> _linkElem : linksList) {
            WebHookValidateRequestObject.LinkDescription _linkObj = new WebHookValidateRequestObject.LinkDescription();
            _linkObj.setMethod((String) _linkElem.get("method"));
            _linkObj.setHref((String) _linkElem.get("href"));
            _linkObj.setRel((String) _linkElem.get("rel"));
            wh_event.getLinks().add(_linkObj);
        }
        webHookValReqObj.setWebhook_event(wh_event);
        return webHookValReqObj;
    }

    public static OrderRequestObject buildOrderRequestObject(String orderId, String paymentMethod, HttpServletRequest request, Delegator delegator) {
        if (paymentMethod == null) {
            return buildWalletOrderRequestObject(orderId, request, delegator);
        } else {
            Debug.logError("PayPal Payment method [" + paymentMethod + "] not supported. Returning null.", module);
        }
        return null;
    }

    public static OrderRequestObject buildOrder3DSHostedRequestObject(String orderId, HashMap<String, String> hostedFieldsMap, String paymentMethod, HttpServletRequest request, Delegator delegator) {
        if (paymentMethod != null && paymentMethod.equals("hosted-fields")) {
            return build3DSCardOrderRequestObject(orderId, hostedFieldsMap, request, delegator);
        } else {
            Debug.logError("PayPal Payment method [" + paymentMethod + "] not supported. Returning null.", module);
        }
        return null;
    }

    /**
     * Method that builds a Java bean Object that will be converted to a JSON
     * object specific for Order Creation REST API Request.
     *
     * @param orderId
     * @param request
     * @param delegator
     * @return
     */
    public static OrderRequestObject buildWalletOrderRequestObject(String orderId, HttpServletRequest request, Delegator delegator) {

        OrderRequestObject oro = null;

        /* Get the orderHeader and OrderReadHelper */
        GenericValue orderHeader = null;
        OrderReadHelper orh = null;

        try {
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        } catch (GenericEntityException gee) {
            String msg = "Error in retrieving OrderHeader for order [" + orderId + "]. Msg => " + gee.getMessage();
            Debug.logError(msg, module);
            return null;
        }

        orh = new OrderReadHelper(orderHeader);

        oro = new OrderRequestObject();

        OrderRequestObject.PurchaseUnit oro_punit = new OrderRequestObject.PurchaseUnit();

        OrderRequestObject.Amount oro_amount = new OrderRequestObject.Amount();
        oro_amount.setCurrency_code(orh.getCurrency());
        oro_amount.setValue(orh.getOrderGrandTotal().toPlainString());

        //Amount Breakdown (per Order Total)
        OrderRequestObject.AmountBreakdown oro_amount_brkdwn = new OrderRequestObject.AmountBreakdown();
        //Total adjustments (including shipping charges)
        BigDecimal orderDiscountTotal = BigDecimal.ZERO;
        BigDecimal orderItemSubTotal = orh.getOrderItemsSubTotal();
        BigDecimal shippingTotal = orh.getShippingTotal();
        BigDecimal taxTotal = orh.getTaxTotal();
        List<GenericValue> allAdjustments = orh.getAdjustments();

        //Check if there are any shipping charge discounts promo
        BigDecimal shipChargeDiscountTotal = BigDecimal.ZERO;
        for (GenericValue orderAdj : allAdjustments) {
            if (orderAdj.getString("orderAdjustmentTypeId").equals("SHIPPING_CHARGES")) {
                BigDecimal amount = (BigDecimal) orderAdj.get("amount");
                //Keep only negative amounts (means discount of Shipping Charges)
                if (amount.compareTo(BigDecimal.ZERO) == -1) {
                    //Discount cannot be a negative number so negate the amount to have a positive value
                    shipChargeDiscountTotal = shipChargeDiscountTotal.add(amount.negate());
                }
            } else {
                //Sum up all the other order adjustments (skip item line adjustments, they are already considered in item subtotal)
                if ( orderAdj.get("orderItemSeqId") != null &&
                        !"_NA_".equals((String) orderAdj.get("orderItemSeqId")) ) {
                    continue;
                }
                BigDecimal amount = (BigDecimal) orderAdj.get("amount");
                if (amount.compareTo(BigDecimal.ZERO) == -1) {
                    orderDiscountTotal = orderDiscountTotal.add(amount.negate());
                }
            }
        }

        if (orderDiscountTotal.compareTo(BigDecimal.ZERO) == 1) {
            OrderRequestObject.Money oro_discount_tot = new OrderRequestObject.Money();
            oro_discount_tot.setCurrency_code(orh.getCurrency());
            oro_discount_tot.setValue(orderDiscountTotal.setScale(2).toPlainString());
            oro_amount_brkdwn.setDiscount(oro_discount_tot);
        }

        OrderRequestObject.Money oro_items_subtot = new OrderRequestObject.Money();
        oro_items_subtot.setCurrency_code(orh.getCurrency());
        oro_items_subtot.setValue(orderItemSubTotal.toPlainString());
        oro_amount_brkdwn.setItem_total(oro_items_subtot);

        OrderRequestObject.Money oro_order_shipping_charge = new OrderRequestObject.Money();
        oro_order_shipping_charge.setCurrency_code(orh.getCurrency());
        oro_order_shipping_charge.setValue(shippingTotal.toPlainString());
        oro_amount_brkdwn.setShipping(oro_order_shipping_charge);

        if (shipChargeDiscountTotal.compareTo(BigDecimal.ZERO) == 1) {
            OrderRequestObject.Money oro_order_shipping_charge_disc = new OrderRequestObject.Money();
            oro_order_shipping_charge_disc.setCurrency_code(orh.getCurrency());
            oro_order_shipping_charge_disc.setValue(shipChargeDiscountTotal.toPlainString());
            oro_amount_brkdwn.setShipping_discount(oro_order_shipping_charge_disc);
        }

        if (taxTotal.compareTo(BigDecimal.ZERO) == 1) {
            OrderRequestObject.Money oro_order_shipping_tax = new OrderRequestObject.Money();
            oro_order_shipping_tax.setCurrency_code(orh.getCurrency());
            oro_order_shipping_tax.setValue(taxTotal.toPlainString());
            oro_amount_brkdwn.setTax_total(oro_order_shipping_tax);
        }

        oro_amount.setBreakdown(oro_amount_brkdwn);

        oro_punit.setAmount(oro_amount);

        oro_punit.setCustom_id(PaypalEventsHelper.trimFieldToLength(orderId, 127));
        oro_punit.setDescription(PaypalEventsHelper.trimFieldToLength("Payment for order " + orderId, 127));
        oro_punit.setInvoice_id(PaypalEventsHelper.trimFieldToLength(orderId, 127));

        //OrderItems
        List<GenericValue> orderItemList = orh.getOrderItemsByCondition(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CREATED"));

        for (GenericValue orderItem : orderItemList) {

            OrderRequestObject.PurchaseOrderItem oro_poitem = new OrderRequestObject.PurchaseOrderItem();
            String sku = (String) orderItem.get("productId");

            //Get product name
            GenericValue parentProduct = ProductWorker.getParentProduct(sku, delegator);
            ProductContentWrapper pcw = ProductContentWrapper.makeProductContentWrapper(parentProduct, request);
            String productName = pcw.get("PRODUCT_NAME", "html").toString();
            String longDescription = pcw.get("LONG_DESCRIPTION", "html").toString();

            oro_poitem.setName(PaypalEventsHelper.trimFieldToLength(productName, 127));
            oro_poitem.setDescription(PaypalEventsHelper.trimFieldToLength(longDescription, 127));
            oro_poitem.setSku(PaypalEventsHelper.trimFieldToLength(sku, 127));

            BigDecimal qty = orh.getItemPendingShipmentQuantity(orderItem);
            BigDecimal itemPrice = orderItem.getBigDecimal("unitPrice").setScale(2);

            BigDecimal itemSubTotal = orh.getOrderItemSubTotal(orderItem);

            OrderRequestObject.Money oro_poitem_unit_amount = new OrderRequestObject.Money();
            oro_poitem_unit_amount.setCurrency_code(orh.getCurrency());
            BigDecimal itemUnitPrice = itemPrice.setScale(2);
            oro_poitem_unit_amount.setValue(itemSubTotal.toPlainString());
            oro_poitem.setUnit_amount(oro_poitem_unit_amount);
            oro_poitem.setQuantity(Integer.toString(qty.intValue()));

            oro_poitem.setCategory(PaypalRequestEnum.REQ_PURCH_ITEM_CATEGORY_PG);

            oro_punit.getItems().add(oro_poitem);
        }

        //Payee
        String environment = PaypalClientHelper.getPaypalEnvironment(delegator);
        OrderRequestObject.Payee oro_payee = new OrderRequestObject.Payee();
        String merchantBusinessEmail = PaypalClientHelper.getPaypalMerchantBusinessEmail(delegator);
        if(merchantBusinessEmail != null && !merchantBusinessEmail.trim().isEmpty())
        {
            oro_payee.setEmail_address(merchantBusinessEmail);
        }

        String accountId = PaypalClientHelper.getPaypalMerchantAccountId(environment,delegator);
        if(accountId != null && !accountId.trim().isEmpty())
        {
            oro_payee.setMerchant_id(accountId);
        }

        oro_punit.setPayee(oro_payee);

        //Payment Instruction
        OrderRequestObject.PaymentInstruction oro_paymins = new OrderRequestObject.PaymentInstruction();
        oro_paymins.setDisbursement_mode(PaypalRequestEnum.PAYMENT_DISBURSEMENT_MODE_INSTANT);
        oro_punit.setPayment_instruction(oro_paymins);

        oro_punit.setReference_id(orderId);

        //Shipping Data
        OrderRequestObject.Shipping oro_shipping = new OrderRequestObject.Shipping();
        OrderRequestObject.Address oro_shipping_address = new OrderRequestObject.Address();
        OrderRequestObject.CustomerName oro_shipping_customer = new OrderRequestObject.CustomerName();

        List<GenericValue> itemShipGroups = orh.getOrderItemShipGroups();
        GenericValue firstShipGroup = EntityUtil.getFirst(itemShipGroups);
        String orderItemShipGroupSeqId = (String) firstShipGroup.get("shipGroupSeqId");
        GenericValue orderShippingAddress = orh.getShippingAddress(orderItemShipGroupSeqId);
        String address1 = (String) orderShippingAddress.get("address1");
        String city = (String) orderShippingAddress.get("city");
        String toName = (String) orderShippingAddress.get("toName");
        String postalCode = (String) orderShippingAddress.get("postalCode");
        String countryGeoId_iso3 = (String) orderShippingAddress.get("countryGeoId");
        String stateProvinceGeoId = (String) orderShippingAddress.get("stateProvinceGeoId");
        String province = null;
        if (stateProvinceGeoId == null || "_NA_".equals(stateProvinceGeoId)) {
            province = "";
        } else if (stateProvinceGeoId.contains("-")) {
            province = stateProvinceGeoId.substring(stateProvinceGeoId.indexOf("-") + 1);
        }

        GenericValue countryGeo = null;

        try {
            countryGeo = delegator.findOne("Geo", UtilMisc.toMap("geoId", countryGeoId_iso3), true);
        } catch (GenericEntityException gee) {
            String msg = "Error in retrieving Geo with id [" + countryGeoId_iso3 + "] Cannot build request object. Error is => " + gee.getMessage();
            Debug.logError(msg, module);
            return null;
        }

        String countryGeoId_iso2 = (String) countryGeo.get("geoCode");

        oro_shipping_address.setCountry_code(countryGeoId_iso2);
        oro_shipping_address.setAddress_line_1(address1);
        oro_shipping_address.setAdmin_area_1(province);
        oro_shipping_address.setAdmin_area_2(city);
        oro_shipping_address.setPostal_code(postalCode);

        //Set customer name
        oro_shipping_customer.setFull_name(toName);

        oro_shipping.setAddress(oro_shipping_address);
        oro_shipping.setName(oro_shipping_customer);
        oro_shipping.setType(PaypalRequestEnum.SHIP_DETAIL_TYPE_SHIPPING);

        oro_punit.setShipping(oro_shipping);

        //Build the payment_source (ex application_context)
        OrderRequestObject.PaymentSource oro_paym_src = new OrderRequestObject.PaymentSource();

        OrderRequestObject.PayPalWallet oro_paym_paypal = new OrderRequestObject.PayPalWallet();

        oro_paym_paypal.setEmail_address(orh.getOrderEmailString());

        OrderRequestObject.PayPalWalletName oro_paypal_name = new OrderRequestObject.PayPalWalletName();

        GenericValue orderPlacingParty = orh.getPlacingParty();
        String partyName = PartyHelper.getPartyName(orderPlacingParty);
        oro_paypal_name.setGiven_name(PaypalEventsHelper.trimFieldToLength(partyName, 140));
        oro_paym_paypal.setName(oro_paypal_name);

        //Experience Context (Cancel URL, Return URL,..)
        OrderRequestObject.ExperienceContext oro_exp_ctx = new OrderRequestObject.ExperienceContext();

        String cancelUrl = PaypalEventsHelper.getPaypalCancelUrl(delegator);
        String returnUrl = PaypalEventsHelper.getPaypalReturnUrl(delegator);
        oro_exp_ctx.setCancel_url(cancelUrl);
        oro_exp_ctx.setReturn_url(returnUrl);
        oro_exp_ctx.setUser_action(PaypalRequestEnum.EXP_CONTEXT_USER_ACTION_PAYNOW);
        oro_exp_ctx.setPayment_method_preference(PaypalRequestEnum.EXP_CONTEXT_PAYMENT_PREF_IMMED);
        oro_paym_paypal.setExperience_context(oro_exp_ctx);

        oro_paym_src.setPaypal(oro_paym_paypal);
        oro.setPayment_source(oro_paym_src);

        //Add the purchase unit (an order) to the list
        oro.getPurchase_units().add(oro_punit);
        oro.setIntent(PaypalRequestEnum.REQ_INTENT_CAPTURE);

        return oro;
    }

    /**
     *
     * @param orderId
     * @param hostedFieldsMap
     * @param request
     * @param delegator
     * @return
     */
    private static OrderRequestObject build3DSCardOrderRequestObject(String orderId, HashMap<String, String> hostedFieldsMap, HttpServletRequest request, Delegator delegator) {

        OrderRequestObject oro = null;

        /* Get the orderHeader and OrderReadHelper */
        GenericValue orderHeader = null;
        OrderReadHelper orh = null;

        try {
            orderHeader =  delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        } catch (GenericEntityException gee) {
            String msg = "Error in retrieving OrderHeader for order [" + orderId + "]. Msg => " + gee.getMessage();
            Debug.logError(msg, module);
            return null;
        }

        orh = new OrderReadHelper(orderHeader);

        oro = new OrderRequestObject();

        OrderRequestObject.PurchaseUnit oro_punit = new OrderRequestObject.PurchaseUnit();

        OrderRequestObject.Amount oro_amount = new OrderRequestObject.Amount();
        oro_amount.setCurrency_code(orh.getCurrency());
        oro_amount.setValue(orh.getOrderGrandTotal().toPlainString());

        //Amount Breakdown (per Order Total)
        OrderRequestObject.AmountBreakdown oro_amount_brkdwn = new OrderRequestObject.AmountBreakdown();
        //Total adjustments (including shipping charges)
        BigDecimal orderDiscountTotal = BigDecimal.ZERO;
        BigDecimal orderItemSubTotal = orh.getOrderItemsSubTotal();
        BigDecimal shippingTotal = orh.getShippingTotal();
        BigDecimal taxTotal = orh.getTaxTotal();
        List<GenericValue> allAdjustments = orh.getAdjustments();

        //Check if there are any shipping charge discounts promo
        BigDecimal shipChargeDiscountTotal = BigDecimal.ZERO;
        for (GenericValue orderAdj : allAdjustments) {
            if (orderAdj.getString("orderAdjustmentTypeId").equals("SHIPPING_CHARGE")) {
                BigDecimal amount = (BigDecimal) orderAdj.get("amount");
                //Keep only negative amounts (means discount of Shipping Charges)
                if(amount.compareTo(BigDecimal.ZERO) == -1) {
                    //Discount cannot be a negative number so negate the amount to have a positive value
                    shipChargeDiscountTotal = shipChargeDiscountTotal.add(amount.negate());
                }
            } else {
                //Sum up all the other order adjustments (skip item line adjustments, they are already considered in item subtotal)
                if ( orderAdj.get("orderItemSeqId") != null &&
                        !"_NA_".equals((String) orderAdj.get("orderItemSeqId")) ) {
                    continue;
                }
                BigDecimal amount = (BigDecimal) orderAdj.get("amount");
                if(amount.compareTo(BigDecimal.ZERO) == -1) {
                    orderDiscountTotal = orderDiscountTotal.add(amount.negate());
                }
            }
        }

        if (orderDiscountTotal.compareTo(BigDecimal.ZERO) == 1) {
            OrderRequestObject.Money oro_discount_tot = new OrderRequestObject.Money();
            oro_discount_tot.setCurrency_code(orh.getCurrency());
            oro_discount_tot.setValue(orderDiscountTotal.setScale(2).toPlainString());
            oro_amount_brkdwn.setDiscount(oro_discount_tot);
        }

        OrderRequestObject.Money oro_items_subtot = new OrderRequestObject.Money();
        oro_items_subtot.setCurrency_code(orh.getCurrency());
        oro_items_subtot.setValue(orderItemSubTotal.toPlainString());
        oro_amount_brkdwn.setItem_total(oro_items_subtot);

        OrderRequestObject.Money oro_order_shipping_charge = new OrderRequestObject.Money();
        oro_order_shipping_charge.setCurrency_code(orh.getCurrency());
        oro_order_shipping_charge.setValue(shippingTotal.toPlainString());
        oro_amount_brkdwn.setShipping(oro_order_shipping_charge);

        if (shipChargeDiscountTotal.compareTo(BigDecimal.ZERO) == 1) {
            OrderRequestObject.Money oro_order_shipping_charge_disc = new OrderRequestObject.Money();
            oro_order_shipping_charge_disc.setCurrency_code(orh.getCurrency());
            oro_order_shipping_charge_disc.setValue(shipChargeDiscountTotal.toPlainString());
            oro_amount_brkdwn.setShipping_discount(oro_order_shipping_charge_disc);
        }

        if (taxTotal.compareTo(BigDecimal.ZERO) == 1) {
            OrderRequestObject.Money oro_order_shipping_tax = new OrderRequestObject.Money();
            oro_order_shipping_tax.setCurrency_code(orh.getCurrency());
            oro_order_shipping_tax.setValue(taxTotal.toPlainString());
        }

        oro_amount.setBreakdown(oro_amount_brkdwn);

        oro_punit.setAmount(oro_amount);

        oro_punit.setCustom_id(PaypalEventsHelper.trimFieldToLength(orderId, 127));
        oro_punit.setDescription(PaypalEventsHelper.trimFieldToLength("Payment for order " + orderId, 127));
        oro_punit.setInvoice_id(PaypalEventsHelper.trimFieldToLength(orderId, 127));

        //OrderItems
        List<GenericValue> orderItemList = orh.getOrderItemsByCondition(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CREATED"));

        for (GenericValue orderItem : orderItemList) {

            OrderRequestObject.PurchaseOrderItem oro_poitem = new OrderRequestObject.PurchaseOrderItem();
            String sku = (String) orderItem.get("productId");

            //Get product name
            GenericValue parentProduct = ProductWorker.getParentProduct(sku, delegator);
            ProductContentWrapper pcw = ProductContentWrapper.makeProductContentWrapper(parentProduct, request);
            String productName = pcw.get("PRODUCT_NAME", "html").toString();
            String longDescription = pcw.get("LONG_DESCRIPTION", "html").toString();

            oro_poitem.setName(PaypalEventsHelper.trimFieldToLength(productName, 127));
            oro_poitem.setDescription(PaypalEventsHelper.trimFieldToLength(longDescription, 127));
            oro_poitem.setSku(PaypalEventsHelper.trimFieldToLength(sku, 127));

            BigDecimal qty = orh.getItemPendingShipmentQuantity(orderItem);
            BigDecimal itemPrice = orderItem.getBigDecimal("unitPrice").setScale(2);
            BigDecimal itemSubTotal = orh.getOrderItemSubTotal(orderItem);

            OrderRequestObject.Money oro_poitem_unit_amount = new OrderRequestObject.Money();
            oro_poitem_unit_amount.setCurrency_code(orh.getCurrency());
            BigDecimal itemUnitPrice = itemPrice.setScale(2);
            oro_poitem_unit_amount.setValue(itemSubTotal.toPlainString());
            oro_poitem.setUnit_amount(oro_poitem_unit_amount);
            oro_poitem.setQuantity(Integer.toString(qty.intValue()));

            oro_poitem.setCategory(PaypalRequestEnum.REQ_PURCH_ITEM_CATEGORY_PG);

            oro_punit.getItems().add(oro_poitem);
        }

        //Payee
        String environment = PaypalClientHelper.getPaypalEnvironment(delegator);
        OrderRequestObject.Payee oro_payee = new OrderRequestObject.Payee();
        String merchantBusinessEmail = PaypalClientHelper.getPaypalMerchantBusinessEmail(delegator);
        if(merchantBusinessEmail != null && !merchantBusinessEmail.trim().isEmpty())
        {
            oro_payee.setEmail_address(merchantBusinessEmail);
        }

        String accountId = PaypalClientHelper.getPaypalMerchantAccountId(environment,delegator);
        if(accountId != null && !accountId.trim().isEmpty())
        {
            oro_payee.setMerchant_id(accountId);
        }

        oro_punit.setPayee(oro_payee);

        //Payment Instruction
        OrderRequestObject.PaymentInstruction oro_paymins = new OrderRequestObject.PaymentInstruction();
        oro_paymins.setDisbursement_mode(PaypalRequestEnum.PAYMENT_DISBURSEMENT_MODE_INSTANT);
        oro_punit.setPayment_instruction(oro_paymins);

        oro_punit.setReference_id(orderId);

        //Shipping Data
        OrderRequestObject.Shipping oro_shipping = new OrderRequestObject.Shipping();
        OrderRequestObject.Address oro_shipping_address = new OrderRequestObject.Address();
        OrderRequestObject.CustomerName oro_shipping_customer = new OrderRequestObject.CustomerName();

        List<GenericValue> itemShipGroups = orh.getOrderItemShipGroups();
        GenericValue firstShipGroup = EntityUtil.getFirst(itemShipGroups);
        String orderItemShipGroupSeqId = (String) firstShipGroup.get("shipGroupSeqId");
        GenericValue orderShippingAddress = orh.getShippingAddress(orderItemShipGroupSeqId);
        String address1 = (String) orderShippingAddress.get("address1");
        String city = (String) orderShippingAddress.get("city");
        String toName = (String) orderShippingAddress.get("toName");
        String postalCode = (String) orderShippingAddress.get("postalCode");
        String countryGeoId_iso3 = (String) orderShippingAddress.get("countryGeoId");
        String stateProvinceGeoId = (String) orderShippingAddress.get("stateProvinceGeoId");
        String province = null;
        if (stateProvinceGeoId == null || "_NA_".equals(stateProvinceGeoId)) {
            province = "";
        } else if (stateProvinceGeoId.contains("-")) {
            province = stateProvinceGeoId.substring(stateProvinceGeoId.indexOf("-") + 1);
        }

        GenericValue countryGeo = null;

        try {
            countryGeo = delegator.findOne("Geo", UtilMisc.toMap("geoId", countryGeoId_iso3), true);
        } catch (GenericEntityException gee) {
            String msg = "Error in retrieving Geo with id [" + countryGeoId_iso3 + "] Cannot build request object. Error is => " + gee.getMessage();
            Debug.logError(msg, module);
            return null;
        }

        String countryGeoId_iso2 = (String) countryGeo.get("geoCode");

        oro_shipping_address.setCountry_code(countryGeoId_iso2);
        oro_shipping_address.setAddress_line_1(address1);
        oro_shipping_address.setAdmin_area_1(province);
        oro_shipping_address.setAdmin_area_2(city);
        oro_shipping_address.setPostal_code(postalCode);

        //Set customer name
        oro_shipping_customer.setFull_name(toName);

        oro_shipping.setAddress(oro_shipping_address);
        oro_shipping.setName(oro_shipping_customer);
        oro_shipping.setType(PaypalRequestEnum.SHIP_DETAIL_TYPE_SHIPPING);

        oro_punit.setShipping(oro_shipping);

        //Build the "card" payment source
        OrderRequestObject.PaymentSource oro_paym_src = new OrderRequestObject.PaymentSource();

        OrderRequestObject.PayPalCard oro_paym_card = new OrderRequestObject.PayPalCard();

        if (hostedFieldsMap.get("cardHolderName") != null && !hostedFieldsMap.get("cardHolderName").trim().isEmpty()) {
            oro_paym_card.setName(hostedFieldsMap.get("cardHolderName"));
        }

        //Card Billing Address
        if(hostedFieldsMap.get("cardStreetAddress") != null && !hostedFieldsMap.get("cardStreetAddress").trim().isEmpty() ||
                hostedFieldsMap.get("cardExtendedAddress") != null && !hostedFieldsMap.get("cardExtendedAddress").trim().isEmpty() ||
                hostedFieldsMap.get("cardAddressCountryCodeAlpha2") != null && !hostedFieldsMap.get("cardAddressCountryCodeAlpha2").trim().isEmpty() ||
                hostedFieldsMap.get("cardAddressPostalCode") != null && !hostedFieldsMap.get("cardAddressPostalCode").trim().isEmpty() ||
                hostedFieldsMap.get("cardAddressCity") != null && !hostedFieldsMap.get("cardAddressCity").trim().isEmpty()) {

            OrderRequestObject.Address oro_paym_card_bill_addr = new OrderRequestObject.Address();
            if (hostedFieldsMap.get("cardStreetAddress") != null && !hostedFieldsMap.get("cardStreetAddress").trim().isEmpty()) {
                oro_paym_card_bill_addr.setAddress_line_1(hostedFieldsMap.get("cardStreetAddress"));
            }
            if (hostedFieldsMap.get("cardExtendedAddress") != null && !hostedFieldsMap.get("cardExtendedAddress").trim().isEmpty()) {
                oro_paym_card_bill_addr.setAddress_line_2(hostedFieldsMap.get("cardExtendedAddress"));
            }
            if (hostedFieldsMap.get("cardAddressCountryCodeAlpha2") != null && !hostedFieldsMap.get("cardAddressCountryCodeAlpha2").trim().isEmpty()) {
                oro_paym_card_bill_addr.setCountry_code(hostedFieldsMap.get("cardAddressCountryCodeAlpha2").toUpperCase());
            }
            if (hostedFieldsMap.get("cardAddressPostalCode") != null && !hostedFieldsMap.get("cardAddressPostalCode").trim().isEmpty()) {
                oro_paym_card_bill_addr.setPostal_code(hostedFieldsMap.get("cardAddressPostalCode"));
            }
            if (hostedFieldsMap.get("cardAddressCity") != null && !hostedFieldsMap.get("cardAddressCity").trim().isEmpty()) {
                oro_paym_card_bill_addr.setAdmin_area_2(hostedFieldsMap.get("cardAddressCity"));
            }
            oro_paym_card.setBilling_address(oro_paym_card_bill_addr);
        }

        //Card Attributes
        OrderRequestObject.Attributes oro_paym_card_attr = new OrderRequestObject.Attributes();
        OrderRequestObject.VerificationAttribute oro_paym_card_verification = new OrderRequestObject.VerificationAttribute();
        oro_paym_card_verification.setMethod("SCA_ALWAYS");
        oro_paym_card_attr.setVerification(oro_paym_card_verification);

        oro_paym_card.setAttributes(oro_paym_card_attr);

        //Experience Context (Cancel URL, Return URL,..)
        OrderRequestObject.ExperienceContext oro_exp_ctx = new OrderRequestObject.ExperienceContext();

        String cancelUrl = PaypalEventsHelper.getPaypalCancelUrl(delegator);
        String returnUrl = PaypalEventsHelper.getPaypalReturnUrl(delegator);
        oro_exp_ctx.setCancel_url(cancelUrl);
        oro_exp_ctx.setReturn_url(returnUrl);
        oro_exp_ctx.setUser_action(PaypalRequestEnum.EXP_CONTEXT_USER_ACTION_PAYNOW);
        oro_exp_ctx.setPayment_method_preference(PaypalRequestEnum.EXP_CONTEXT_PAYMENT_PREF_IMMED);

        oro_paym_card.setExperience_context(oro_exp_ctx);

        oro_paym_src.setCard(oro_paym_card);

        oro.setPayment_source(oro_paym_src);

        //Add the purchase unit (an order) to the list
        oro.getPurchase_units().add(oro_punit);
        oro.setIntent(PaypalRequestEnum.REQ_INTENT_CAPTURE);

        return oro;
    }

    /* ####################### PRIVATE METHODS ####################### */
    private static String getPaypalCancelUrl(String apiEnv, Delegator delegator) {
        return (PaypalClientHelper.PAYPAL_ENV_TEST.equals(apiEnv))
                ? EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "cancelUrl.test", delegator) : EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "cancelUrl.prod", delegator);
    }

    private static String getPaypalReturnUrl(String apiEnv, Delegator delegator) {
        return (PaypalClientHelper.PAYPAL_ENV_TEST.equals(apiEnv))
                ? EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "returnUrl.test", delegator) : EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "returnUrl.prod", delegator);
    }

} //end class
