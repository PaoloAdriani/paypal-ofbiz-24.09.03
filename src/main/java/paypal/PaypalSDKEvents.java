package paypal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.order.order.OrderChangeHelper;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import paypal.beans.OrderRequestObject;
import paypal.beans.PaypalRequestEnum;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PaypalSDKEvents {

    public static final String module = PaypalSDKEvents.class.getName();
    /**
     *
     * @param request
     * @param response
     * @return
     */
    public static String createOrderRequest(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        boolean requiredNewToken = false;
        boolean error = false;
        Map<String, Object> requestResponseMap = null;
        HashMap<String, String> jsonResponseFields = null;
        String eventReturn = "success";
        String jsonResponseString = null;
        String msg = "";

        String orderId = PaypalSDKEventsHelper.getOrderIdFromRequestBody(request);

        PaypalLogger logger = new PaypalLogger(delegator.getDelegatorTenantId());

        if(orderId == null) {
            msg = "createOrderRequest() - orderId not found in request. Cannot proceed with payment processing.";
            if (logger != null) logger.logError(msg);
            Debug.logError(msg, module);

            //Build a json response for the servlet
            jsonResponseFields = new HashMap<>();
            jsonResponseFields.put("error", "true");
            jsonResponseFields.put("ERROR_MSG", msg);
            jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

            // Writing the response and return error
            try {
                PrintWriter out = response.getWriter();
                response.setContentType("text/plain");
                out.println(jsonResponseString);
                out.close();
            } catch (IOException e) {
                msg = "createOrderRequest() - Error writing servlet output. Msg => " + e.getMessage();
                Debug.logError(msg, module);
                if (logger != null) logger.logError(msg);
                return "error";
            }
            return "error";
        }

        msg = "- Called createOrderRequest method of PaypalSDKEvents for order id ["+orderId+"] -";
        if (logger != null) logger.logInfo(msg);
        Debug.logInfo(msg, module);

        /* Get the orderHeader and OrderReadHelper */
        OrderRequestObject ordReqObj = PaypalEventsHelper.buildOrderRequestObject(orderId, null, request, delegator);

        //Object serialization into json
        Gson oreq_gson = new Gson();
        String oreq_jsonString = oreq_gson.toJson(ordReqObj);

        Debug.logWarning("Resulting json object => "+oreq_jsonString, module);
        msg = "Creating PayPal Order. JSON object => "+oreq_jsonString;
        if (logger != null) logger.logInfo(msg);

        requestResponseMap = PaypalClient.createPaypalOrder(oreq_jsonString, delegator);

        if (ServiceUtil.isSuccess(requestResponseMap)) {
            JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
            jsonResponseString = jsonResponse.toString();
            Debug.logInfo("Retrieved data => " + jsonResponseString, module);
            String payPalOrderStatus = PaypalEventsHelper.getOrderStatusFromJsonResponse(jsonResponse);
            String payPalOrderId = PaypalEventsHelper.getOrderIdFromJsonResponse(jsonResponse);

            msg = "PayPal Order Created with status: " + payPalOrderStatus + "and PayPal Order ID: " + payPalOrderId;
            if (logger != null) logger.logInfo(msg);
            Debug.logInfo(msg, module);

            PaypalEventsHelper.setOrderHeaderPayPalOrderId(payPalOrderId, orderId, delegator);
            String noteMsgOrderCreated = "PayPal Order created with ID => " + payPalOrderId + " and Status => " + payPalOrderStatus;
            if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderCreated, dispatcher, delegator)) {
                msg = "Error in creating OrderHeader note for order [" + orderId + "]";
                if (logger != null) logger.logError(msg);
                Debug.logError(msg, module);
            }
        } else {
            //Something went wrong during the request: check the http response code
            int http_response_code = (int) requestResponseMap.get("http_response_code");
            String http_response_message = (String) requestResponseMap.get("http_response_message");

            msg = "initOrderRequest() ERROR => Response code: " + http_response_code + ", Response message: " + http_response_message;
            if (logger != null) logger.logError(msg);
            Debug.logError(msg, module);

            if (401 == http_response_code) {
                requiredNewToken = true;
            } else if (http_response_code != 200) {
                //A general error occured
                error = true;
                eventReturn = "error";

                //Everytihing else should be a specific API issue
                JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
                jsonResponse.addProperty("error", "true");
                jsonResponseString = jsonResponse.toString();

                msg = "PAYPAL COMM. ERROR: " + jsonResponseString;
                if (logger != null) logger.logError(msg);

            }
        }

        if (!error && requiredNewToken) {

            msg = "A new OAuth Access Token is required: requesting a new one.";
            if (logger != null) logger.logInfo(msg);
            Debug.logInfo(msg, module);

            Map<String, Object> requestAPIAuthTokenMap = PaypalClient.requestAndStoreAccessToken(delegator);
            if (!ServiceUtil.isSuccess(requestAPIAuthTokenMap)) {
                msg = ServiceUtil.getErrorMessage(requestAPIAuthTokenMap);
                Debug.logError(msg, module);
                if (logger != null) logger.logError(msg);

                //Build a json response for the servlet
                jsonResponseFields = new HashMap<>();
                jsonResponseFields.put("error", "true");
                jsonResponseFields.put("ERROR_MSG", msg);
                jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

                try {
                    PrintWriter out = response.getWriter();
                    response.setContentType("text/plain");
                    out.println(jsonResponseString);
                    out.close();
                } catch (IOException e) {
                    msg = "createOrderRequest() - Error writing servlet output. Msg => " + e.getMessage();
                    Debug.logError(msg, module);
                    if (logger != null) logger.logError(msg);
                    return "error";
                }
                return "error";

            }

            /* Here I should have a new fresh token, so I'll retry the request. */
            Debug.logInfo("New Access Token generated: 2nd attempt for request createPaypalOrder", module);
            msg = "New Access Token generated: 2nd attempt to create PayPal Order...";
            if (logger != null) logger.logInfo(msg);

            requestResponseMap = PaypalClient.createPaypalOrder(oreq_jsonString, delegator);

            if (ServiceUtil.isSuccess(requestResponseMap)) {
                JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
                jsonResponseString = jsonResponse.toString();

                Debug.logInfo("Retrieved data => " + jsonResponseString, module);

                String payPalOrderStatus = PaypalEventsHelper.getOrderStatusFromJsonResponse(jsonResponse);
                String payPalOrderId = PaypalEventsHelper.getOrderIdFromJsonResponse(jsonResponse);

                msg = "PayPal Order Created with status: " + payPalOrderStatus + " and PayPal Order ID: " + payPalOrderId;
                if (logger != null) logger.logInfo(msg);
                Debug.logInfo(msg, module);

                PaypalEventsHelper.setOrderHeaderPayPalOrderId(payPalOrderId, orderId, delegator);

                String noteMsgOrderCreated = "PayPal Order created with ID => " + payPalOrderId + " and Status => " + payPalOrderStatus;
                if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderCreated, dispatcher, delegator)) {
                    Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
                }
            } else {
                error = true;
                eventReturn = "error";
                Debug.logError(ServiceUtil.getErrorMessage(requestResponseMap), module);
                JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
                jsonResponse.addProperty("error", "true");
                jsonResponseString = jsonResponse.toString();
                System.out.println("######## jsonResponseString: " + jsonResponseString);

                msg = "PAYPAL COMM. ERROR: " + jsonResponseString;
                Debug.logError(msg, module);
                if (logger != null) logger.logError(msg);
            }
        }

        // Writing the response
        try {
            PrintWriter out = response.getWriter();
            response.setContentType("text/plain");
            out.println(jsonResponseString);
            out.close();
        } catch (IOException e) {
            msg = "createOrderRequest() - Error writing servlet output. Msg => " + e.getMessage();
            Debug.logError(msg, module);
            if (logger != null) logger.logError(msg);
            return "error";
        }

        return eventReturn;

    }

    /**
     * Create order request specific for advanced hosted fields integration with 3DS Auth.
     * @param request
     * @param response
     * @return
     */
    public static String createOrder3DSHostedRequest(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        boolean requiredNewToken = false;
        boolean error = false;
        Map<String, Object> requestResponseMap = null;
        HashMap<String, String> jsonResponseFields = null;
        String eventReturn = "success";
        String jsonResponseString = null;
        String msg = "";

        //Reading parameters from the interface
        HashMap<String, String> hostedCardFieldsMap = (HashMap<String, String>) PaypalSDKEventsHelper.getHostedFieldsOrderParametersFromRequestBody(request);

        String orderId = hostedCardFieldsMap.get("orderId");
        String paymentMethod = hostedCardFieldsMap.get("paymentMethod");

        PaypalLogger logger = new PaypalLogger(delegator.getDelegatorTenantId());

        if(orderId == null) {
            msg = "createOrder3DSHostedRequest() - orderId not found in request. Cannot proceed with payment processing.";
            if (logger != null) logger.logError(msg);
            Debug.logError(msg, module);

            //Build a json response for the servlet
            jsonResponseFields = new HashMap<>();
            jsonResponseFields.put("error", "true");
            jsonResponseFields.put("ERROR_MSG", msg);
            jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

            // Writing the response and return error
            try {
                PrintWriter out = response.getWriter();
                response.setContentType("text/plain");
                out.println(jsonResponseString);
                out.close();
            } catch (IOException e) {
                msg = "createOrder3DSHostedRequest() - Error writing servlet output. Msg => " + e.getMessage();
                Debug.logError(msg, module);
                if (logger != null) logger.logError(msg);
                return "error";
            }
            return "error";
        }

        msg = "- Called createOrder3DSHostedRequest method of PaypalEvents for order id ["+orderId+"] -";
        if (logger != null) logger.logInfo(msg);
        Debug.logInfo(msg, module);

        OrderRequestObject ordReqObj = PaypalEventsHelper.buildOrder3DSHostedRequestObject(orderId, hostedCardFieldsMap, paymentMethod, request, delegator);

        //Object serialization into json
        Gson oreq_gson = new Gson();
        String oreq_jsonString = oreq_gson.toJson(ordReqObj);

        Debug.logWarning("Resulting json object => "+oreq_jsonString, module);
        msg = "Creating PayPal Order. JSON object => "+oreq_jsonString;
        if (logger != null) logger.logInfo(msg);

        requestResponseMap = PaypalClient.createPaypalOrder(oreq_jsonString, delegator);

        if (ServiceUtil.isSuccess(requestResponseMap)) {
            JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
            jsonResponseString = jsonResponse.toString();
            Debug.logInfo("Retrieved data => " + jsonResponseString, module);
            String payPalOrderStatus = PaypalEventsHelper.getOrderStatusFromJsonResponse(jsonResponse);
            String payPalOrderId = PaypalEventsHelper.getOrderIdFromJsonResponse(jsonResponse);

            msg = "PayPal Order Created with status: " + payPalOrderStatus + "and PayPal Order ID: " + payPalOrderId;
            if (logger != null) logger.logInfo(msg);
            Debug.logInfo(msg, module);

            PaypalEventsHelper.setOrderHeaderPayPalOrderId(payPalOrderId, orderId, delegator);
            String noteMsgOrderCreated = "PayPal Order created with ID => " + payPalOrderId + " and Status => " + payPalOrderStatus;
            if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderCreated, dispatcher, delegator)) {
                msg = "Error in creating OrderHeader note for order [" + orderId + "]";
                if (logger != null) logger.logError(msg);
                Debug.logError(msg, module);
            }

        } else {
            //Something went wrong during the request: check the http response code
            int http_response_code = (int) requestResponseMap.get("http_response_code");
            String http_response_message = (String) requestResponseMap.get("http_response_message");

            msg = "initOrderRequest() ERROR => Response code: " + http_response_code + ", Response message: " + http_response_message;
            if (logger != null) logger.logError(msg);
            Debug.logError(msg, module);

            if (401 == http_response_code) {
                requiredNewToken = true;
            } else if (http_response_code != 200) {
                //A general error occured
                error = true;
                eventReturn = "error";

                //Everything else should be a specific API issue
                JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
                jsonResponse.addProperty("error", "true");
                jsonResponseString = jsonResponse.toString();

                msg = "PAYPAL COMM. ERROR: " + jsonResponseString;
                if (logger != null) logger.logError(msg);
            }
        }

        if (!error && requiredNewToken) {

            msg = "A new OAuth Access Token is required: requesting a new one.";
            if (logger != null) logger.logInfo(msg);
            Debug.logInfo(msg, module);

            Map<String, Object> requestAPIAuthTokenMap = PaypalClient.requestAndStoreAccessToken(delegator);
            if (!ServiceUtil.isSuccess(requestAPIAuthTokenMap)) {
                msg = ServiceUtil.getErrorMessage(requestAPIAuthTokenMap);
                Debug.logError(msg, module);
                if (logger != null) logger.logError(msg);

                //Build a json response for the servlet
                jsonResponseFields = new HashMap<>();
                jsonResponseFields.put("error", "true");
                jsonResponseFields.put("ERROR_MSG", msg);
                jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

                try {
                    PrintWriter out = response.getWriter();
                    response.setContentType("text/plain");
                    out.println(jsonResponseString);
                    out.close();
                } catch (IOException e) {
                    msg = "createOrder3DSHostedRequest() - Error writing servlet output. Msg => " + e.getMessage();
                    Debug.logError(msg, module);
                    if (logger != null) logger.logError(msg);
                    return "error";
                }
                return "error";
            }

            /* Here I should have a new fresh token, so I'll retry the request. */
            Debug.logInfo("New Access Token generated: 2nd attempt for request createPaypalOrder", module);
            msg = "New Access Token generated: 2nd attempt to create PayPal Order...";
            if (logger != null) logger.logInfo(msg);

            requestResponseMap = PaypalClient.createPaypalOrder(oreq_jsonString, delegator);

            if (ServiceUtil.isSuccess(requestResponseMap)) {
                JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
                jsonResponseString = jsonResponse.toString();

                Debug.logInfo("Retrieved data => " + jsonResponseString, module);

                String payPalOrderStatus = PaypalEventsHelper.getOrderStatusFromJsonResponse(jsonResponse);
                String payPalOrderId = PaypalEventsHelper.getOrderIdFromJsonResponse(jsonResponse);

                msg = "PayPal Order Created with status: " + payPalOrderStatus + " and PayPal Order ID: " + payPalOrderId;
                if (logger != null) logger.logInfo(msg);
                Debug.logInfo(msg, module);

                PaypalEventsHelper.setOrderHeaderPayPalOrderId(payPalOrderId, orderId, delegator);

                String noteMsgOrderCreated = "PayPal Order created with ID => " + payPalOrderId + " and Status => " + payPalOrderStatus;
                if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderCreated, dispatcher, delegator)) {
                    Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
                }
            } else {
                error = true;
                eventReturn = "error";
                Debug.logError(ServiceUtil.getErrorMessage(requestResponseMap), module);
                JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
                jsonResponse.addProperty("error", "true");
                jsonResponseString = jsonResponse.toString();
                System.out.println("######## jsonResponseString: " + jsonResponseString);

                msg = "PAYPAL COMM. ERROR: " + jsonResponseString;
                Debug.logError(msg, module);
                if (logger != null) logger.logError(msg);

            }
        }

        // Writing the response
        try {
            PrintWriter out = response.getWriter();
            response.setContentType("text/plain");
            out.println(jsonResponseString);
            out.close();
        } catch (IOException e) {
            msg = "createOrder3DSHostedRequest() - Error writing servlet output. Msg => " + e.getMessage();
            Debug.logError(msg, module);
            if (logger != null) logger.logError(msg);
            return "error";
        }

        return eventReturn;
    }

    /**
     *
     * @param request
     * @param response
     * @return
     */
    public static String captureOrderRequest(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        boolean requiredNewToken = false;
        boolean verified3ds = false;
        boolean check3ds = false;
        Map<String, Object> requestResponseMap = null;
        Map<String, Object> orderReturnMap = null;
        HashMap<String, String> jsonResponseFields = null;
        String jsonResponseString = null;
        JsonObject jsonResponse = null;
        String eventReturn = "success";
        String msg = "";
        String paypalOrderStatus = "";
        String orderStatus = ""; //backoffice order status

        HashMap<String, String> orderIdsMap = (HashMap) PaypalSDKEventsHelper.getOrderIdsFromRequestBody(request);

        //TODO: error handling
        if(orderIdsMap == null) {
            Debug.logError("orderIdsMap is null", module);
            return "error";
        }

        String paypalOrderId = orderIdsMap.get("paypalOrderId");
        String orderId = orderIdsMap.get("orderId");

        //Check 3DS is enabled for Credit Card Payment (hosted-fields) performed in page
        check3ds = (request.getAttribute("check3ds") != null) && ((String) request.getAttribute("check3ds")).equals("Y");

        ArrayList<HashMap<String, String>> hateoasLinkList = null;

        PaypalLogger logger = new PaypalLogger(delegator.getDelegatorTenantId());

        System.out.println("JAVASCRIPT SDK onApprove => captureOrderRequest for order [" + paypalOrderId + "/" + orderId + "]. Check 3DS is " + check3ds);

        //Try to retrieve a backoffice order associated to the PayPal Order ID
        orderReturnMap = PaypalEventsHelper.getOrderDataFromPaypalOrderId(paypalOrderId, delegator);

        //Get order details from Paypal
        requestResponseMap = PaypalClient.getPaypalOrderDetails(paypalOrderId, delegator);
        if (!ServiceUtil.isSuccess(orderReturnMap)) {
            msg = ServiceUtil.getErrorMessage(orderReturnMap);
            Debug.logError(msg, module);

            //Build a json response for the servlet
            jsonResponseFields = new HashMap<>();
            jsonResponseFields.put("error", "true");
            jsonResponseFields.put("ERROR_MSG", msg);
            jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

            try {
                PrintWriter out = response.getWriter();
                response.setContentType("text/plain");
                out.println(jsonResponseString);
                out.close();
            } catch (IOException e) {
                msg = "createOrder3DSHostedRequest() - Error writing servlet output. Msg => " + e.getMessage();
                Debug.logError(msg, module);
                if (logger != null) logger.logError(msg);
                return "error";
            }

            return "error";
        }
        orderStatus = (String) orderReturnMap.get("statusId");

        // Checks on backoffice order status: if order is CANCELLED, stop processing since there is nothing more that can be done
        if (orderStatus.equals("ORDER_CANCELLED") || orderStatus.equals("ORDER_COMPLETED")) {
            msg = "Order Id " + orderId + "(" + paypalOrderId + ") is " + orderStatus + ". No more actions are possible for this order.";
            if (logger != null) logger.logInfo(msg);
            Debug.logInfo(msg, module);

            //Build a json response for the servlet
            jsonResponseFields = new HashMap<>();
            jsonResponseFields.put("error", "true");
            jsonResponseFields.put("ERROR_MSG", msg);
            jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

            try {
                PrintWriter out = response.getWriter();
                response.setContentType("text/plain");
                out.println(jsonResponseString);
                out.close();
            } catch (IOException e) {
                msg = "createOrder3DSHostedRequest() - Error writing servlet output. Msg => " + e.getMessage();
                Debug.logError(msg, module);
                if (logger != null) logger.logError(msg);
                return "error";
            }

            return "error";
        }

        //If success then check the order status and try to capture the payment
        if (ServiceUtil.isSuccess(requestResponseMap)) {

            jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
            jsonResponseString = jsonResponse.toString();
            System.out.println("Order details => " + jsonResponseString);
            JsonElement status = jsonResponse.get("status");
            paypalOrderStatus = status.getAsString();

            /*
             * If 3DS is enabled (hosted-fields) check if we can continue with the order capture or not,
             * according to these references:
             * https://developer.paypal.com/docs/checkout/advanced/customize/3d-secure/response-parameters/
             * If LiabilityShift is "POSSIBLE" always proceed with the capture, otherwise check values of
             * EnrollmentStatus and Authentication_Status parameters.
             */
            String liabilityShift = "";
            String enrollmentStatus = "";
            String authenticationStatus = "";

            if (check3ds) {
                //Perform checks
                Map<String, Object> verify3dsReturnMap = PaypalEventsHelper.verify3DSAuthorization(jsonResponse);
                verified3ds = (boolean) verify3dsReturnMap.get("3DS_VERIFIED");
                liabilityShift = (String) verify3dsReturnMap.get("LIABILITY_SHIFT");
                enrollmentStatus = (String) verify3dsReturnMap.get("ENROLLMENT_STATUS");
                authenticationStatus = (String) verify3dsReturnMap.get("AUTHENTICATION_STATUS");
            } else {
                //If not enabled, set the verified always to true
                verified3ds = true;
            }

            if (!verified3ds) {
                msg = "3DS VERIFICATION FAILED. DO NOT CAPTURE THE ORDER [" + orderId + "/" + paypalOrderId + "]. LiabilityShift: " + liabilityShift + ", EnrollmentStatus: " + enrollmentStatus + ", AuthorizationStatus: " + authenticationStatus;
                Debug.logError(msg, module);
                if (logger != null) logger.logInfo(msg);

                //Build a json response for the servlet
                jsonResponseFields = new HashMap<>();
                jsonResponseFields.put("error", "true");
                jsonResponseFields.put("ERROR_MSG", msg);
                jsonResponseFields.put("LIABILITY_SHIFT", liabilityShift);
                jsonResponseFields.put("ENROLLMENT_STATUS", enrollmentStatus);
                jsonResponseFields.put("AUTH_STATUS", authenticationStatus);
                jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

            } else {

                hateoasLinkList = PaypalEventsHelper.getHATEOASLinksFromJsonResponse(jsonResponse);

                String orderCaptureLink = PaypalEventsHelper.getHATEOASByRelType(hateoasLinkList, "capture");
                String linkMethod = PaypalEventsHelper.getHATEOASLinkMethodByRelType(hateoasLinkList, "capture");

                String noteMsgOrderPaymApproved = "PayPal Payment Approved for order " + orderId + "(" + paypalOrderId + "). PAYMENT CAPTURE REQUIRED.";
                Debug.logInfo(noteMsgOrderPaymApproved, module);
                if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderPaymApproved, dispatcher, delegator)) {
                    Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
                }

                Map<String, Object> captureOrderReturnMap = PaypalClient.capturePaypalOrder(orderCaptureLink, linkMethod, paypalOrderId, delegator);
                if (ServiceUtil.isSuccess(captureOrderReturnMap)) {
                    jsonResponse = (JsonObject) captureOrderReturnMap.get("json_response_object");
                    jsonResponseString = jsonResponse.toString();
                    status = jsonResponse.get("status");
                    paypalOrderStatus = status.getAsString();

                    Debug.logInfo("Capture order response => " + jsonResponse.toString(), module);

                    //Retrieve payment capture status
                    String transactionStatus = PaypalEventsHelper.getTransactionStatusFromJsonResponse(jsonResponse);
                    String transactionStatusDetail = PaypalEventsHelper.getTransactionStatusDetailFromJsonResponse(jsonResponse);
                    HashMap<String, String> processorStatusMap = (HashMap<String, String>) PaypalEventsHelper.getCardTransactionProcessorResponseFromJsonResponse(jsonResponse);

                    //msg = "Payment capture request completed successfully. Capture status is: " + paypalOrderStatus;
                    msg = "Payment capture request completed for order " + orderId + "/" + paypalOrderId + ". Paypal order_status is: " + paypalOrderStatus + ", payment_status is: " + transactionStatus;
                    if (logger != null) logger.logInfo(msg);

                    //Payment complete/pending: approve the order
                    if (transactionStatus.equals(PaypalRequestEnum.PAYPAL_CAPTURE_COMPLETED) || transactionStatus.equals(PaypalRequestEnum.PAYPAL_CAPTURE_PENDING)) {
                        String noteMsgOrderCaptured = "PayPal Order Payment Captured. PayPal Order " + paypalOrderId + ", Order Status is " + paypalOrderStatus + ", Payment Capture Status is " + transactionStatus;
                        if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderCaptured, dispatcher, delegator)) {
                            Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
                        }
                        OrderReadHelper orh = new OrderReadHelper(PaypalEventsHelper.getOrderHeaderFromId(orderId, delegator));
                        GenericValue productStore = orh.getProductStore();
                        boolean autoApproveOrder = (productStore != null && ( UtilValidate.isEmpty(productStore.get("autoApproveOrder")) || "Y".equals(productStore.getString("autoApproveOrder"))));
                        if (autoApproveOrder) {
                            boolean orderApproved = OrderChangeHelper.approveOrder(dispatcher, PaypalEventsHelper.getSystemUserLogin(delegator), orderId);
                            if (!orderApproved) {
                                msg = "Order [" + orderId + "] not auto-approved.";
                                if (logger != null) logger.logError(msg);
                                Debug.logError(msg, module);
                            } else {
                                //Send the confirmation email
                                Map<String, Object> orderConfirmEmailReturnMap = null;
                                orderConfirmEmailReturnMap = PaypalEventsHelper.sendOrderConfirmationEmail(orderId, delegator, dispatcher);
                                if (ServiceUtil.isSuccess(orderConfirmEmailReturnMap)) {
                                    msg = ServiceUtil.getErrorMessage(orderConfirmEmailReturnMap);
                                    if (logger != null) logger.logError(msg);
                                    Debug.logError(msg, module);
                                }
                            }
                        }
                    } else {
                        //Payment capture not completed. Do not approve the order
                        msg = "Capture error.  Status:" + transactionStatus + " => " + transactionStatusDetail + ".";
                        String cardResponseCode = "";
                        String avs_code = "";
                        String cvv_code = "";

                        if (!processorStatusMap.isEmpty()) {
                            cardResponseCode =  (processorStatusMap.get("response_code") != null) ? processorStatusMap.get("response_code") : "";
                            avs_code = (processorStatusMap.get("avs_code") != null) ? processorStatusMap.get("avs_code") : "";
                            cvv_code = (processorStatusMap.get("cvv_code") != null) ? processorStatusMap.get("cvv_code") : "";

                            msg += "Card processor => response_code:" + cardResponseCode + ",avs_code:" + avs_code + ",cvv_code:" + cvv_code;
                        }

                        if (logger != null) logger.logError(msg);
                        if (!PaypalEventsHelper.createOrderNote(orderId, msg, dispatcher, delegator)) {
                            Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
                        }

                        jsonResponse.addProperty("error", "true");
                        jsonResponse.addProperty("ERROR_MSG", msg);
                        jsonResponseString = jsonResponse.toString();
                        eventReturn = "error";
                    }
                }
            } //3ds verification branch

        } else {
            if (logger != null) logger.logError(ServiceUtil.getErrorMessage(requestResponseMap));
            jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
            jsonResponse.addProperty("error", "true");
            jsonResponseString = jsonResponse.toString();
            System.out.println("#### Order detail error: " + ServiceUtil.getErrorMessage(requestResponseMap));
            eventReturn = "error";
        }

        //Writing the response
        try {
            PrintWriter out = response.getWriter();
            response.setContentType("text/plain");
            out.println(jsonResponseString);
            out.close();
        } catch (IOException e) {
            msg = "Problems writing servlet output! Msg => " + e.getMessage();
            Debug.logError(msg, module);
            return "error";
        }

        return eventReturn;

    }
    /**
     *
     * @param request
     * @param response
     * @return
     */
    public static String cancelOrderRequest(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HashMap<String, String> jsonResponseFields = null;
        GenericValue orderHeader = null;
        String eventReturn = "success";
        String msg = "";
        String jsonResponseString = null;
        boolean error = false;

        PaypalLogger logger = new PaypalLogger(delegator.getDelegatorTenantId());

        HashMap<String, String> orderIdsMap = (HashMap) PaypalSDKEventsHelper.getOrderIdsFromRequestBody(request);

        //TODO: error handling
        if(orderIdsMap == null) {
            Debug.logError("orderIdsMap is null", module);
            return "error";
        }

        String paypalOrderId = orderIdsMap.get("paypalOrderId");
        String orderId = orderIdsMap.get("orderId");

        if (logger != null) logger.logInfo("- Cancelling PayPal Order ID " + paypalOrderId + "[" + orderId + "] -");

        System.out.println("Cancelling PayPal Order ID " + paypalOrderId + "[" + orderId + "]");

        try {
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        } catch (GenericEntityException gee) {
            msg = "Error in retrieving OrderHeader for order " + orderId + ". Msg => " + gee.getMessage();
            Debug.logError(msg, module);
            if (logger != null) logger.logError(msg);

            //Build a json response for the servlet
            jsonResponseFields = new HashMap<>();
            jsonResponseFields.put("error", "true");
            jsonResponseFields.put("ERROR_MSG", msg);
            jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

            try {
                PrintWriter out = response.getWriter();
                response.setContentType("text/plain");
                out.println(jsonResponseString);
                out.close();
            } catch (IOException e) {
                msg = "createOrder3DSHostedRequest() - Error writing servlet output. Msg => " + e.getMessage();
                Debug.logError(msg, module);
                if (logger != null) logger.logError(msg);
                return "error";
            }
            return "error";
        }

        if (orderHeader != null) {
            String orderStatus = orderHeader.getString("statusId");

            if ("ORDER_CANCELLED".equals(orderStatus) || "ORDER_COMPLETED".equals(orderStatus)) {
                msg = "Order Id " + orderId + "(" + paypalOrderId + ") is " + orderStatus + ". No more actions are possible for this order.";
                if (logger != null) logger.logInfo(msg);
                Debug.logInfo(msg, module);

                //Build a json servlet response
                jsonResponseFields = new HashMap<>();
                jsonResponseFields.put("error", "true");
                jsonResponseFields.put("ERROR_MSG", msg);
                jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

                error = true;
                eventReturn = "error";
            }

            if(!error) {
                //Try to cancel the order
                boolean orderCancelled = OrderChangeHelper.cancelOrder(dispatcher, PaypalEventsHelper.getSystemUserLogin(delegator), orderId);
                String orderCancelledNoteMsg = "Order " + orderId + "(" + paypalOrderId + ") Cancel triggered by user. ORDER CANCELLED: " + orderCancelled;
                if (logger != null) logger.logInfo(orderCancelledNoteMsg);
                Debug.logInfo(orderCancelledNoteMsg, module);
                if (!PaypalEventsHelper.createOrderNote(orderId, orderCancelledNoteMsg, dispatcher, delegator)) {
                    Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
                }

                if (!orderCancelled) {
                    msg = "Problems occured while cancelling order " + orderId + "(" + paypalOrderId + ").";
                    if (logger != null) logger.logError(msg);
                    Debug.logError(msg, module);

                    //Build a json servlet response
                    jsonResponseFields = new HashMap<>();
                    jsonResponseFields.put("error", "true");
                    jsonResponseFields.put("ERROR_MSG", msg);
                    jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

                    error = true;
                    eventReturn = "error";
                }

            }

        } else {
            msg = "No order found for orderId " + orderId;
            Debug.logError(msg, module);
            //Build a json servlet response
            jsonResponseFields = new HashMap<>();
            jsonResponseFields.put("error", "true");
            jsonResponseFields.put("ERROR_MSG", msg);
            jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);

            error = true;
            eventReturn = "error";
        }

        if(!error) {
            msg = "Order " + orderId + " deleted";
            jsonResponseFields = new HashMap<>();
            jsonResponseFields.put("error", "false");
            jsonResponseFields.put("SUCCESS_MSG", msg);
            jsonResponseFields.put("orderId", orderId);
            jsonResponseString = PaypalEventsHelper.buildServletJsonResponse(jsonResponseFields);
        }

        //Writing the response
        try {
            PrintWriter out = response.getWriter();
            response.setContentType("text/plain");
            out.println(jsonResponseString);
            out.close();
        } catch (IOException e) {
            msg = "Problems writing servlet output! Msg => " + e.getMessage();
            Debug.logError(msg, module);
            return "error";
        }

        return eventReturn;
    }
} //end class
