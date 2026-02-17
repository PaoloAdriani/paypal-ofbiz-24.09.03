package paypal.beans;

public class PaypalRequestEnum {
    /**
     * The server returns a minimal response to optimize communication between the API caller and the server. A minimal response includes the id, status and HATEOAS links.
     */
    public static final String HDR_SERVER_RESPONSE_PREFER_MINIMAL = "return=minimal";
    /**
     * The server returns a complete resource representation, including the current state of the resource.
     */
    public static final String HDR_SERVER_RESPONSE_PREFER_REPRESENT = "return=representation";
    /**
     * The merchant intends to capture payment immediately after the customer makes a payment.
     */
    public static final String REQ_INTENT_CAPTURE = "CAPTURE";
    /**
     * The merchant intends to authorize a payment and place funds on hold after the customer makes a payment.
     * Authorized payments are best captured within three days of authorization but are available to capture for up to 29 days.
     * After the three-day honor period, the original authorized payment expires and you must re-authorize the payment.
     * You must make a separate request to capture payments on demand. This intent is not supported when you have more than one
     * 'purchase_unit' within your order.
     */
    public static final String REQ_INTENT_AUTH = "AUTHORIZE";
    /**
     *  Goods that are stored, delivered, and used in their electronic format. This value is not currently supported for API callers that leverage the PayPal for Commerce Platform product.
     */
    public static final String REQ_PURCH_ITEM_CATEGORY_DG = "DIGITAL_GOODS";
    /**
     * A tangible item that can be shipped with proof of delivery.
     */
    public static final String REQ_PURCH_ITEM_CATEGORY_PG = "PHYSICAL_GOODS";
    /**
     * A contribution or gift for which no good or service is exchanged, usually to a not for profit organization.
     */
    public static final String REQ_PURCH_ITEM_CATEGORY_DONATE = "DONATION";
    /**
     * The payer intends to receive the items at a specified address.
     */
    public static final String SHIP_DETAIL_TYPE_SHIPPING = "SHIPPING";
    /**
     * The payer intends to pick up the items from the payee in person.
     */
    public static final String SHIP_DETAIL_TYPE_PICK_IN_PERSON = "PICKUP_IN_PERSON";
    /**
     * The funds are released to the merchant immediately.
     */
    public static final String PAYMENT_DISBURSEMENT_MODE_INSTANT = "INSTANT";
    /**
     * The funds are held for a finite number of days. The actual duration depends on the region and type of integration.
     * You can release the funds through a referenced payout. Otherwise, the funds disbursed automatically after the specified duration.
     */
    public static final String PAYMENT_DISBURSEMENT_MODE_DELAYED = "DELAYED";
    /**
     * When the customer clicks PayPal Checkout, the customer is redirected to a page to log in to PayPal and approve the payment.
     */
    public static final String EXP_CONTEXT_LANDING_PAGE_LOGIN = "LOGIN";
    /**
     * When the customer clicks PayPal Checkout, the customer is redirected to a page to enter credit or debit card and other relevant billing information required to complete the purchase.
     * This option has previously been also called as 'BILLING'
     */
    public static final String EXP_CONTEXT_LANDING_PAGE_GUEST = "GUEST_CHECKOUT";
    /**
     * When the customer clicks PayPal Checkout, the customer is redirected to either a page to log in to PayPal and approve the payment or to a page to enter credit or debit card and other
     *  relevant billing information required to complete the purchase, depending on their previous interaction with PayPal.
     */
    public static final String EXP_CONTEXT_LANDING_PAGE_NOPREF = "NO_PREFERENCE";
    /**
     * Accepts only immediate payment from the customer. For example, credit card, PayPal balance, or instant ACH. Ensures that at the time of capture, the payment does not have the `pending` status.
     */
    public static final String EXP_CONTEXT_PAYMENT_PREF_IMMED = "IMMEDIATE_PAYMENT_REQUIRED";
    /**
     * Accepts any type of payment from the customer.
     */
    public static final String EXP_CONTEXT_PAYMENT_PREF_UNREST = "UNRESTRICTED";
    /**
     * After you redirect the customer to the PayPal payment page, a Continue button appears. Use this option when the final amount is not known when the checkout flow is
     * initiated and you want to redirect the customer to the merchant page without processing the payment.
     */
    public static final String EXP_CONTEXT_USER_ACTION_CONTINUE = "CONTINUE";
    /**
     * After you redirect the customer to the PayPal payment page, a Pay Now button appears. Use this option when the final amount is known when the checkout is
     * initiated and you want to process the payment immediately when the customer clicks Pay Now.
     */
    public static final String EXP_CONTEXT_USER_ACTION_PAYNOW = "PAY_NOW";
    /**
     * The status of the signature verification.
     * WebHook Signature verification SUCCESS.
     */
    public static final String VERIFY_WEBHOOK_SIG_SUCCESS = "SUCCESS";
    /**
     * The status of the signature verification.
     * WebHook Signature verification FAILURE.
     */
    public static final String VERIFY_WEBHOOK_SIG_FAIL = "FAILURE";

    /**
     * When a payment for an order has been Approved by the customer and can be captured.
     */
    public static final String PAYPAL_ORDER_APPROVED = "APPROVED";
    /**
     * A payment for an order has been Captured and the order purchase is completed.
     */
    public static final String PAYPAL_ORDER_COMPLETED = "COMPLETED";
    /**
     *
     */
    public static final String PAYPAL_CAPTURE_COMPLETED = "COMPLETED";
    /**
     *
     */
    public static final String PAYPAL_CAPTURE_DECLINED = "DECLINED";
    /**
     *
     */
    public static final String PAYPAL_CAPTURE_PENDING = "PENDING";
    /**
     *
     */
    public static final String PAYPAL_CAPTURE_FAILED = "FAILED";

    /**
     * Notifiy Event Type Capture Completed: a payment capture has been completed.
     */
    public static final String NOTIF_ET_PAYMENT_CAPTURE_COMPLETED = "PAYMENT.CAPTURE.COMPLETED";
    /**
     * Notifiy Event Type Capture dENIED: a payment capture has been denied.
     */
    public static final String NOTIF_ET_PAYMENT_CAPTURE_DENIED = "PAYMENT.CAPTURE.DENIED";
    /**
     * Notifiy Event Checkout Payment Approved: a payment has been Approved for an order
     */
    public static final String NOTIF_ET_CHECKOUT_ORDER_APPROV = "CHECKOUT.ORDER.APPROVED";

} //end class
