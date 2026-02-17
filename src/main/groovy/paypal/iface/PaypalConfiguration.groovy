import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.util.EntityUtilProperties
import org.apache.ofbiz.order.order.OrderReadHelper
import paypal.PaypalClientHelper
import paypal.PaypalClient

orderId = request.getAttribute("orderId")

if (orderId) {
    orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false)
    orh = new OrderReadHelper(orderHeader)
    currency = orh.getCurrency()
    orderTotal = orh.getOrderGrandTotal()
    context.orderCurrency = currency
    context.orderTotal = orderTotal
}

paypalClientId = PaypalClientHelper.getPaypalClientId(delegator)
//merchant specific asset location
merchantAssetLocation = EntityUtilProperties.getPropertyValue("paypal", "merchantAssetLocation", delegator)
//paypal asset location : to style interface
paypalAssetLocation = EntityUtilProperties.getPropertyValue("paypal", "paypalAssetLocation", delegator)
//merchant PayPal API fetch urls
merchantWebapp = EntityUtilProperties.getPropertyValue("paypal", "merchantWebapp", delegator)
fetchCreatePaypalOrderSDKUrl = EntityUtilProperties.getPropertyValue("paypal", "fetch.createPaypalOrderSDKUrl", delegator)
fetchCreatePaypalOrder3DSHostedSDKUrl = EntityUtilProperties.getPropertyValue("paypal", "fetch.createPaypalOrder3DSHostedSDKUrl", delegator)
fetchCapturePaypalOrderSDKUrl = EntityUtilProperties.getPropertyValue("paypal", "fetch.capturePaypalOrderSDKUrl", delegator)
fetchCancelPaypalOrderSDKUrl = EntityUtilProperties.getPropertyValue("paypal", "fetch.cancelPaypalOrderSDKUrl", delegator)
//merchant logo image
merchantLogoLocation = EntityUtilProperties.getPropertyValue("paypal", "merchantLogoLocation", delegator)

clientToken = PaypalClient.getClientToken(delegator)

context.paypalClientId = paypalClientId
context.clientToken = clientToken

context.assetLocation = merchantAssetLocation
context.paypalAssetLocation = paypalAssetLocation
context.logoLocation = merchantLogoLocation
context.merchantWebappName = merchantWebapp
context.fetchCreatePaypalOrderSDKUrl = fetchCreatePaypalOrderSDKUrl
context.fetchCreatePaypalOrder3DSHostedSDKUrl = fetchCreatePaypalOrder3DSHostedSDKUrl
context.fetchCapturePaypalOrderSDKUrl = fetchCapturePaypalOrderSDKUrl
context.fetchCancelPaypalOrderSDKUrl = fetchCancelPaypalOrderSDKUrl