package paypal.beans;

import java.util.ArrayList;
import java.util.List;

public class OrderRequestObject {

    //Required
    private String intent;
    //Required
    List<PurchaseUnit> purchase_units;
    OrderRequestObject.PaymentSource payment_source;


    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<OrderRequestObject.PurchaseUnit> getPurchase_units() {
        if (this.purchase_units == null) {
            this.purchase_units = new ArrayList<>();
        }
        return this.purchase_units;
    }

    public void setPurchase_units(List<OrderRequestObject.PurchaseUnit> purchase_units) {
        this.purchase_units = purchase_units;
    }

    public OrderRequestObject.PaymentSource getPayment_source() {
        return payment_source;
    }

    public void setPayment_source(OrderRequestObject.PaymentSource payment_source) {
        this.payment_source = payment_source;
    }

    /**
     * Inner class for PurchaseUnit object
     * @author equake58
     *
     */
    public static class PurchaseUnit {
        /**
         * The total order amount with an optional breakdown that provides details, such as the total item amount, total tax amount, shipping, handling, insurance, and discounts, if any.
         * If you specify amount.breakdown, the amount equals item_total plus tax_total plus shipping plus handling plus insurance minus shipping_discount minus discount.
         * The amount must be a positive number. For listed of supported currencies and decimal precision, see the PayPal REST APIs Currency Codes.
         */
        private OrderRequestObject.Amount amount;
        /**
         * The API caller-provided external ID. Used to reconcile client transactions with PayPal transactions.
         * Appears in transaction and settlement reports but is not visible to the payer.
         */
        private String custom_id;
        /**
         * The purchase description. The maximum length of the character is dependent on the type of characters used.
         * The character length is specified assuming a US ASCII character. Depending on type of character;
         * (e.g. accented character, Japanese characters) the number of characters that that can be specified as input might not equal the permissible max length.
         */
        private String description;
        /**
         * The API caller-provided external invoice number for this order. Appears in both the payer's transaction history and the emails that the payer receives.
         */
        private String invoice_id;
        /**
         * An array of items that the customer purchases from the merchant.
         */
        private List<OrderRequestObject.PurchaseOrderItem> items;
        /**
         * The merchant who receives payment for this transaction.
         */
        private OrderRequestObject.Payee payee;
        /**
         * Any additional payment instructions to be consider during payment processing. This processing instruction is applicable for Capturing an order or Authorizing an Order.
         */
        private OrderRequestObject.PaymentInstruction payment_instruction;
        /**
         * The API caller-provided external ID for the purchase unit. Required for multiple purchase units when you must update the order through PATCH.
         * If you omit this value and the order contains only one purchase unit, PayPal sets this value to default.
         */
        private String reference_id;
        /**
         * The name and address of the person to whom to ship the items.
         */
        private OrderRequestObject.Shipping shipping;
        /**
         * The soft descriptor is the dynamic text used to construct the statement descriptor that appears on a payer's card statement.
         * If an Order is paid using the "PayPal Wallet", the statement descriptor will appear in following format on the payer's card statement: PAYPAL_prefix+(space)+merchant_descriptor+(space)+ soft_descriptor
         */
        private String soft_descriptor;

