package paypal.iface
// @giulio Retrieve information data about an order and its items for
// Google Tag Manager Ecommerce plugin

import org.apache.commons.lang.StringUtils;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.condition.*;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.entity.util.*;

def delegator = request.getAttribute("delegator");
def orderId = request.getParameter("orderId");

String tenantId = delegator.getDelegatorTenantId().toLowerCase();


// TRACKING DATALAYER GA ANALITYCS USED BY ABH AND NEIRAMI
String gtmContainerId = EntityUtilProperties.getPropertyValue(tenantId,"gtmContainerId",delegator);

String brandName = EntityUtilProperties.getPropertyValue(tenantId,"brandName",delegator);


// TRACKING MAGNEWS USED BY ABH
String scriptBrand = EntityUtilProperties.getPropertyValue(tenantId,"scriptBrand",delegator);  

String magnewsApiKey = EntityUtilProperties.getPropertyValue(tenantId,"magnewsApiKey",delegator);

String trackerHostname = EntityUtilProperties.getPropertyValue(tenantId,"trackerHostname",delegator);

String goalBrand = EntityUtilProperties.getPropertyValue(tenantId,"goalBrand",delegator);


// FACEBOOK PIXEL ID FOR PURCHASE EVENT USED BY PIERANTONIOGASPARI
String fbPixelId = EntityUtilProperties.getPropertyValue(tenantId,"fbPixelId",delegator);


// EVENT AND SEND TO USED FOR GTAG BY NEIRAMI
String eventGtag = EntityUtilProperties.getPropertyValue(tenantId,"eventNameGtag",delegator);

String sendToGtag = EntityUtilProperties.getPropertyValue(tenantId,"sendToGtag",delegator);



Debug.logWarning("******* orderId found into request *******"+orderId, "GoogleTagMgrOrderData.groovy");

if(orderId == null) {
	
	Debug.logWarning("******* No orderId found in the request parameters *******", "GoogleTagMgrOrderData.groovy");
	
	orderId = parameters.orderId;
	
	Debug.logWarning("******* orderId found in the parameters *******"+orderId, "GoogleTagMgrOrderData.groovy");
	
	if(orderId == null) {
		
		orderId = request.getParameter("transactionid");
		
		Debug.logWarning("******* orderId found into transactionId *******"+orderId, "GoogleTagMgrOrderData.groovy");
		
		if(orderId == null) {
			
			orderToken = request.getParameter("orderToken");
			
			if(orderToken == null) {
				
				Debug.logWarning("******* No orderId found with this orderToken *******"+orderToken, "GoogleTagMgrOrderData.groovy");
				
				return null;
			
			}else{

				Debug.logWarning("******* Founded orderToken *******"+orderToken, "GoogleTagMgrOrderData.groovy");
				
				orderId = getOrderIdFromScalapayOrderToken(orderToken);
				
			}
		}
	}
}

//Get the OrderHeader
orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId",orderId), false);

if(orderHeader == null) {
	Debug.logWarning("******* No orderHeader found for orderId: "+orderId+" *******", "GoogleTagMgrOrderData.groovy");
	return null;
}

OrderReadHelper orh = new OrderReadHelper(orderHeader);

//Get the product store name and save it in the context
productStoreId = orderHeader.productStoreId;

productStore = delegator.findOne("ProductStore", UtilMisc.toMap("productStoreId", productStoreId), false);

productStoreGroupId = productStore.primaryStoreGroupId;

currencyUomId = productStore.defaultCurrencyUomId;

context.currencyUomId = currencyUomId;

shippingCosts = null;

//Get the orderItems (created/approved)

EntityCondition itemOrderCondition = EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId);

EntityCondition itemStatusCond = EntityCondition.makeCondition(EntityOperator.OR,
		EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CREATED"),
		EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));

EntityCondition itemCond = EntityCondition.makeCondition(EntityOperator.AND, itemOrderCondition, itemStatusCond);

orderItemList = delegator.findList("OrderItem", itemCond, null, null, null, false);

firstItem = EntityUtil.getFirst(orderItemList);

