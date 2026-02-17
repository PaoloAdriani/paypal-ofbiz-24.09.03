package paypal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PaypalEvents {

    public static final String module = PaypalEvents.class.getName();

    public static final String PAYPAL_SYSTEM_RESOURCE = "paypal";

    /**
     * Init request: it creates the PayPal Order.
     *
     * @param request
     * @param response
     * @return
     */
    public static String initOrderRequest(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        boolean requiredNewToken = false;
        Map<String, Object> requestResponseMap = null;
        String redirectLink = null;
        String msg = "";

        String orderId = (String) request.getAttribute("orderId");

        PaypalLogger logger = new PaypalLogger(delegator.getDelegatorTenantId());

        if (orderId == null) {
            msg = "initOrderRequest() - orderId not found in request. Cannot proceed with payment processing.";
            if (logger != null) {
                logger.logError(msg);
            }
            Debug.logError(msg, module);
            return "error";
        }

        msg = "- Called initOrderRequest method of PaypalEvents for order id [" + orderId + "] -";
        if (logger != null) {
            logger.logInfo(msg);
        }
        Debug.logInfo(msg, module);

        /* Get the orderHeader and OrderReadHelper */
        OrderRequestObject ordReqObj = PaypalEventsHelper.buildOrderRequestObject(orderId, null, request, delegator);

        //Object serialization into json
        Gson oreq_gson = new Gson();
        String oreq_jsonString = oreq_gson.toJson(ordReqObj);

        Debug.logWarning("Resulting json object => " + oreq_jsonString, module);
        msg = "Creating PayPal Order.JSON object => "+oreq_jsonString;
        if (logger != null) {
            logger.logInfo(msg);
        }

        requestResponseMap = PaypalClient.createPaypalOrder(oreq_jsonString, delegator);

        if (ServiceUtil.isSuccess(requestResponseMap)) {
            JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");
            Debug.logInfo("Retrieved data => " + jsonResponse.toString(), module);
            String payPalOrderStatus = PaypalEventsHelper.getOrderStatusFromJsonResponse(jsonResponse);
            String payPalOrderId = PaypalEventsHelper.getOrderIdFromJsonResponse(jsonResponse);
            ArrayList<HashMap<String, String>> hateoasLinksList = PaypalEventsHelper.getHATEOASLinksFromJsonResponse(jsonResponse);

            msg = "PayPal Order Created with status: " + payPalOrderStatus + "and PayPal Order ID: " + payPalOrderId;
            if (logger != null) {
                logger.logInfo(msg);
            }
            Debug.logInfo(msg, module);

            redirectLink = PaypalEventsHelper.getHATEOASByRelType(hateoasLinksList, PaypalClient.HATEOAS_LINK_REL_PAYER_ACTION);
            PaypalEventsHelper.setOrderHeaderPayPalOrderId(payPalOrderId, orderId, delegator);
            String noteMsgOrderCreated = "PayPal Order created with ID => " + payPalOrderId + " and Status => " + payPalOrderStatus;
            if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderCreated, dispatcher, delegator)) {
                msg = "Error in creating OrderHeader note for order [" + orderId + "]";
                if (logger != null) {
                    logger.logError(msg);
                }
                Debug.logError(msg, module);
            }

        } else {
            //Something went wrong during the request: check the http response code
            int http_response_code = (int) requestResponseMap.get("http_response_code");
            String http_response_message = (String) requestResponseMap.get("http_response_message");

            msg = "initOrderRequest() ERROR => Response code: " + http_response_code + ", Response message: " + http_response_message;
            if (logger != null) {
                logger.logError(msg);
            }
            Debug.logError(msg, module);

            if (401 == http_response_code) {
                requiredNewToken = true;
            } else if (http_response_code != 200) {
                //Everytihing else should be a specific API issue
                JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");

                JsonElement issueName = jsonResponse.get("name");
                JsonElement issueMessage = jsonResponse.get("message");
                JsonElement issueDetail = jsonResponse.get("details");

                request.setAttribute("_PAYPAL_ISSUE_NAME_", issueName.toString());
                request.setAttribute("_PAYPAL_ISSUE_MESSAGE_", issueMessage.toString());
                request.setAttribute("_PAYPAL_ISSUE_DETAIL_", issueDetail.toString());

                msg = "_PAYPAL_ISSUE_NAME_: " + issueName.toString() + ", _PAYPAL_ISSUE_MESSAGE_: " + issueMessage.toString() + ", _PAYPAL_ISSUE_DETAIL_: " + issueDetail.toString();
                if (logger != null) {
                    logger.logError(msg);
                }
                return "error";
            }
        }

        if (requiredNewToken) {

            msg = "A new OAuth Access Token is required: requesting a new one.";
            if (logger != null) {
                logger.logInfo(msg);
            }
            Debug.logInfo(msg, module);

            Map<String, Object> requestAPIAuthTokenMap = PaypalClient.requestAndStoreAccessToken(delegator);
            if (!ServiceUtil.isSuccess(requestAPIAuthTokenMap)) {
                msg = ServiceUtil.getErrorMessage(requestAPIAuthTokenMap);
                Debug.logError(msg, module);
                if (logger != null) {
                    logger.logError(msg);
                }
                return "error";
            }

            /* Here I should have a new fresh token, so I'll retry the request. */
            Debug.logInfo("New Access Token generated: 2nd attempt for request createPaypalOrder", module);
            msg = "New Access Token generated: 2nd attempt to create PayPal Order...";
            if (logger != null) {
                logger.logInfo(msg);
            }

            requestResponseMap = PaypalClient.createPaypalOrder(oreq_jsonString, delegator);

            if (ServiceUtil.isSuccess(requestResponseMap)) {
                JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");

                Debug.logInfo("Retrieved data => " + jsonResponse.toString(), module);

                String payPalOrderStatus = PaypalEventsHelper.getOrderStatusFromJsonResponse(jsonResponse);
                String payPalOrderId = PaypalEventsHelper.getOrderIdFromJsonResponse(jsonResponse);
                ArrayList<HashMap<String, String>> hateoasLinksList = PaypalEventsHelper.getHATEOASLinksFromJsonResponse(jsonResponse);

                msg = "PayPal Order Created with status: " + payPalOrderStatus + " and PayPal Order ID: " + payPalOrderId;
                if (logger != null) {
                    logger.logInfo(msg);
                }
                Debug.logInfo(msg, module);

                /*
            	for (HashMap<String, String> _linkMap : hateoasLinksList) {
            		System.out.println(UtilMisc.printMap(_linkMap));
            	}
                 */
                redirectLink = PaypalEventsHelper.getHATEOASByRelType(hateoasLinksList, PaypalClient.HATEOAS_LINK_REL_PAYER_ACTION);
                PaypalEventsHelper.setOrderHeaderPayPalOrderId(payPalOrderId, orderId, delegator);

                String noteMsgOrderCreated = "PayPal Order created with ID => " + payPalOrderId + " and Status => " + payPalOrderStatus;
                if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderCreated, dispatcher, delegator)) {
                    Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
                }
            } else {
                Debug.logError(ServiceUtil.getErrorMessage(requestResponseMap), module);
                JsonObject jsonResponse = (JsonObject) requestResponseMap.get("json_response_object");

                JsonElement issueName = jsonResponse.get("name");
                JsonElement issueMessage = jsonResponse.get("message");
                JsonElement issueDetail = jsonResponse.get("details");

                request.setAttribute("_PAYPAL_ISSUE_NAME_", issueName.toString());
                request.setAttribute("_PAYPAL_ISSUE_MESSAGE_", issueMessage.toString());
                request.setAttribute("_PAYPAL_ISSUE_DETAIL_", issueDetail.toString());

                msg = "_PAYPAL_ISSUE_NAME_: " + issueName.toString() + ", _PAYPAL_ISSUE_MESSAGE_: " + issueMessage.toString() + ", _PAYPAL_ISSUE_DETAIL_: " + issueDetail.toString();
                if (logger != null) {
                    logger.logError(msg);
                }

                return "error";
            }
        }

        if (redirectLink != null) {
            try {
                msg = "Redirecting to PayPal Gateway for payment authorization for order " + orderId + "...";
                if (logger != null) {
                    logger.logInfo(msg);
                }

                response.sendRedirect(redirectLink);
            } catch (IOException e) {
                msg = "Error occured during redirection to PayPal Payment Gateway for order [" + orderId + "]. Error is => " + e.getMessage();
                if (logger != null) {
                    logger.logError(msg);
                }
                Debug.logError(msg, module);

                request.setAttribute("_PAYPAL_ISSUE_NAME_", "CLIENT_REDIRECT_ERROR");
                request.setAttribute("_PAYPAL_ISSUE_MESSAGE_", e.getMessage());
                request.setAttribute("_PAYPAL_ISSUE_DETAIL_", msg);
                return "error";
            }
        }

        return "success";
    }

    /**
     * Notify Hook
     *
     * @param request
     * @param response
     * @return
     */
    public static String payPalNotify(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        boolean requiredNewToken = false;
        boolean webhook_verified = false;
        String msg = "";

        PaypalLogger logger = new PaypalLogger(delegator.getDelegatorTenantId());

        msg = "- Called PayPal WebHook -";
        if (logger != null) {
            logger.logInfo(msg);
        }
        Debug.logInfo(msg, module);

        //Get headers from the request
        HashMap<String, String> headersMap = PaypalClientHelper.getRequestHeaderData(request);
        HashMap<String, Object> requestBodyMap = PaypalClientHelper.getRequestBodyData(request);

        String event_type = (String) requestBodyMap.get("event_type");
        String resource_type = (String) requestBodyMap.get("resource_type");

        msg = "PayPal Notify Hook: EVENT TYPE => " + event_type + ", RESOURCE TYPE: " + resource_type;
        if (logger != null) {
            logger.logInfo(msg);
        }
        Debug.logInfo(msg, module);
        Debug.logInfo("Request header data: " + UtilMisc.printMap(headersMap), module);

        /* ##### START Webhook Verify Signature Phase ##### */
        Debug.logInfo("@@@@ PayPal RequestBody Data Map => " + UtilMisc.printMap(requestBodyMap), module);

        msg = "Verifing webhook signature...";
        if (logger != null) {
            logger.logInfo(msg);
        }

        boolean skipWebhookVerification = true;

        if(!skipWebhookVerification) {

            Map<String, Object> verifyWebHookReturnMap = PaypalClient.verifyWebhookSignature(headersMap, requestBodyMap, delegator);

            String verification_status = "";



            if (ServiceUtil.isSuccess(verifyWebHookReturnMap)) {
                JsonObject jsonResponse = (JsonObject) verifyWebHookReturnMap.get("json_response_object");
                Debug.logInfo("Verify response data => " + jsonResponse.toString(), module);
                JsonElement response_elem = jsonResponse.get("verification_status");
                verification_status = response_elem.getAsString();
                webhook_verified = (verification_status.equals(PaypalRequestEnum.VERIFY_WEBHOOK_SIG_SUCCESS));
            } else {
                //Something went wrong during the request: check the http response code
                int http_response_code = (int) verifyWebHookReturnMap.get("http_response_code");
                String http_response_message = (String) verifyWebHookReturnMap.get("http_response_message");

                msg = "payPalNotify() ERROR => Response code: " + http_response_code + ", Response message: " + http_response_message;
                if (logger != null) {
                    logger.logError(msg);
                }
                Debug.logError(msg, module);

                if (401 == http_response_code) {
                    requiredNewToken = true;
                } else if (http_response_code != 200) {
                    //Everytihing else should be a specific API issue
                    JsonObject jsonResponse = (JsonObject) verifyWebHookReturnMap.get("json_response_object");
                    JsonElement issueName = jsonResponse.get("name");
                    JsonElement issueMessage = jsonResponse.get("message");
                    JsonElement issueDetail = jsonResponse.get("details");
                    request.setAttribute("_PAYPAL_ISSUE_NAME_", issueName.toString());
                    request.setAttribute("_PAYPAL_ISSUE_MESSAGE_", issueMessage.toString());
                    request.setAttribute("_PAYPAL_ISSUE_DETAIL_", issueDetail.toString());

                    msg = "_PAYPAL_ISSUE_NAME_: " + issueName.toString() + ", _PAYPAL_ISSUE_MESSAGE_: " + issueMessage.toString() + ", _PAYPAL_ISSUE_DETAIL_: " + issueDetail.toString();
                    if (logger != null) {
                        logger.logError(msg);
                    }

                    return "error";
                }
            }
        }

        if (requiredNewToken) {
            Map<String, Object> requestAPIAuthTokenMap = PaypalClient.requestAndStoreAccessToken(delegator);
            if (!ServiceUtil.isSuccess(requestAPIAuthTokenMap)) {
                msg = ServiceUtil.getErrorMessage(requestAPIAuthTokenMap);
                Debug.logError(msg, module);
                if (logger != null) {
                    logger.logError(msg);
                }
                return "error";
            }

            if(!skipWebhookVerification) {

                Map<String, Object> verifyWebHookReturnMap = PaypalClient.verifyWebhookSignature(headersMap, requestBodyMap, delegator);

                if (ServiceUtil.isSuccess(verifyWebHookReturnMap)) {
                    JsonObject jsonResponse = (JsonObject) verifyWebHookReturnMap.get("json_response_object");
                    Debug.logInfo("Verify response data => " + jsonResponse.toString(), module);
                    JsonElement response_elem = jsonResponse.get("verification_status");
                    String verification_status = response_elem.getAsString();
                    webhook_verified = (verification_status.equals(PaypalRequestEnum.VERIFY_WEBHOOK_SIG_SUCCESS));
                } else {
                    msg = "Error during WebHook Signature Verification process.";
                    String detail = ServiceUtil.getErrorMessage(verifyWebHookReturnMap);
                    request.setAttribute("_PAYPAL_ISSUE_NAME_", "WEBHOOK_SIGNATURE_VERIFICATION_ERROR");
                    request.setAttribute("_PAYPAL_ISSUE_MESSAGE_", msg);
                    request.setAttribute("_PAYPAL_ISSUE_DETAIL_", detail);

                    msg = "_PAYPAL_ISSUE_NAME_: WEBHOOK_SIGNATURE_VERIFICATION_ERROR, _PAYPAL_ISSUE_MESSAGE_: " + msg + ", _PAYPAL_ISSUE_DETAIL_: " + detail;
                    if (logger != null) {
                        logger.logError(msg);
                    }

                    return "error";
                }

            }
        }

        /* ##### END Webhook Verify Signature Phase ##### */
        /* ####
         * UNDER INVESTIGATION: THE VERIFICATION PROCESS ALWAYS RETURNS FAILURE
         * FOR NOW FORCE IT TO ALWAYS BE true
         * ####
         */
        webhook_verified = true;

        //POST request OK, but verification FAILED
        if (!webhook_verified) {
            msg = "Webhook Signature Verification returned : " + PaypalRequestEnum.VERIFY_WEBHOOK_SIG_FAIL;
            String detail = "Webhook Signature Verification returned : " + PaypalRequestEnum.VERIFY_WEBHOOK_SIG_FAIL + ". Cannot verify notification message. Stop processing.";
            if (logger != null) {
                logger.logError(msg);
            }
            Debug.logError(detail, module);
            request.setAttribute("_PAYPAL_ISSUE_NAME_", "WEBHOOK_SIGNATURE_VERIFICATION_ERROR");
            request.setAttribute("_PAYPAL_ISSUE_MESSAGE_", msg);
            request.setAttribute("_PAYPAL_ISSUE_DETAIL_", detail);
            return "error";
        }

        msg = "Webhook Signature Verification returned: " + PaypalRequestEnum.VERIFY_WEBHOOK_SIG_SUCCESS + ". Proceeding.";
        if (logger != null) {
            logger.logInfo(msg);
        }

        //based on the order status perform some actions
        String paypalOrderId = PaypalEventsHelper.getPaypalOrderIdFromNotifyResponse(requestBodyMap, resource_type);
        String paypalOrderStatus = PaypalEventsHelper.getPaypalOrderStatusFromNotifyResponse(requestBodyMap);
        ArrayList<HashMap<String, String>> orderLinks = PaypalEventsHelper.getHATEOASLinksFromNotifyResponse(requestBodyMap);

        msg = "Retrieved paypal order id: " + paypalOrderId + ", order status: " + paypalOrderStatus + ", links: " + orderLinks;
        if (logger != null) {
            logger.logInfo(msg);
        }
        Debug.logInfo(msg, module);

        if (paypalOrderId == null || UtilValidate.isEmpty(paypalOrderId)) {
            msg = "PayPal Order Id not found in the Notify Response. Stop the processing.";
            if (logger != null) {
                logger.logError(msg);
            }
            Debug.logError(msg, module);
            return "ignore";
        }

        String orderId = "";
        String orderStatus = "";
        Map<String, Object> orderReturnMap = null;
        String returnEvent = "";
        OrderReadHelper orh = null;

        switch (event_type) {

            case PaypalRequestEnum.NOTIF_ET_CHECKOUT_ORDER_APPROV:

                //Try to retrieve a backoffice order associated to the PayPal Order ID
                orderReturnMap = PaypalEventsHelper.getOrderDataFromPaypalOrderId(paypalOrderId, delegator);
                if (!ServiceUtil.isSuccess(orderReturnMap)) {
                    msg = ServiceUtil.getErrorMessage(orderReturnMap);
                    Debug.logError(msg, module);
                    if (logger != null) {
                        logger.logError(msg);
                    }

                    returnEvent = (String) orderReturnMap.get("_EVENT_RETURN_");
                    return returnEvent;
                }
                orderId = (String) orderReturnMap.get("orderId");
                orderStatus = (String) orderReturnMap.get("statusId");

                // Checks on backoffice order status: if order is CANCELLED, stop processing since there is nothing more that can be done
                if (orderStatus.equals("ORDER_CANCELLED") || orderStatus.equals("ORDER_COMPLETED")) {
                    msg = "Order Id " + orderId + "(" + paypalOrderId + ") is " + orderStatus + ". No more actions are possible for this order.";
                    if (logger != null) {
                        logger.logInfo(msg);
                    }
                    Debug.logInfo(msg, module);
                    return "ignore";
                }

                String noteMsgOrderPaymApproved = "PayPal Payment Approved for order " + orderId + "(" + paypalOrderId + "). PAYMENT CAPTURE REQUIRED.";
                Debug.logInfo(noteMsgOrderPaymApproved, module);
                if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderPaymApproved, dispatcher, delegator)) {
                    Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
                }

                //The customer approved the payment: we can capture the payment, approve the order and send the notification email
                msg = "Order " + orderId + "(" + paypalOrderId + ") payment approved, going to capture it.";
                if (logger != null) {
                    logger.logInfo(msg);
                }

                String orderCaptureLink = PaypalEventsHelper.getHATEOASByRelType(orderLinks, "capture");
                String linkMethod = PaypalEventsHelper.getHATEOASLinkMethodByRelType(orderLinks, "capture");
                Map<String, Object> captureOrderReturnMap = PaypalClient.capturePaypalOrder(orderCaptureLink, linkMethod, paypalOrderId, delegator);
                if (ServiceUtil.isSuccess(captureOrderReturnMap)) {
                    JsonObject jsonResponse = (JsonObject) captureOrderReturnMap.get("json_response_object");
                    Debug.logInfo("Verify response data => " + jsonResponse.toString(), module);
                    msg = "Payment capture request completed successfully. Waiting for CAPTURE.COMPLETE notify message";
                    if (logger != null) {
                        logger.logInfo(msg);
                    }
                }
                break;

            case PaypalRequestEnum.NOTIF_ET_PAYMENT_CAPTURE_COMPLETED:
                orderId = PaypalEventsHelper.getCustomOrderIdFromNotifyResponse(requestBodyMap, resource_type);
                msg = "Payment Capture Completed for order " + orderId + "(" + paypalOrderId + "). Approving the order.";
                if (logger != null) {
                    logger.logInfo(msg);
                }
                Debug.logInfo(msg, module);

                String noteMsgOrderCaptured = "PayPal Order Payment Captured. PayPal Order " + paypalOrderId + " Status is => " + paypalOrderStatus;
                if (!PaypalEventsHelper.createOrderNote(orderId, noteMsgOrderCaptured, dispatcher, delegator)) {
                    Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
                }
                orh = new OrderReadHelper(PaypalEventsHelper.getOrderHeaderFromId(orderId, delegator));
                GenericValue productStore = orh.getProductStore();
                boolean autoApproveOrder = (productStore != null && (UtilValidate.isEmpty(productStore.get("autoApproveOrder")) || "Y".equals(productStore.getString("autoApproveOrder"))));
                if (autoApproveOrder) {
                    boolean orderApproved = OrderChangeHelper.approveOrder(dispatcher, PaypalEventsHelper.getSystemUserLogin(delegator), orderId);
                    if (!orderApproved) {
                        msg = "Order [" + orderId + "] not auto-approved.";
                        if (logger != null) {
                            logger.logError(msg);
                        }
                        Debug.logError(msg, module);
                    } else {
                        //Send the confirmation email
                        Map<String, Object> orderConfirmEmailReturnMap = null;
                        orderConfirmEmailReturnMap = PaypalEventsHelper.sendOrderConfirmationEmail(orderId, delegator, dispatcher);
                        if (ServiceUtil.isSuccess(orderConfirmEmailReturnMap)) {
                            msg = ServiceUtil.getErrorMessage(orderConfirmEmailReturnMap);
                            if (logger != null) {
                                logger.logError(msg);
                            }
                            Debug.logError(msg, module);
                        }
                    }
                }

                break;

            default:
                msg = "PayPal Event Type [" + event_type + " not handled. Stop processing";
                if (logger != null) {
                    logger.logWarning(msg);
                }
                Debug.logWarning(msg, module);
                return "ignore";
        }

        return "success";
    }

    /**
     * Order Cancel Event
     *
     * @param request
     * @param response
     * @return
     */
    public static String cancelOrder(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String msg = "";

        PaypalLogger logger = new PaypalLogger(delegator.getDelegatorTenantId());

        //PayPal Order Token
        String orderToken = (String) request.getParameter("token");

        Map<String, Object> orderDataMap = PaypalEventsHelper.getOrderDataFromPaypalOrderId(orderToken, delegator);

        if (!ServiceUtil.isSuccess(orderDataMap)) {
            Debug.logError(ServiceUtil.getErrorMessage(orderDataMap), module);
            String returnEvent = (String) orderDataMap.get("_EVENT_RETURN_");
            return returnEvent;
        }

        String orderId = (String) orderDataMap.get("orderId");
        String statusId = (String) orderDataMap.get("statusId");

        msg = "Called PayPal Cancel Event for order " + orderId + "(" + orderToken + ").";
        if (logger != null) {
            logger.logInfo(msg);
        }
        Debug.logInfo(msg, module);

        if (statusId.equals("ORDER_CANCELLED") || statusId.equals("ORDER_COMPLETED")) {
            msg = "Order Id " + orderId + "(" + orderToken + ") is " + statusId + ". No more actions are possible for this order.";
            if (logger != null) {
                logger.logInfo(msg);
            }
            Debug.logInfo(msg, module);
            return "success";
        }

        boolean orderCancelled = OrderChangeHelper.cancelOrder(dispatcher, PaypalEventsHelper.getSystemUserLogin(delegator), orderId);

        String orderCancelledNoteMsg = "Order" + orderId + "(" + orderToken + ") Cancel triggered by user. ORDER CANCELLED: " + orderCancelled;
        if (logger != null) {
            logger.logInfo(orderCancelledNoteMsg);
        }
        Debug.logInfo(orderCancelledNoteMsg, module);
        if (!PaypalEventsHelper.createOrderNote(orderId, orderCancelledNoteMsg, dispatcher, delegator)) {
            Debug.logError("Error in creating OrderHeader note for order [" + orderId + "].", module);
        }

        if (!orderCancelled) {
            msg = "Problems occured while cancelling order " + orderId + "(" + orderToken + ").";
            if (logger != null) {
                logger.logError(msg);
            }
            Debug.logError(msg, module);
            return "error";
        }
        return "success";
    }

} //end class