        public OrderRequestObject.Amount getAmount() {
            if (this.amount == null) {
                this.amount = new OrderRequestObject.Amount();
            }
            return this.amount;
        }
        public void setAmount(OrderRequestObject.Amount amount) {
            this.amount = amount;
        }
        public String getCustom_id() {
            return custom_id;
        }
        public void setCustom_id(String custom_id) {
            this.custom_id = custom_id;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getInvoice_id() {
            return invoice_id;
        }
        public void setInvoice_id(String invoice_id) {
            this.invoice_id = invoice_id;
        }
        public List<OrderRequestObject.PurchaseOrderItem> getItems() {
            if (this.items == null) {
                this.items = new ArrayList<>();
            }
            return this.items;
        }
        public void setItems(List<OrderRequestObject.PurchaseOrderItem> items) {
            this.items = items;
        }
        public OrderRequestObject.Payee getPayee() {
            if (this.payee == null) {
                this.payee = new OrderRequestObject.Payee();
            }
            return payee;
        }
        public void setPayee(OrderRequestObject.Payee payee) {
            this.payee = payee;
        }
        public OrderRequestObject.PaymentInstruction getPayment_instruction() {
            if (this.payment_instruction == null) {
                this.payment_instruction = new OrderRequestObject.PaymentInstruction();
            }
            return this.payment_instruction;
        }
        public void setPayment_instruction(OrderRequestObject.PaymentInstruction payment_instruction) {
            this.payment_instruction = payment_instruction;
        }
        public String getReference_id() {
            return reference_id;
        }
        public void setReference_id(String reference_id) {
            this.reference_id = reference_id;
        }
        public OrderRequestObject.Shipping getShipping() {
            if (this.shipping == null) {
                this.shipping = new OrderRequestObject.Shipping();
            }
            return this.shipping;
        }
        public void setShipping(OrderRequestObject.Shipping shipping) {
            this.shipping = shipping;
        }
        public String getSoft_descriptor() {
            return soft_descriptor;
        }
        public void setSoft_descriptor(String soft_descriptor) {
            this.soft_descriptor = soft_descriptor;
        }

    } //end inner class

    /**
     * Amount inner class
     * @author equake58
     *
     */
    public static class Amount {
        /**
         * The three-character ISO-4217 currency code that identifies the currency.
         * Required
         */
        private String currency_code;
        /**
         * The value, which might be:
         * An integer for currencies like JPY that are not typically fractional.
         * A decimal fraction for currencies like TND that are subdivided into thousandths.
         * For the required number of decimal places for a currency code, see Currency Codes.
         * Required
         */
        private String value;
        /**
         * The breakdown of the amount. Breakdown provides details such as total item amount, total tax amount, shipping, handling, insurance, and discounts, if any.
         * Optional
         */
        private OrderRequestObject.AmountBreakdown breakdown;

        public String getCurrency_code() {
            return currency_code;
        }
        public void setCurrency_code(String currency_code) {
            this.currency_code = currency_code;
        }
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }
        public OrderRequestObject.AmountBreakdown getBreakdown() {
            if (this.breakdown == null) {
                this.breakdown = new OrderRequestObject.AmountBreakdown();
            }
            return breakdown;
        }
        public void setBreakdown(OrderRequestObject.AmountBreakdown breakdown) {
            this.breakdown = breakdown;
        }
    }

    /**
     * AmountBreakDown inner class
     * @author equake58
     *
     */
    public static class AmountBreakdown {
        /**
         * The discount for all items within a given purchase_unit. discount.value can not be a negative number.
         */
        private OrderRequestObject.Money discount;
        /**
         * The handling fee for all items within a given purchase_unit. handling.value can not be a negative number.
         */
        private OrderRequestObject.Money handling;
        /**
         * The insurance fee for all items within a given purchase_unit. insurance.value can not be a negative number.
         */
        private OrderRequestObject.Money insurance;
        /**
         * The subtotal for all items. Required if the request includes purchase_units[].items[].unit_amount. Must equal the sum of (items[].unit_amount * items[].quantity) for all items. item_total.value can not be a negative number.
         */
        private OrderRequestObject.Money item_total;
        /**
         * The shipping fee for all items within a given purchase_unit. shipping.value can not be a negative number.
         */
        private OrderRequestObject.Money shipping;
        /**
         * The shipping discount for all items within a given purchase_unit. shipping_discount.value can not be a negative number.
         */
        private OrderRequestObject.Money shipping_discount;
        /**
         * The total tax for all items. Required if the request includes purchase_units.items.tax. Must equal the sum of (items[].tax * items[].quantity) for all items. tax_total.value can not be a negative number.
         */
        private OrderRequestObject.Money tax_total;