//Get order adjustment: shipping charges
orderAdjustmentList = delegator.findList("OrderAdjustment",
		EntityCondition.makeCondition(EntityOperator.AND,
		EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
		EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "SHIPPING_CHARGES")),
		null,null,null,false);

if(orderAdjustmentList.size > 0)
{	
	firstAdjustment = EntityUtil.getFirst(orderAdjustmentList);
	shippingCosts = (firstAdjustment.amount).intValue();
	
}else {
	shippingCosts = BigDecimal.ZERO;
}

//put the shipping cost in the context
grandTotal = orderHeader.grandTotal;

/***** TAXES AND ITEM SUBTOTAL******/

//Default Italian Tax Percentage
defaultTaxPercentage = new BigDecimal("0.22");
taxPercentage = new BigDecimal("0.22");

itemSubTotal = BigDecimal.ZERO;

//Retrieve TaxPercentag from product price record
if(orderItemList.size() > 0) {

	firstItem = EntityUtil.getFirst(orderItemList);

	varProductId = firstItem.productId;

	//TRETRIEVE THE VIRTUAL PRODUCT (PARENT)
	if(varProductId.contains(".")) 
	{
		//then remove the size specification
		virProductId = varProductId.substring(0, varProductId.lastIndexOf("."));

		//retrieve a price record to get taxPercentage value for this store group
		productPriceList = delegator.findList("ProductPrice",
				EntityCondition.makeCondition(EntityOperator.AND,
				EntityCondition.makeCondition("productId", EntityOperator.EQUALS, virProductId),
				EntityCondition.makeCondition("productPricePurposeId", EntityOperator.EQUALS, "PURCHASE"),
				EntityCondition.makeCondition("productPriceTypeId", EntityOperator.EQUALS, "LIST_PRICE"),
				EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, productStoreGroupId),
				EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, currencyUomId)), null, null, null, false);

		if(productPriceList != null && productPriceList.size() > 0) 
		{
			firstPrice = EntityUtil.getFirst(productPriceList);

			taxPercentage = firstPrice.taxPercentage;

			if(taxPercentage == null)
				taxPercentage = defaultTaxPercentage;

		}else {
			
			taxPercentage = defaultTaxPercentage;
		}
	}

	//##### After tax calculation get orderItemSUbtotal #####
	for(orderItem in orderItemList) {

		itemSubTotal = itemSubTotal.add(orh.getOrderItemSubTotal(orderItem));
		
	}
}

//extra check
if(taxPercentage == null)
	taxPercentage = defaultTaxPercentage;

totalTaxValue = grandTotal.multiply(taxPercentage);

transactionNoTax = grandTotal.subtract(totalTaxValue).setScale(2, BigDecimal.ROUND_HALF_UP);

context.transactionTotal = grandTotal;
context.orderId = orderId;
context.shippingCosts = shippingCosts;
context.storeName = productStore.storeName;
context.orderItems = orderItemList;
context.totalTaxValue = totalTaxValue;

context.gtmContainerId = gtmContainerId;
context.brandName = brandName;

context.scriptBrand = scriptBrand;
context.magnewsApiKey = magnewsApiKey;
context.trackerHostname = trackerHostname;
context.goalBrand = goalBrand;

context.fbPixelId = fbPixelId; 

context.eventGtag = eventGtag;
context.sendToGtag = sendToGtag;


def getOrderIdFromScalapayOrderToken(orderToken) {
	
	orderHeader = null;
	
	orderList = null;
	
	orderList = delegator.findList("OrderHeader", EntityCondition.makeCondition("mpScalapayOrderToken", orderToken), null, UtilMisc.toList("orderId"), null, false);
	
	if(orderList == null || orderList.isEmpty()) {
		Debug.logWarning("No Scalapay order found with orderToken ["+orderToken+"].", "GoogleTagMgrOrderData.groovy");
		return null;
	}
	
	orderHeader = EntityUtil.getFirst(orderList);
	
	orderId = (String) orderHeader.get("orderId");
	
	Debug.logWarning("Scalapay order found  ["+orderId+"].", "GoogleTagMgrOrderData.groovy");
	
	return orderId;
}