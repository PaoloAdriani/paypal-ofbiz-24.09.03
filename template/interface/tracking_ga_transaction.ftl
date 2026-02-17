<script>

window.dataLayer = window.dataLayer || [];


function trackingGaPurchase(){

	var total =  parseFloat('${transactionTotal?default(0.00)}').toFixed(2);

	<#if (orderItems?? && orderItems?size > 0) >
		
		let gaObject = <@buildDataLayer orderItems=orderItems orderId='${orderId?if_exists}' storeName='${storeName}' transactionTotal='${transactionTotal}' shippingCosts='${shippingCosts}'/>;
	
		window.dataLayer.push(gaObject);
	
	</#if>
}
		
  		
<#macro buildDataLayer orderItems orderId storeName transactionTotal shippingCosts>

			{
			  'event': 'purchasePaypal',
			  'ecommerce': {
			  
			    'currencyCode': '${currencyUomId}',
			    'purchase': {
			    
			      	'actionField': {
			        'id': '${orderId?if_exists}',
			        'affiliation': '${brandName}',
			        'revenue': total,
			        'tax':'${totalTaxValue}',
			        'shipping': '${shippingCosts}',
			        'coupon': ''
                 },
			      
			     'products': [
 
						<#list orderItems as item>
		
							<#assign varProductId = item.productId />
						
							<#if varProductId?contains(".")>
								<#assign virProductId = varProductId?keep_before(".") />
							</#if>
		
							<#assign delegator = request.getAttribute("delegator") />
						
							<#assign productGV = delegator.findOne("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId", virProductId), false) />
							
							<#assign prodPrimaryCat = productGV.primaryProductCategoryId?if_exists />
						
							<#if prodPrimaryCat?? && prodPrimaryCat?has_content>     
								
								<#assign categoryGV = delegator.findOne("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId", prodPrimaryCat), false) />
							
								<#assign categoryContentWrapper = Static["org.ofbiz.product.category.CategoryContentWrapper"].makeCategoryContentWrapper(categoryGV, request) />
							
								<#assign categoryName = categoryContentWrapper.get("CATEGORY_NAME")?if_exists />
						
							<#else>
       							 
       							 <#assign categoryName = "_NA_" />
    						
    						</#if>
                            
                            {                            
					        	'name': '${item.itemDescription}',
					        	'id': '${varProductId}',
					        	'price': parseFloat('${item.unitPrice}').toFixed(2),
					        	'brand': 'Liviana Conti',
					        	'category': '${categoryName}',
					        	'variant': '',
					        	'quantity': ${item.quantity},
					        	'coupon': ''
					       }
						
							<#if item_has_next>,</#if>


                     </#list>
					]
				}
        	}
		}
		
</#macro>

</script>