        public OrderRequestObject.Money getDiscount() {
            return discount;
        }
        public void setDiscount(OrderRequestObject.Money discount) {
            if (this.discount == null) {
                this.discount = new OrderRequestObject.Money();
            }
            this.discount = discount;
        }
        public OrderRequestObject.Money getHandling() {
            return handling;
        }
        public void setHandling(OrderRequestObject.Money handling) {
            this.handling = handling;
        }
        public OrderRequestObject.Money getInsurance() {
            if (this.insurance == null) {
                this.insurance = new OrderRequestObject.Money();
            }
            return this.insurance;
        }
        public void setInsurance(OrderRequestObject.Money insurance) {
            this.insurance = insurance;
        }
        public OrderRequestObject.Money getItem_total() {
            if (this.item_total == null) {
                this.item_total = new OrderRequestObject.Money();
            }
            return this.item_total;
        }
        public void setItem_total(OrderRequestObject.Money item_total) {
            this.item_total = item_total;
        }
        public OrderRequestObject.Money getShipping() {
            if (this.shipping == null) {
                this.shipping = new OrderRequestObject.Money();
            }
            return this.shipping;
        }
        public void setShipping(OrderRequestObject.Money shipping) {
            this.shipping = shipping;
        }
        public OrderRequestObject.Money getShipping_discount() {
            if (this.shipping_discount == null) {
                this.shipping_discount = new OrderRequestObject.Money();
            }
            return this.shipping_discount;
        }
        public void setShipping_discount(OrderRequestObject.Money shipping_discount) {
            this.shipping_discount = shipping_discount;
        }
        public OrderRequestObject.Money getTax_total() {
            if (this.tax_total == null) {
                this.tax_total = new OrderRequestObject.Money();
            }
            return this.tax_total;
        }
        public void setTax_total(OrderRequestObject.Money tax_total) {
            this.tax_total = tax_total;
        }
    }

    /**
     * Money inner class
     * @author equake58
     *
     */
    public static class Money {
        /**
         * The three-character ISO-4217 currency code that identifies the currency.
         */
        private String currency_code;
        /**
         * The value, which might be:
         * An integer for currencies like JPY that are not typically fractional.
         * A decimal fraction for currencies like TND that are subdivided into thousandths.
         * For the required number of decimal places for a currency code, see Currency Codes.
         */
        private String value;
        /**
         * Get ISO-4217 three letters currency code
         * @return
         */
        public String getCurrency_code() {
            return currency_code;
        }
        /**
         * Set ISO-4217 three letters currency code
         * @param currency_code
         */
        public void setCurrency_code(String currency_code) {
            this.currency_code = currency_code;
        }
        /**
         * Get amount value
         * @return
         */
        public String getValue() {
            return value;
        }
        /**
         * Set amount value.
         * The value, which might be:
         * An integer for currencies like JPY that are not typically fractional.
         * A decimal fraction for currencies like TND that are subdivided into thousandths.
         * @param value
         */
        public void setValue(String value) {
            this.value = value;
        }
    }
    /**
     * PurchaseOrderItem inner class
     * @author equake58
     *
     */
    public static class PurchaseOrderItem {
        /**
         * The item name or title.
         */
        private String name;
        /**
         * The item quantity. Must be a whole number.
         */
        private String quantity;
        /**
         * The item price or rate per unit.
         * If you specify unit_amount, purchase_units[].amount.breakdown.item_total is required. Must equal unit_amount * quantity for all items. unit_amount.value can not be a negative number.
         */
        private OrderRequestObject.Money unit_amount;
        /**
         * The item category type.
         * The possible values are:
         * DIGITAL_GOODS. Goods that are stored, delivered, and used in their electronic format. This value is not currently supported for API callers that leverage the PayPal for Commerce Platform product.
         * PHYSICAL_GOODS. A tangible item that can be shipped with proof of delivery.
         * DONATION. A contribution or gift for which no good or service is exchanged, usually to a not for profit organization.
         */
        private String category;
        /**
         * The detailed item description.
         */
        private String description;
        /**
         * The stock keeping unit (SKU) for the item.
         */
        private String sku;
        /**
         * The item tax for each unit. If tax is specified, purchase_units[].amount.breakdown.tax_total is required. Must equal tax * quantity for all items. tax.value can not be a negative number.
         */
        private OrderRequestObject.Money tax;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getQuantity() {
            return quantity;
        }
        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }
        public OrderRequestObject.Money getUnit_amount() {
            if (this.unit_amount == null) {
                this.unit_amount = new OrderRequestObject.Money();
            }
            return this.unit_amount;
        }
        public void setUnit_amount(OrderRequestObject.Money unit_amount) {
            this.unit_amount = unit_amount;
        }
        public String getCategory() {
            return category;
        }
        public void setCategory(String category) {
            this.category = category;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getSku() {
            return sku;
        }
        public void setSku(String sku) {
            this.sku = sku;
        }
        public OrderRequestObject.Money getTax() {
            if (this.tax == null) {
                this.tax = new OrderRequestObject.Money();
            }
            return this.tax;
        }
        public void setTax(OrderRequestObject.Money tax) {
            this.tax = tax;
        }
    }

