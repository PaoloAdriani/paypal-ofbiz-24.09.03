/*
 *  Groovy script that checks for paypal specific errors in the request (coming from API REST calls)
 * Attributes: _PAYPAL_ISSUE_NAME_, _PAYPAL_ISSUE_MESSAGE_, _PAYPAL_ISSUE_DETAIL_
 */

paypalIssueName = request.getAttribute("_PAYPAL_ISSUE_NAME_")
paypalIssueMessage = request.getAttribute("_PAYPAL_ISSUE_MESSAGE_")
paypalIssueDetail = request.getAttribute("_PAYPAL_ISSUE_DETAIL_")

println "********** paypalIssueName => " + paypalIssueName
println "********* paypalIssueMessage => " + paypalIssueMessage
println "********* paypalIssueDetail => " + paypalIssueDetail

context.paypalIssueName = paypalIssueName
context.paypalIssueMessage = paypalIssueMessage
context.paypalIssueDetail = paypalIssueDetail
context.returnUrl = "index"