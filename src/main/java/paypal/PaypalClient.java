package paypal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.ServiceUtil;
import paypal.beans.WebHookValidateRequestObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PaypalClient {

    public static final String module = PaypalClient.class.getName();
    public static final String PAYPAL_SYSTEM_RESOURCE = "paypal";

    private final static String REQUEST_CONTENT_TYPE_JSON = "application/json";
    private final static String REQUEST_CONTENT_TYPE_FORMURLENC = "application/x-www-form-urlencoded";
    private final static String REQUEST_ACCEPT_TYPE_JSON = "application/json";
    private static final String REQUEST_METHOD_POST = "POST";
    private static final String REQUEST_METHOD_GET = "GET";
    private static final String AUTH_TYPE_BASIC = "Basic";
    private static final String AUTH_TYPE_BEARER = "Bearer";

    private static String API_ACCESS_TOKEN = null;
    /**
     *
     * Optional. Contains a unique user-generated ID that the server stores for
     * a period of time. Use this header to enforce idempotency on REST API POST
     * calls. You can make these calls any number of times without concern that
     * the server creates or completes an action on a resource more than once.
     * You can retry calls that fail with network timeouts or the HTTP 500
     * status code. You can retry calls for as long as the server stores the ID.
     * MANDATORY IF payment_source object is specified in the create order
     * request object.
     */
    private static String API_REQUEST_ID = null;

    public static final String HATEOAS_LINK_REL_PAYER_ACTION = "payer-action";
    public static final String HATEOAS_LINK_REL_SELF = "self";

    public static String getClientToken(Delegator delegator) {

        String clientToken = null;
        String clientTokenEP = PaypalClientHelper.getClientTokenEndPoint(delegator);
        String environment = PaypalClientHelper.getPaypalEnvironment(delegator);
        boolean requiredNewToken = false;

        Map<String, Object> generateClientTokenResponseMap = sendRequest(PaypalClient.REQUEST_METHOD_POST, PaypalClient.REQUEST_ACCEPT_TYPE_JSON,
                PaypalClient.REQUEST_CONTENT_TYPE_JSON, null, PaypalClient.AUTH_TYPE_BEARER, clientTokenEP, "", environment, delegator);

        if (ServiceUtil.isSuccess(generateClientTokenResponseMap)) {
            JsonObject jsonResponse = (JsonObject) generateClientTokenResponseMap.get("json_response_object");
            JsonElement clientTokenElement = jsonResponse.get("client_token");
            clientToken = clientTokenElement.getAsString();
        } else {
            //Something went wrong during the request: check the http response code
            int http_response_code = (int) generateClientTokenResponseMap.get("http_response_code");
            String http_response_message = (String) generateClientTokenResponseMap.get("http_response_message");


            if (401 == http_response_code) {
                requiredNewToken = true;
            } else if (http_response_code != 200) {
                //Everytihing else should be a specific API issue
                JsonObject jsonResponse = (JsonObject) generateClientTokenResponseMap.get("json_response_object");

                JsonElement issueName = jsonResponse.get("name");
                JsonElement issueMessage = jsonResponse.get("message");
                JsonElement issueDetail = jsonResponse.get("details");

                String msg = "_PAYPAL_ISSUE_NAME_: " + issueName.toString() + ", _PAYPAL_ISSUE_MESSAGE_: " + issueMessage.toString() + ", _PAYPAL_ISSUE_DETAIL_: " + issueDetail.toString();
                Debug.logError(msg , module);
                return null;
            }

            if (requiredNewToken) {
                Map<String, Object> requestAPIAuthTokenMap = PaypalClient.requestAndStoreAccessToken(delegator);
                if (!ServiceUtil.isSuccess(requestAPIAuthTokenMap)) {
                    String msg = ServiceUtil.getErrorMessage(requestAPIAuthTokenMap);
                    Debug.logError(msg, module);
                    return null;
                }
                /* Here I should have a new fresh token, so I'll retry the request. */
                generateClientTokenResponseMap = sendRequest(PaypalClient.REQUEST_METHOD_POST, PaypalClient.REQUEST_ACCEPT_TYPE_JSON,
                        PaypalClient.REQUEST_CONTENT_TYPE_JSON, null, PaypalClient.AUTH_TYPE_BEARER, clientTokenEP, "", environment, delegator);
                if (ServiceUtil.isSuccess(generateClientTokenResponseMap)) {
                    JsonObject jsonResponse = (JsonObject) generateClientTokenResponseMap.get("json_response_object");
                    JsonElement clientTokenElement = jsonResponse.get("client_token");
                    clientToken = clientTokenElement.getAsString();
                } else {
                    Debug.logError(ServiceUtil.getErrorMessage(generateClientTokenResponseMap), module);
                    JsonObject jsonResponse = (JsonObject) generateClientTokenResponseMap.get("json_response_object");
                    String jsonResponseString = jsonResponse.toString();
                    System.out.println("######## jsonResponseString: " + jsonResponseString);

                    String msg = "PAYPAL COMM. ERROR: " + jsonResponseString;
                    Debug.logError(msg, module);
                    return null;
                }
            }
        }
        System.out.println("##### Client Token returned: " + clientToken);
        return clientToken;
    }

    /**
     * Get an Access Token from the db if exists, otherwise request a new token
     * and return it.
     *
     * @param delegator
     * @return
     */
    public static synchronized String getAPIAuthToken(Delegator delegator) {

        String authToken = null;
        String appID = PaypalClientHelper.getPaypalAppId(delegator);

        if (PaypalClient.API_ACCESS_TOKEN != null && UtilValidate.isNotEmpty(PaypalClient.API_ACCESS_TOKEN)) {
            return PaypalClient.API_ACCESS_TOKEN;
        }

        if (appID == null) {
            Debug.logWarning("PayPal API appId not set in PayPay Configuration Properties. Cannot check token existence on db. Returning null.", module);
            return null;
        }

        //The Access Token has not been retrieved yet: check on db first.
        GenericValue mpAccessToken = null;
        try {
            mpAccessToken = delegator.findOne("MpAccessToken", UtilMisc.toMap("appId", appID), false);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            return null;
        }
        if (mpAccessToken != null) {
            authToken = (String) mpAccessToken.get("tokenValue");
            if (PaypalClient.API_ACCESS_TOKEN == null || !PaypalClient.API_ACCESS_TOKEN.equals(authToken)) {
                setAPIAuthToken(authToken);
            }
            Debug.logWarning("## Found token on db for app_id [" + appID + "] => " + authToken, module);
        }

        /* A token has not be found on the db.
         * Requesting a new one and store it.
         */
        if (authToken == null || UtilValidate.isEmpty(authToken)) {
            Map<String, Object> authTokenReturnMap = requestAndStoreAccessToken(delegator);
            if (ServiceUtil.isSuccess(authTokenReturnMap)) {
                authToken = (String) authTokenReturnMap.get("access_token");
            } else {
                Debug.logError(ServiceUtil.getErrorMessage(authTokenReturnMap), module);
            }
        }
        return authToken;
    }

    /**
     *
     * @param delegator
     * @return
     */
    public static String getAPIRequestId(Delegator delegator) {

    	/*
        if (PaypalClient.API_REQUEST_ID != null && UtilValidate.isNotEmpty(PaypalClient.API_REQUEST_ID)) {
            return PaypalClient.API_REQUEST_ID;
        }*/

        //Generate a new UUID, store it on the db and set the variable
        UUID api_uuid = UUID.randomUUID();
        setAPIRequestId(api_uuid.toString());
        return api_uuid.toString();

    }

    /**
     * Return API User Info data
     *
     * @param delegator
     * @return
     */
    public static Map<String, Object> getAPIUserInfo(Delegator delegator) {

        Map<String, Object> returnMap = null;
        String environment = PaypalClientHelper.getPaypalEnvironment(delegator);

        //OAuth 2.0 OAuth UserInfo REST end point
        String userInfoEP = EntityUtilProperties.getPropertyValue(PAYPAL_SYSTEM_RESOURCE, "api.authUserInfoEP", delegator);

        Map<String, Object> userInfoRequestResponseMap = sendRequest(PaypalClient.REQUEST_METHOD_GET, PaypalClient.REQUEST_ACCEPT_TYPE_JSON,
                PaypalClient.REQUEST_CONTENT_TYPE_JSON, PaypalClient.AUTH_TYPE_BEARER, userInfoEP, null, environment, delegator);

        returnMap = userInfoRequestResponseMap;
        return returnMap;
    }

    /**
     * Return details of a specific paypal order ID
     * @param delegator
     * @param paypalOrderId
     * @return
     */
    public static Map<String, Object> getPaypalOrderDetails(String paypalOrderId, Delegator delegator) {

        Map<String, Object> returnMap = null;

        if (paypalOrderId == null || UtilValidate.isEmpty(paypalOrderId)) {
            return ServiceUtil.returnError("Paypal Order ID is null or empty. Cannot get details.");
        }

        String environment = PaypalClientHelper.getPaypalEnvironment(delegator);
        String orderDetailEP = PaypalClientHelper.showOrderDetailAPIEndPoint(delegator);
        //Replace {id} of the order with the paypal order ID passed in
        if (orderDetailEP.endsWith("{id}")) {
            orderDetailEP = orderDetailEP.replace("{id}", paypalOrderId);
        }
        System.out.println("order detail end point: " + orderDetailEP);

        returnMap = sendRequest(PaypalClient.REQUEST_METHOD_GET, PaypalClient.REQUEST_ACCEPT_TYPE_JSON,
                PaypalClient.REQUEST_CONTENT_TYPE_JSON, PaypalClient.AUTH_TYPE_BEARER, orderDetailEP, null, environment, delegator);

        return returnMap;
    }

    /**
     *
     * @param orderReqObjectJson
     * @param delegator
     * @return
     */
    public static Map<String, Object> createPaypalOrder(String orderReqObjectJson, Delegator delegator) {

        Map<String, Object> returnMap = null;

        if (orderReqObjectJson == null || UtilValidate.isEmpty(orderReqObjectJson)) {
            String msg = "JSON Object for Order request is null or empty. Cannot create Paypal order.";
            return ServiceUtil.returnError(msg);
        }

        String environment = PaypalClientHelper.getPaypalEnvironment(delegator);

        //API create order REST end point
        String createOrderEP = PaypalClientHelper.getOrderAPIEndPoint(delegator);

        HashMap<String, String> otherHeaderProps = new HashMap<>();
        otherHeaderProps.put("PayPal-Request-Id", getAPIRequestId(delegator));

        Map<String, Object> createOrderRequestResponseMap = sendRequest(PaypalClient.REQUEST_METHOD_POST, PaypalClient.REQUEST_ACCEPT_TYPE_JSON,
                PaypalClient.REQUEST_CONTENT_TYPE_JSON, otherHeaderProps, PaypalClient.AUTH_TYPE_BEARER, createOrderEP, orderReqObjectJson, environment, delegator);

        returnMap = createOrderRequestResponseMap;
        return returnMap;
    }

    /**
     *
     * @param orderCaptureLink
     * @param captureLinkMethod
     * @param paypalOrderId
     * @param delegator
     * @return
     */
    public static Map<String, Object> capturePaypalOrder(String orderCaptureLink, String captureLinkMethod, String paypalOrderId, Delegator delegator) {
        Map<String, Object> returnMap = null;

        if (orderCaptureLink == null || UtilValidate.isEmpty(orderCaptureLink)) {
            String msg = "Order capture link is null or emtpy for PayPal Order Id " + paypalOrderId + ". Cannot capture the payment for this order.";
            return ServiceUtil.returnError(msg);
        }

        String environment = PaypalClientHelper.getPaypalEnvironment(delegator);

        returnMap = sendRequest(captureLinkMethod.toUpperCase(), orderCaptureLink, PaypalClient.REQUEST_ACCEPT_TYPE_JSON,
                PaypalClient.REQUEST_CONTENT_TYPE_JSON, null, PaypalClient.AUTH_TYPE_BEARER, "", environment, delegator);

        return returnMap;
    }

    /**
     *
     * @param headerMap
     * @param requestBodyMap
     * @param delegator
     * @return
     */
    public static Map<String, Object> verifyWebhookSignature(HashMap<String, String> headerMap, HashMap<String, Object> requestBodyMap, Delegator delegator) {
        WebHookValidateRequestObject webHookValReqObj = PaypalEventsHelper.buildWebHookValidateRequestObject(headerMap, requestBodyMap, delegator);
        //Object serialization into json
        Gson verwhook_gson = new Gson();
        String verwhook_jsonString = verwhook_gson.toJson(webHookValReqObj);
        Debug.logWarning("@@@@@ Verify webhook json data => " + verwhook_jsonString, module);

        return verifyWebhookSignature(verwhook_jsonString, delegator);
    }

    /**
     *
     * @param webhookReqObjectJson
     * @param delegator
     * @return
     */
    private static Map<String, Object> verifyWebhookSignature(String webhookReqObjectJson, Delegator delegator) {
        Map<String, Object> returnMap = null;
        if (webhookReqObjectJson == null || UtilValidate.isEmpty(webhookReqObjectJson)) {
            String msg = "JSON Object for WebHook Verification request is null or empty. Cannot verify webhook signature.";
            return ServiceUtil.returnError(msg);
        }
        String environment = PaypalClientHelper.getPaypalEnvironment(delegator);
        //API WebHook Management verify signature REST end point
        String verifWebHookSigEP = PaypalClientHelper.getVerifyWebhookSignatureEP(delegator);
        returnMap = sendRequest(PaypalClient.REQUEST_METHOD_POST, PaypalClient.REQUEST_ACCEPT_TYPE_JSON,
                PaypalClient.REQUEST_CONTENT_TYPE_JSON, null, PaypalClient.AUTH_TYPE_BEARER, verifWebHookSigEP, webhookReqObjectJson, environment, delegator);
        return returnMap;
    }

    /**
     * Request a new Access Token and persist it.
     *
     * @param delegator
     * @return
     */
    public static synchronized Map<String, Object> requestAndStoreAccessToken(Delegator delegator) {

        Map<String, Object> returnMap = null;
        String paypalClientID = PaypalClientHelper.getPaypalClientId(delegator);
        String paypalClientSECRET = PaypalClientHelper.getPaypalClientSecret(delegator);

        //1 - Request a new Access Token
        Map<String, Object> reqNewTokenReturnMap = requestAPIAuthToken(paypalClientID, paypalClientSECRET, delegator);

        if (!ServiceUtil.isSuccess(reqNewTokenReturnMap)) {
            return reqNewTokenReturnMap;
        }

        //2 - Update Access Token Data
        if (!PaypalClient.updateAccessTokenData((String) reqNewTokenReturnMap.get("app_id"), (String) reqNewTokenReturnMap.get("access_token"),
                (String) reqNewTokenReturnMap.get("expires_in"), (String) reqNewTokenReturnMap.get("token_type"),
                (String) reqNewTokenReturnMap.get("nonce"), delegator)) {

            String msg = "Error in updating Paypal Access Token Data";
            //Debug.logError("msg", module);
            return ServiceUtil.returnError(msg);
        }

        returnMap = reqNewTokenReturnMap;
        return returnMap;
    }

    /* ################# PRIVATE METHODS ################# */
    /**
     * Get an authorization token in order to communicate via REST APIs
     *
     * @param paypalClientID
     * @param paypalClientSECRET
     * @return
     */
    private static Map<String, Object> requestAPIAuthToken(String paypalClientID, String paypalClientSECRET, Delegator delegator) {

        Map<String, Object> returnMap = null;
        JsonObject jsonResponse = null;

        if (paypalClientID == null || UtilValidate.isEmpty(paypalClientID)) {
            String msg = "PayPal Account Client ID is null or empty. Cannot creat OAuth Token.";
            Debug.logError(msg, module);
            return null;
        }

        if (paypalClientSECRET == null || UtilValidate.isEmpty(paypalClientSECRET)) {
            String msg = "PayPal Account Client SECRET is null or empty. Cannot creat OAuth Token.";
            Debug.logError(msg, module);
            return null;
        }

        String apiURL = PaypalClientHelper.getPaypalAPIUrl(delegator);

        //OAuth 2.0 Token REST end point
        String authTokenEP = PaypalClientHelper.getOAuthAccessTokenEndPoint(delegator);

        String oautURL = apiURL + authTokenEP;
        try {
            URL url = new URL(oautURL);
            HttpsURLConnection scon = (HttpsURLConnection) url.openConnection();
            scon.setDoOutput(true); //use the connection for output
            scon.setInstanceFollowRedirects(false); //default:true => automatically follow http redirection if a status code of 3XX is returned
            scon.setRequestMethod("POST");
            scon.setRequestProperty("User-Agent", "Mozilla/5.0");
            scon.setRequestProperty("charset", "utf-8");
            scon.setRequestProperty("Accept", REQUEST_ACCEPT_TYPE_JSON);
            scon.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE_FORMURLENC);
            String basic_auth = "Basic " + PaypalClient.encodeBase64(paypalClientID + ":" + paypalClientSECRET);
            scon.setRequestProperty("Authorization", basic_auth);
            scon.setUseCaches(false);

            if ("POST".equals(REQUEST_METHOD_POST)) {
                String postData = "grant_type=client_credentials";
                scon.setDoInput(true); //use the connection for input
                scon.setRequestProperty("Content-Length", "" + Integer.toString(postData.getBytes().length));
                BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(scon.getOutputStream(), "UTF-8"));
                wr.write(postData);
                wr.flush();
                wr.close();
            }

            int code = scon.getResponseCode();
            String responseMessage = scon.getResponseMessage();
            String inputLine;
            BufferedReader reader = null;

            Debug.logWarning("@@@@ Client response code: " + code + ", response message: " + scon.getResponseMessage(), module);

            if (code == 200) {
                returnMap = ServiceUtil.returnSuccess();
                reader = new BufferedReader(new InputStreamReader(scon.getInputStream()));
            } else {
                String msg = "HTTP Error while requesting a new Access Token : " + code + " - " + responseMessage;
                returnMap = ServiceUtil.returnError(msg);
                reader = new BufferedReader(new InputStreamReader(scon.getErrorStream()));
            }

            returnMap.put("http_response_code", code);
            returnMap.put("http_response_message", responseMessage);

            StringBuilder responseBuf = new StringBuilder();
            while ((inputLine = reader.readLine()) != null) {
                responseBuf.append(inputLine);
            }
            reader.close();

            jsonResponse = (JsonObject) new JsonParser().parse(responseBuf.toString());
            Debug.logWarning("@@@JsonResponse => " + jsonResponse, module);
            returnMap.put("json_response_object", jsonResponse);

            if (code == 200) {
                JsonElement tokenAppId_josn = jsonResponse.get("app_id");
                JsonElement authToken_json = jsonResponse.get("access_token");
                JsonElement tokenType_json = jsonResponse.get("token_type");
                JsonElement nonce_json = jsonResponse.get("nonce");
                JsonElement expiresIn = jsonResponse.get("expires_in");

                returnMap.put("app_id", tokenAppId_josn.getAsString());
                returnMap.put("access_token", authToken_json.getAsString());
                returnMap.put("token_type", tokenType_json.getAsString());
                returnMap.put("expires_in", expiresIn.getAsString());
                returnMap.put("nonce", nonce_json.getAsString());
            }

            scon.disconnect();

        } catch (MalformedURLException mue) {
            String msg = "Error in requestAPIAuthToken() =>  " + mue.getMessage();
            Debug.logError(msg, module);
            returnMap = ServiceUtil.returnError(msg);
        } catch (ProtocolException prote) {
            String msg = "Error in requestAPIAuthToken() =>  " + prote.getMessage();
            Debug.logError(msg, module);
            returnMap = ServiceUtil.returnError(msg);
        } catch (UnsupportedEncodingException uee) {
            String msg = "Error in requestAPIAuthToken() =>  " + uee.getMessage();
            Debug.logError(msg, module);
            returnMap = ServiceUtil.returnError(msg);
        } catch (IOException ioe) {
            String msg = "Error in requestAPIAuthToken() =>  " + ioe.getMessage();
            Debug.logError(msg, module);
            returnMap = ServiceUtil.returnError(msg);
        }

        return returnMap;
    }

    /**
     *
     * @param appId
     * @param accessToken
     * @param expiresInStr
     * @param tokenType
     * @param nonce
     * @param delegator
     * @return
     */
    private static boolean updateAccessTokenData(String appId, String accessToken, String expiresInStr, String tokenType, String nonce, Delegator delegator) {

        GenericValue mpAccessToken = null;
        boolean updateSuccess = true;

        try {
            mpAccessToken = delegator.findOne("MpAccessToken", UtilMisc.toMap("appId", appId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
        }

        Long expiresInLong = Long.valueOf("-1");
        try {
            expiresInLong = Long.valueOf(expiresInStr);
        } catch (NumberFormatException nfe) {
            Debug.logError(nfe.getMessage(), module);
        }

        //If an access token record exists, then update it, else create a new one.
        if (mpAccessToken != null) {
            mpAccessToken.set("tokenValue", accessToken);
            mpAccessToken.set("tokenTTL", expiresInLong);
            mpAccessToken.set("tokenType", tokenType);
            mpAccessToken.set("nonceValue", nonce);

            Debug.logInfo("Updating existing Access Token record", module);
            try {
                delegator.store(mpAccessToken);
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
                updateSuccess = false;
            }
        } else {
            Debug.logInfo("Creating a new Access Token record", module);
            mpAccessToken = delegator.makeValue("MpAccessToken");
            mpAccessToken.set("appId", appId);
            mpAccessToken.set("tokenValue", accessToken);
            mpAccessToken.set("tokenTTL", expiresInLong);
            mpAccessToken.set("tokenType", tokenType);
            mpAccessToken.set("nonceValue", nonce);

            try {
                delegator.create(mpAccessToken);
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
                updateSuccess = false;
            }
        }

        //Update the static value
        PaypalClient.setAPIAuthToken(accessToken);

        return updateSuccess;
    }

    /**
     * Base method to send Http requests to PayPal REST API.
     *
     * @param reqMethod
     * @param reqHdrAcceptType
     * @param reqContentType
     * @param otherHeaderAttr
     * @param authType
     * @param requestEndPoint
     * @param postData
     * @param apiEnv
     * @param delegator
     * @return
     */
    private static Map<String, Object> sendRequest(String reqMethod, String reqHdrAcceptType, String reqContentType,
                                                   HashMap<String, String> otherHeaderAttr, String authType, String requestEndPoint, String postData, String apiEnv, Delegator delegator) {

        Map<String, Object> returnMap = null;

        if (reqMethod == null || UtilValidate.isEmpty(reqMethod)) {
            String msg = "Request method is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (reqHdrAcceptType == null || UtilValidate.isEmpty(reqHdrAcceptType)) {
            String msg = "Request-Header Accept type is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (reqContentType == null || UtilValidate.isEmpty(reqContentType)) {
            String msg = "Request-Header Content-Type is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (authType == null || UtilValidate.isEmpty(authType)) {
            String msg = "Authorization type (Basic/Bearer) is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (requestEndPoint == null || UtilValidate.isEmpty(requestEndPoint)) {
            String msg = "Request end point is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (apiEnv == null || UtilValidate.isEmpty(apiEnv)) {
            String msg = "Environemnt type is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        String apiURL = PaypalClientHelper.getPaypalAPIUrl(delegator);
        String paypalClientID = PaypalClientHelper.getPaypalClientId(delegator);
        String paypalClientSECRET = PaypalClientHelper.getPaypalClientSecret(delegator);

        String requestUrl = apiURL + requestEndPoint;
        String authString = "";

        if (AUTH_TYPE_BASIC.equals(authType)) {
            authString = authType + " " + PaypalClient.encodeBase64(paypalClientID + ":" + paypalClientSECRET);
        } else if (AUTH_TYPE_BEARER.equals(authType)) {
            authString = authType + " " + getAPIAuthToken(delegator);
        }

        JsonObject jsonResponse = null;

        try {

            URL url = new URL(requestUrl);
            HttpsURLConnection scon = (HttpsURLConnection) url.openConnection();
            scon.setDoOutput(true); //use the connection for output
            scon.setInstanceFollowRedirects(false); //default:true => automatically follow http redirection if a status code of 3XX is returned
            scon.setRequestMethod(reqMethod);
            scon.setRequestProperty("User-Agent", "Mozilla/5.0");
            scon.setRequestProperty("charset", "utf-8");
            scon.setRequestProperty("Accept", REQUEST_ACCEPT_TYPE_JSON);
            scon.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE_JSON);
            scon.setRequestProperty("Authorization", authString);
            //Adding other Header Attributes
            if (otherHeaderAttr != null && !otherHeaderAttr.isEmpty()) {
                for (Map.Entry<String, String> _entry : otherHeaderAttr.entrySet()) {
                    scon.setRequestProperty(_entry.getKey(), _entry.getValue());
                }
            }
            scon.setUseCaches(false);
            scon.setDoInput(true);

            //If is a POST write data into request body
            if (REQUEST_METHOD_POST.equals(reqMethod)) {
                scon.setRequestProperty("Content-Length", "" + Integer.toString(postData.getBytes().length));
                BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(scon.getOutputStream(), "UTF-8"));
                wr.write(postData);
                wr.flush();
                wr.close();
            }

            int code = scon.getResponseCode();
            String responseMessage = scon.getResponseMessage();
            String inputLine;
            BufferedReader reader = null;

            Debug.logWarning("@@@@ Client response code: " + code + ", response message: " + responseMessage + " @@@", module);

            if (code == 200 || code == 201 || code == 202 || code == 204) {
                returnMap = ServiceUtil.returnSuccess();
                reader = new BufferedReader(new InputStreamReader(scon.getInputStream()));
            } else {
                String msg = "Error in sending request to => " + requestUrl;
                returnMap = ServiceUtil.returnError(msg);
                if (scon.getErrorStream() != null) {
                    reader = new BufferedReader(new InputStreamReader(scon.getErrorStream()));
                }
            }

            returnMap.put("http_response_code", code);
            returnMap.put("http_response_message", responseMessage);

            StringBuilder responseBuf = null;
            if (reader != null) {
                responseBuf = new StringBuilder();
                while ((inputLine = reader.readLine()) != null) {
                    responseBuf.append(inputLine);
                }
                reader.close();
            } else {
                responseBuf = new StringBuilder();
                responseBuf.append("{");
                responseBuf.append("'").append("error").append("':'").append("true").append("',");
                responseBuf.append("'").append("HTTP_RES_CODE").append("':'").append(code).append("',");
                responseBuf.append("'").append("HTTP_RES_MSG").append("':'").append(responseMessage).append("',");
                responseBuf.append("'").append("DETAIL_MSG").append("':'").append("Request failed for URL => ").append(requestUrl).append("'}");
            }

            jsonResponse = (JsonObject) new JsonParser().parse(responseBuf.toString());
            Debug.logWarning("@@@JsonResponse => " + jsonResponse, module);
            returnMap.put("json_response_object", jsonResponse);

            scon.disconnect();

        } catch (MalformedURLException mue) {
            Debug.logError("sendRequest() - " + mue, module);
        } catch (ProtocolException prote) {
            Debug.logError("sendRequest() - " + prote, module);
        } catch (UnsupportedEncodingException uee) {
            Debug.logError("sendRequest() - " + uee, module);
        } catch (IOException ioe) {
            Debug.logError("sendRequest() - " + ioe, module);
        }

        return returnMap;
    }

    /**
     * This method make a call to the complete requestUrl passed in as
     * parameter, without building it using a base URL and an End Point resource
     * location.
     *
     * @param reqMethod
     * @param requestFullUrl The complete URL for the request (contains the end
     * point)
     * @param reqHdrAcceptType
     * @param reqContentType
     * @param otherHeaderAttr
     * @param authType
     * @param postData
     * @param apiEnv
     * @param delegator
     * @return
     */
    private static Map<String, Object> sendRequest(String reqMethod, String requestFullUrl, String reqHdrAcceptType, String reqContentType,
                                                   HashMap<String, String> otherHeaderAttr, String authType, String postData, String apiEnv, Delegator delegator) {

        Map<String, Object> returnMap = null;

        if (reqMethod == null || UtilValidate.isEmpty(reqMethod)) {
            String msg = "Request method is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (reqHdrAcceptType == null || UtilValidate.isEmpty(reqHdrAcceptType)) {
            String msg = "Request-Header Accept type is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (reqContentType == null || UtilValidate.isEmpty(reqContentType)) {
            String msg = "Request-Header Content-Type is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (authType == null || UtilValidate.isEmpty(authType)) {
            String msg = "Authorization type (Basic/Bearer) is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (requestFullUrl == null || UtilValidate.isEmpty(requestFullUrl)) {
            String msg = "Request full URL is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        if (apiEnv == null || UtilValidate.isEmpty(apiEnv)) {
            String msg = "Environemnt type is null or empty. Cannot send request.";
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }

        String paypalClientID = PaypalClientHelper.getPaypalClientId(delegator);
        String paypalClientSECRET = PaypalClientHelper.getPaypalClientSecret(delegator);

        String requestUrl = requestFullUrl;
        String authString = "";

        if (AUTH_TYPE_BASIC.equals(authType)) {
            authString = authType + " " + PaypalClient.encodeBase64(paypalClientID + ":" + paypalClientSECRET);
        } else if (AUTH_TYPE_BEARER.equals(authType)) {
            authString = authType + " " + getAPIAuthToken(delegator);
        }

        JsonObject jsonResponse = null;

        try {

            URL url = new URL(requestUrl);
            HttpsURLConnection scon = (HttpsURLConnection) url.openConnection();
            scon.setDoOutput(true); //use the connection for output
            scon.setInstanceFollowRedirects(false); //default:true => automatically follow http redirection if a status code of 3XX is returned
            scon.setRequestMethod(reqMethod);
            scon.setRequestProperty("User-Agent", "Mozilla/5.0");
            scon.setRequestProperty("charset", "utf-8");
            scon.setRequestProperty("Accept", REQUEST_ACCEPT_TYPE_JSON);
            scon.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE_JSON);
            scon.setRequestProperty("Authorization", authString);
            //Adding other Header Attributes
            if (otherHeaderAttr != null && !otherHeaderAttr.isEmpty()) {
                for (Map.Entry<String, String> _entry : otherHeaderAttr.entrySet()) {
                    scon.setRequestProperty(_entry.getKey(), _entry.getValue());
                }
            }
            scon.setUseCaches(false);
            scon.setDoInput(true);

            //If is a POST write data into request body
            if (REQUEST_METHOD_POST.equals(reqMethod)) {
                scon.setRequestProperty("Content-Length", "" + Integer.toString(postData.getBytes().length));
                BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(scon.getOutputStream(), "UTF-8"));
                wr.write(postData);
                wr.flush();
                wr.close();
            }

            int code = scon.getResponseCode();
            String responseMessage = scon.getResponseMessage();
            String inputLine;
            BufferedReader reader = null;

            Debug.logWarning("@@@@ Client response code: " + code + ", response message: " + responseMessage + " @@@", module);

            if (code == 200 || code == 201 || code == 202 || code == 204) {
                returnMap = ServiceUtil.returnSuccess();
                reader = new BufferedReader(new InputStreamReader(scon.getInputStream()));
            } else {
                String msg = "Error in sending request to => " + requestUrl;
                returnMap = ServiceUtil.returnError(msg);
                if (scon.getErrorStream() != null) {
                    reader = new BufferedReader(new InputStreamReader(scon.getErrorStream()));
                }
            }

            returnMap.put("http_response_code", code);
            returnMap.put("http_response_message", responseMessage);

            StringBuilder responseBuf = null;
            if (reader != null) {
                responseBuf = new StringBuilder();
                while ((inputLine = reader.readLine()) != null) {
                    responseBuf.append(inputLine);
                }
                reader.close();
            } else {
                responseBuf = new StringBuilder();
                responseBuf.append("{");
                responseBuf.append("'").append("error").append("':'").append("true").append("',");
                responseBuf.append("'").append("HTTP_RES_CODE").append("':'").append(code).append("',");
                responseBuf.append("'").append("HTTP_RES_MSG").append("':'").append(responseMessage).append("',");
                responseBuf.append("'").append("DETAIL_MSG").append("':'").append("Request failed for URL => ").append(requestUrl).append("'}");
            }

            jsonResponse = (JsonObject) new JsonParser().parse(responseBuf.toString());
            Debug.logWarning("@@@JsonResponse => " + jsonResponse, module);
            returnMap.put("json_response_object", jsonResponse);

            scon.disconnect();

        } catch (MalformedURLException mue) {
            Debug.logError("sendRequest() - " + mue, module);
        } catch (ProtocolException prote) {
            Debug.logError("sendRequest() - " + prote, module);
        } catch (UnsupportedEncodingException uee) {
            Debug.logError("sendRequest() - " + uee, module);
        } catch (IOException ioe) {
            Debug.logError("sendRequest() - " + ioe, module);
        }

        return returnMap;
    }

    /**
     *
     * @param reqMethod
     * @param reqHdrAcceptType
     * @param reqContentType
     * @param authType
     * @param requestEndPoint
     * @param postData
     * @param apiEnv
     * @param delegator
     * @return
     */
    private static Map<String, Object> sendRequest(String reqMethod, String reqHdrAcceptType, String reqContentType,
                                                   String authType, String requestEndPoint, String postData, String apiEnv, Delegator delegator) {
        return sendRequest(PaypalClient.REQUEST_METHOD_GET, PaypalClient.REQUEST_ACCEPT_TYPE_JSON,
                PaypalClient.REQUEST_CONTENT_TYPE_JSON, null, PaypalClient.AUTH_TYPE_BEARER, requestEndPoint, null, apiEnv, delegator);
    }

    /**
     *
     * @param accessToken
     */
    private static void setAPIAuthToken(String accessToken) {
        PaypalClient.API_ACCESS_TOKEN = accessToken;
    }

    private static void setAPIRequestId(String apiRequestId) {
        PaypalClient.API_REQUEST_ID = apiRequestId;
    }

    /**
     * BASE64 ENCODING METHOD
     *
     * @param plainString
     * @return
     */
    private static String encodeBase64(String plainString) {
        if (plainString != null && !plainString.isEmpty()) {
            return Base64.getEncoder().encodeToString(plainString.getBytes());
        }
        return null;
    }

} //end class