    /**
     * Payee (Merchant) data
     * @author equake58
     *
     */
    public static class Payee {
        /**
         * The email address of merchant.
         */
        private String email_address;
        /**
         * The PayPal payer ID, which is a masked version of the PayPal account number intended for use with third parties.
         * The account number is reversibly encrypted and a proprietary variant of Base32 is used to encode the result.
         */
        private String merchant_id;
        public String getEmail_address() {
            return email_address;
        }
        public void setEmail_address(String email_address) {
            this.email_address = email_address;
        }
        public String getMerchant_id() {
            return merchant_id;
        }
        public void setMerchant_id(String merchant_id) {
            this.merchant_id = merchant_id;
        }

    }

    /**
     * PaymentInstruction inner class
     * @author equake58
     *
     */
    public static class PaymentInstruction {
        /**
         * The funds that are held payee by the marketplace/platform.
         * This field is only applicable to merchants that been enabled for PayPal Commerce Platform for Marketplaces and Platforms capability.
         */
        private String disbursement_mode;
        /**
         * This field is only enabled for selected merchants/partners to use and provides the ability to trigger a specific pricing rate/plan for a payment transaction.
         * The list of eligible 'payee_pricing_tier_id' would be provided to you by your Account Manager.
         * Specifying values other than the one provided to you by your account manager would result in an error.
         */
        private String payee_pricing_tier_id;
        /**
         * FX identifier generated returned by PayPal to be used for payment processing in order to honor FX rate (for eligible integrations) to be used when amount is settled/received into the payee account.
         */
        private String payee_receivable_fx_rate_id;
        /**
         * An array of various fees, commissions, tips, or donations. This field is only applicable to merchants that been enabled for PayPal Commerce Platform for Marketplaces and Platforms capability.
         */
        private List<OrderRequestObject.PlatformFee> platform_fees;

        public String getDisbursement_mode() {
            return disbursement_mode;
        }
        public void setDisbursement_mode(String disbursement_mode) {
            this.disbursement_mode = disbursement_mode;
        }
        public String getPayee_pricing_tier_id() {
            return payee_pricing_tier_id;
        }
        public void setPayee_pricing_tier_id(String payee_pricing_tier_id) {
            this.payee_pricing_tier_id = payee_pricing_tier_id;
        }
        public String getPayee_receivable_fx_rate_id() {
            return payee_receivable_fx_rate_id;
        }
        public void setPayee_receivable_fx_rate_id(String payee_receivable_fx_rate_id) {
            this.payee_receivable_fx_rate_id = payee_receivable_fx_rate_id;
        }
        public List<OrderRequestObject.PlatformFee> getPlatform_fees() {
            if (this.platform_fees == null) {
                this.platform_fees = new ArrayList<>();
            }
            return this.platform_fees;
        }
        public void setPlatform_fees(List<OrderRequestObject.PlatformFee> platform_fees) {
            this.platform_fees = platform_fees;
        }
    }

    /**
     * PaymentFee inner class
     * @author equake58
     *
     */
    public static class PlatformFee {
        private OrderRequestObject.Money amount;
        private OrderRequestObject.Payee payee;

        public OrderRequestObject.Money getAmount() {
            if (this.amount == null) {
                this.amount = new OrderRequestObject.Money();
            }
            return this.amount;
        }
        public void setAmount(OrderRequestObject.Money amount) {
            this.amount = amount;
        }
        public OrderRequestObject.Payee getPayee() {
            return payee;
        }
        public void setPayee(OrderRequestObject.Payee payee) {
            if (this.payee == null) {
                this.payee = new OrderRequestObject.Payee();
            }
            this.payee = payee;
        }
    }

    /**
     * Shipping inner class
     * @author equake58
     *
     */
    public static class Shipping {
        /**
         * The address of the person to whom to ship the items. Supports only the address_line_1, address_line_2, admin_area_1, admin_area_2, postal_code, and country_code properties.
         */
        private OrderRequestObject.Address address;
        /**
         * The name of the person to whom to ship the items. Supports only the full_name property.
         */
        private OrderRequestObject.CustomerName name;
        /**
         * The method by which the payer wants to get their items from the payee e.g shipping, in-person pickup. Either type or options but not both may be present.
         * The possible values are:
         * SHIPPING. The payer intends to receive the items at a specified address.
         * PICKUP_IN_PERSON. The payer intends to pick up the items from the payee in person.
         */
        private String type;

        public OrderRequestObject.Address getAddress() {
            if (this.address == null) {
                this.address = new OrderRequestObject.Address();
            }
            return this.address;
        }
        public void setAddress(OrderRequestObject.Address address) {
            this.address = address;
        }
        public OrderRequestObject.CustomerName getName() {
            if (this.name == null) {
                this.name = new OrderRequestObject.CustomerName();
            }
            return this.name;
        }
        public void setName(OrderRequestObject.CustomerName name) {
            this.name = name;
        }
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
    }

    /**
     * Address inner class
     * @author equake58
     *
     */
    public static class Address {
        /**
         * The two-character ISO 3166-1 code that identifies the country or region.
         * Required
         */
        private String country_code;
        /**
         * The first line of the address. For example, number or street. For example, 173 Drury Lane.
         * Required for data entry and compliance and risk checks. Must contain the full address.
         */
        private String address_line_1;
        /**
         * The second line of the address. For example, suite or apartment number.
         */
        private String address_line_2;
        /**
         * The highest level sub-division in a country, which is usually a province, state, or ISO-3166-2 subdivision. Format for postal delivery. For example, CA and not California. Value, by country,
         */
        private String admin_area_1;
        /**
         * A city, town, or village. Smaller than admin_area_level_1.
         */
        private String admin_area_2;
        /**
         * The postal code, which is the zip code or equivalent. Typically required for countries with a postal code or an equivalent. See postal code.
         */
        private String postal_code;

        public String getCountry_code() {
            return country_code;
        }
        public void setCountry_code(String country_code) {
            this.country_code = country_code;
        }
        public String getAddress_line_1() {
            return address_line_1;
        }
        public void setAddress_line_1(String address_line_1) {
            this.address_line_1 = address_line_1;
        }
        public String getAddress_line_2() {
            return address_line_2;
        }
        public void setAddress_line_2(String address_line_2) {
            this.address_line_2 = address_line_2;
        }
        public String getAdmin_area_1() {
            return admin_area_1;
        }
        public void setAdmin_area_1(String admin_area_1) {
            this.admin_area_1 = admin_area_1;
        }
        public String getAdmin_area_2() {
            return admin_area_2;
        }
        public void setAdmin_area_2(String admin_area_2) {
            this.admin_area_2 = admin_area_2;
        }
        public String getPostal_code() {
            return postal_code;
        }
        public void setPostal_code(String postal_code) {
            this.postal_code = postal_code;
        }
    }

    /**
     * CustomerName inner class
     * @author equake58
     *
     */
    public static class CustomerName {
        /**
         * When the party is a person, the party's full name.
         */
        private String full_name;

        public String getFull_name() {
            return full_name;
        }
        public void setFull_name(String full_name) {
            this.full_name = full_name;
        }
    }
    /**
     * PaymentSource inner class
     * @author equake58
     *
     */
    public static class PaymentSource {
        /**
         * Indicates that PayPal Wallet is the payment source. Main use of this selection is to provide additional instructions associated with this choice like vaulting.
         */
        OrderRequestObject.PayPalWallet paypal;
        /**
         * The payment card to use to fund a payment. Can be a credit or debit card.
         * USed in the Advanced Payment integration.
         */
        OrderRequestObject.PayPalCard card;

        public OrderRequestObject.PayPalWallet getPaypal() {
            return paypal;
        }

        public void setPaypal(OrderRequestObject.PayPalWallet paypal) {
            this.paypal = paypal;
        }

        public OrderRequestObject.PayPalCard getCard() {
            return card;
        }

        public void setCard(OrderRequestObject.PayPalCard card) {
            this.card = card;
        }

    }
    /**
     * PayPal payment source inner class
     * @author equake58
     *
     */
    public static class PayPalWallet {
        /**
         * The address of the PayPal account holder. Supports only the address_line_1, address_line_2,
         * admin_area_1, admin_area_2, postal_code, and country_code properties. Also referred to as the billing address of the customer.
         */
        OrderRequestObject.Address address;
        /**
         * The birth date of the PayPal account holder in YYYY-MM-DD format.
         */
        String birth_date;
        /**
         * The email address of the PayPal account holder.
         */
        String email_address;
        /**
         * Customizes the payer experience during the approval process for payment with PayPal.
         * Note: Partners and Marketplaces might configure brand_name and shipping_preference during partner account setup, which overrides the request values.
         */
        OrderRequestObject.ExperienceContext experience_context;
        /**
         * The name of the PayPal account holder. Supports only the given_name and surname properties.
         */
        OrderRequestObject.PayPalWalletName name;
        /**
         * The tax information of the PayPal account holder. Required only for Brazilian PayPal account holder's. Both tax_id and tax_id_type are required.
         */
        OrderRequestObject.TaxInfo tax_info;

        public OrderRequestObject.Address getAddress() {
            return address;
        }
        public void setAddress(OrderRequestObject.Address address) {
            this.address = address;
        }
        public String getBirth_date() {
            return birth_date;
        }
        public void setBirth_date(String birth_date) {
            this.birth_date = birth_date;
        }
        public String getEmail_address() {
            return email_address;
        }
        public void setEmail_address(String email_address) {
            this.email_address = email_address;
        }
        public OrderRequestObject.ExperienceContext getExperience_context() {
            return experience_context;
        }
        public void setExperience_context(OrderRequestObject.ExperienceContext experience_context) {
            this.experience_context = experience_context;
        }
        public OrderRequestObject.PayPalWalletName getName() {
            return name;
        }
        public void setName(OrderRequestObject.PayPalWalletName name) {
            this.name = name;
        }
        public OrderRequestObject.TaxInfo getTax_info() {
            return tax_info;
        }
        public void setTax_info(OrderRequestObject.TaxInfo tax_info) {
            this.tax_info = tax_info;
        }
    }

    /**
     *
     * @author equake58
     *
     */
    public static class PayPalCard {
        /**
         *
         */
        private String name;
        /**
         *
         */
        private String number;
        /**
         *
         */
        private String security_code;
        /**
         *
         */
        private String expiry;
        /**
         *
         */
        private OrderRequestObject.Address billing_address;
        /**
         *
         */
        private OrderRequestObject.Attributes attributes;
        /**
         * Customizes the payer experience during the approval process for payment with PayPal.
         * Note: Partners and Marketplaces might configure brand_name and shipping_preference during partner account setup, which overrides the request values.
         */
        OrderRequestObject.ExperienceContext experience_context;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getNumber() {
            return number;
        }
        public void setNumber(String number) {
            this.number = number;
        }
        public String getSecurity_code() {
            return security_code;
        }
        public void setSecurity_code(String security_code) {
            this.security_code = security_code;
        }
        public String getExpiry() {
            return expiry;
        }
        public void setExpiry(String expiry) {
            this.expiry = expiry;
        }
        public OrderRequestObject.Address getBilling_address() {
            return billing_address;
        }
        public void setBilling_address(OrderRequestObject.Address billing_address) {
            this.billing_address = billing_address;
        }
        public OrderRequestObject.Attributes getAttributes() {
            return attributes;
        }
        public void setAttributes(OrderRequestObject.Attributes attributes) {
            this.attributes = attributes;
        }
        public OrderRequestObject.ExperienceContext getExperience_context() {
            return experience_context;
        }
        public void setExperience_context(OrderRequestObject.ExperienceContext experience_context) {
            this.experience_context = experience_context;
        }

    }

    /**
     *
     * @author equake58
     *
     */
    public static class Attributes {
        private OrderRequestObject.VerificationAttribute verification;

        public OrderRequestObject.VerificationAttribute getVerification() {
            return verification;
        }

        public void setVerification(OrderRequestObject.VerificationAttribute verification) {
            this.verification = verification;
        }

    }

    /**
     *
     * @author equake58
     *
     */
    public static class VerificationAttribute {
        private String method;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }
    }

    /**
     * ExperienceContext inner class
     * @author equake58
     *
     */
    public static class ExperienceContext {
        /**
         * The label that overrides the business name in the PayPal account on the PayPal site. The pattern is defined by an external party and supports Unicode.
         */
        private String brand_name;
        /**
         * The URL where the customer is redirected after the customer cancels the payment.
         */
        private String cancel_url;
        /**
         * The type of landing page to show on the PayPal site for customer checkout.
         * The possible values are:
         * LOGIN. When the customer clicks PayPal Checkout, the customer is redirected to a page to log in to PayPal and approve the payment.
         * GUEST_CHECKOUT. When the customer clicks PayPal Checkout, the customer is redirected to a page to enter credit or debit card and other relevant billing information required to complete the purchase.
         * This option has previously been also called as 'BILLING'
         * NO_PREFERENCE. When the customer clicks PayPal Checkout, the customer is redirected to either a page to log in to PayPal and approve the payment or to a page to enter credit or debit card and other
         * relevant billing information required to complete the purchase, depending on their previous interaction with PayPal.
         */
        private String landing_page;
        /**
         * The BCP 47-formatted locale of pages that the PayPal payment experience shows. PayPal supports a five-character code. For example, da-DK, he-IL, id-ID, ja-JP, no-NO, pt-BR, ru-RU, sv-SE, th-TH, zh-CN, zh-HK, or zh-TW.
         */
        private String locale;
        /**
         * The merchant-preferred payment methods.
         * The possible values are:
         * UNRESTRICTED. Accepts any type of payment from the customer.
         * IMMEDIATE_PAYMENT_REQUIRED. Accepts only immediate payment from the customer. For example, credit card, PayPal balance, or instant ACH. Ensures that at the time of capture, the payment does not have the `pending` status.
         */
        private String payment_method_preference;
        /**
         * The URL where the customer is redirected after the customer approves the payment.
         */
        private String return_url;
        /**
         * The location from which the shipping address is derived.
         * The possible values are:
         * GET_FROM_FILE. Get the customer-provided shipping address on the PayPal site.
         * NO_SHIPPING. Redacts the shipping address from the PayPal site. Recommended for digital goods.
         * SET_PROVIDED_ADDRESS. Get the merchant-provided address. The customer cannot change this address on the PayPal site. If merchant does not pass an address, customer can choose the address on PayPal pages.
         */
        private String shipping_preference;
        /**
         * Configures a Continue or Pay Now checkout flow.
         * The possible values are:
         * CONTINUE. After you redirect the customer to the PayPal payment page, a Continue button appears.
         * Use this option when the final amount is not known when the checkout flow is initiated and you want to redirect the customer to the merchant page without processing the payment.
         * PAY_NOW. After you redirect the customer to the PayPal payment page, a Pay Now button appears. Use this option when the final amount is known when the checkout is initiated and you want to process the payment immediately when the customer clicks Pay Now.
         */
        private String user_action;


        public String getBrand_name() {
            return brand_name;
        }
        public void setBrand_name(String brand_name) {
            this.brand_name = brand_name;
        }
        public String getCancel_url() {
            return cancel_url;
        }
        public void setCancel_url(String cancel_url) {
            this.cancel_url = cancel_url;
        }
        public String getLanding_page() {
            return landing_page;
        }
        public void setLanding_page(String landing_page) {
            this.landing_page = landing_page;
        }
        public String getLocale() {
            return locale;
        }
        public void setLocale(String locale) {
            this.locale = locale;
        }
        public String getPayment_method_preference() {
            return payment_method_preference;
        }
        public void setPayment_method_preference(String payment_method_preference) {
            this.payment_method_preference = payment_method_preference;
        }
        public String getReturn_url() {
            return return_url;
        }
        public void setReturn_url(String return_url) {
            this.return_url = return_url;
        }
        public String getShipping_preference() {
            return shipping_preference;
        }
        public void setShipping_preference(String shipping_preference) {
            this.shipping_preference = shipping_preference;
        }
        public String getUser_action() {
            return user_action;
        }
        public void setUser_action(String user_action) {
            this.user_action = user_action;
        }
    }
    /**
     * PayPalWalletName inner class
     * @author equake58
     *
     */
    public static class PayPalWalletName {
        /**
         * When the party is a person, the party's given, or first, name.
         */
        private String given_name;
        /**
         * When the party is a person, the party's surname or family name. Also known as the last name. Required when the party is a person. Use also to store multiple surnames including the matronymic, or mother's, surname.
         */
        private String surname;

        public String getGiven_name() {
            return given_name;
        }
        public void setGiven_name(String given_name) {
            this.given_name = given_name;
        }
        public String getSurname() {
            return surname;
        }
        public void setSurname(String surname) {
            this.surname = surname;
        }
    }
    /**
     * TaxInfo inner class
     * @author equake58
     *
     */
    public static class TaxInfo {
        /**
         * The customer's tax ID value.
         */
        private String tax_id;
        /**
         * The customer's tax ID type.
         * The possible values are:
         * BR_CPF. The individual tax ID type, typically is 11 characters long.
         * BR_CNPJ. The business tax ID type, typically is 14 characters long.
         */
        private String tax_id_type;

        public String getTax_id() {
            return tax_id;
        }
        public void setTax_id(String tax_id) {
            this.tax_id = tax_id;
        }
        public String getTax_id_type() {
            return tax_id_type;
        }
        public void setTax_id_type(String tax_id_type) {
            this.tax_id_type = tax_id_type;
        }
    }

} //end class
