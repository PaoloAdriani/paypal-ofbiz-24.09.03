<!DOCTYPE html>
<html dir="ltr" lang="en-US">

    <head>
        <meta http-equiv="content-type" content="text/html; charset=utf-8" />
        <meta name="author" content="MpStyle" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        
        <link href="<@ofbizContentUrl>${assetLocation!}/css/vendor/bootstrap.min.css</@ofbizContentUrl>" rel="stylesheet">
        <#-- Paypal assets -->
        <link href="<@ofbizContentUrl>${paypalAssetLocation!}/css/cardfields.css</@ofbizContentUrl>" rel="stylesheet">
        <link href="<@ofbizContentUrl>${paypalAssetLocation!}/css/interface.css</@ofbizContentUrl>" rel="stylesheet">

		<#if gtmContainerId?? && gtmContainerId?has_content>
        	
        	<#include "tag_manager.ftl">
		
		</#if>
		
		<#if magnewsApiKey?? && magnewsApiKey?has_content>
        	
        	<#include "magnews.ftl">
		
		</#if>
		
		
        <!-- Document Title
        ============================================= -->
        <title>${uiLabelMap.InterfaceMetaTitle!} | PayPal Interface Page</title>
 
    </head>


    <body class="stretched">
        
        <#assign orderId = request.getAttribute("orderId") />

		<#if gtmContainerId?? && gtmContainerId?has_content>
			        
        	<#include "tag_manager_noscript.ftl">        
              
            <#if sendToGtag?? && sendToGtag?has_content>                    
                                                    
            	<#include "tracking_gTag.ftl">                                                        
                                        
            <#else>                                                                                                                                                    
        		
        		<#include "tracking_ga_transaction.ftl">
        	
        	</#if>		
        
        </#if>
        
        <#if magnewsApiKey?? && magnewsApiKey?has_content>
        	
        	<#include "tracking_magnews_transaction.ftl">
		
		</#if>
		
		 <#if fbPixelId?? && fbPixelId?has_content>
        	
        	<#include "facebook_purchase.ftl">
		
		</#if>	
						
        			

        <div class="center tridimensional-effect rounded-corners">
            <div class="row" style="margin-bottom:50px; margin-top:10px;">
            	<div class="col-lg-6 col-md-6 col-xs-12">
                    <img class="img-responsive" src="<@ofbizContentUrl>${paypalAssetLocation!}/img/paypal_PNG2.png</@ofbizContentUrl>" alt="paypal-logo" style="width:200px;">
            	</div>
            	<div class="col-lg-6 col-md-6 col-xs-12">
                    <img class="img-responsive" src="<@ofbizContentUrl>${logoLocation!}</@ofbizContentUrl>" alt="merchant-logo" style="width:200px;">
            	</div>
            </div>
            
            <h4 style="margin-bottom:50px; text-align:center; font-weight:bold;" id="paypal-title">${uiLabelMap.InterfaceTitleClickToPay}</h4>
            <#-- Set up a container element for the button -->
            <div id="paypal-button-container" class="paypal-button-container"></div>
            <#if orderTotal?contains(",")>
            	<#assign orderTotal = orderTotal?replace(",", ".") />
            </#if>
            <#-- PayPal Messages -->
            <div id="paypal-message-container" 
            	 data-pp-message 
            	 data-pp-style-layout="text"
            	 data-pp-style-text-size="14"
            	 data-pp-style-text-align="center"
            	 data-pp-amount="${orderTotal!}"></div>
            	 
            <#-- Card container -->
            <div class="card_container" id="card-container">
            <hr class="hr-text" data-content="${uiLabelMap.PaypalOr}">
                <form id="card-form">
                        <p><small><b>${uiLabelMap.MandatoryFields}</b></small></p>
                <label for="card-number">${uiLabelMap.CardNumber}*</label><div id="card-number" class="card_field"></div>
                        <div>
                        <label for="expiration-date">${uiLabelMap.ExpirationDate}*</label>
                        <div id="expiration-date" class="card_field"></div>
                    </div>
                    <div>
                        <label for="cvv">CVV*</label><div id="cvv" class="card_field"></div>
                    </div>
                        <label for="card-holder-name">${uiLabelMap.NameOnCard}*</label>
                        <input type="text" id="card-holder-name" name="card-holder-name" autocomplete="off" placeholder="${uiLabelMap.NameOnCard}"/>
                        <small id ="cardHolderNameMissingField" class="text-danger" style="display: none"><b>${uiLabelMap.MandatoryField}</b></small>
                    <br/><br/>
                        <button value="submit" id="submit" class="btn text-center">${uiLabelMap.Pay}</button>
                </form>
            </div>
            <#-- Custom messages containers -->
            <div class="alert alert-danger" id="paypal-error-container" style="display: none"></div>
            <div class="alert alert-success" id="paypal-success-container" style="display: none"></div>
            <#-- Other messages  -->
            <div id="processing-message" class="text-center" style="display: none;"><h5 class="blink"><b>${uiLabelMap.ProcessingMessage}...</b></h5></div>
        </div>
        

       	<#-- Download PayPal SDK Javascript if PayPal account ClientID is provided -->
        <#if paypalClientId?? && paypalClientId?has_content>			
		
            <script src="https://www.paypal.com/sdk/js?components=buttons,hosted-fields,messages&enable-funding=paylater&client-id=${paypalClientId!}&currency=${orderCurrency!'EUR'}&debug=true" data-client-token="${clientToken!}"></script>
			
                <script>
                    setErrorMessage("");
                    setSuccessMessage("");

                    //Hide error message for card holder field
                    let cardHolderField = document.getElementById("cardHolderNameMissingField");
                    cardHolderField.addEventListener("keyup", function() { cardHolderField.style.display = "none"; });

                    // Render the PayPal button into #paypal-button-container
                    paypal.Buttons({
                        style: {
                            layout: 'horizontal'
                        },

                        // Call your server to set up the transaction
                        createOrder: function(data, actions) {
                            return fetch('/${merchantWebappName!}/control/${fetchCreatePaypalOrderSDKUrl!}/', {
                                method: 'post',
                                headers: {

                                    'Content-Type': 'application/json',
                                    'Accept': 'application/json',
                                },
                                body: JSON.stringify({
                                    orderId: "${orderId}"
                                })
                            }).then(function(res) {
                                console.log("createOrder res: " + JSON.stringify(res));
                                return res.json();
                            }).then(function(orderData) {
                                /* orderData json structure is specific for this server-side method. */
                                console.log("createOrder orderData: " + JSON.stringify(orderData));
                                let err = orderData.error;
                                if (err !== null && (typeof err !== 'undefined') && (err === "true")) {
                                    let err_code = orderData.HTTP_RES_CODE;
                                    let err_msg = orderData.HTTP_RES_MSG;
                                    let detail_msg = orderData.DETAIL_MSG;
                                    setErrorMessage("<p>${uiLabelMap.InterfacePayPalError} => " + err_code + " " + err_msg + " : " + detail_msg + "</p>");
                                    showErrorMessageContainer();
                                    hidePayPalMessageContainer();
                                    return;
                                } 
                                return orderData.id;
                            }).catch((error) => {
                                console.log("paypalCreateOrder catch error => " + error);
                                hidePayPalMessageContainer();
                                setButtonContainerContent('');
                                setCardContainerContent('');
                                setContainerTitle('');
                                setContainerTitle('${uiLabelMap.InterfaceTitleError}');
                                setButtonContainerContent('<div class="row" style="text-align:center"><a class="btn btn-sm btn-danger" href="<@ofbizUrl>index</@ofbizUrl>" style="margin: 0 auto;">${uiLabelMap.BackToSite}</a></div>');
                                setErrorMessage("<p>${uiLabelMap.InterfacePayPalError} => " + error + "</p>");
                                showErrorMessageContainer();
                                showButtonContainer();
                                hideProcessingMessageContainer();
                                return;
                            });
                        },

                        // Call your server to finalize the transaction
                        onApprove: function(data, actions) {
                            console.log("PayPal order return data: " + JSON.stringify(data));
                            return fetch('/${merchantWebappName!}/control/${fetchCapturePaypalOrderSDKUrl!}/', {
                                method: 'post',
                                headers: {

                                    'Content-Type': 'application/json',
                                    'Accept': 'application/json',
                                },
                                body: JSON.stringify({
                                    paypalOrderId: data.orderID,
                                    orderId: "${orderId}"
                                })
                            }).then(function(res) {
                                    console.log("capturePayment res: " + res);
                                return res.json();
                            }).then(function(orderData) {
                                /* Three cases to handle:
                                 *   (1) Recoverable INSTRUMENT_DECLINED -> call actions.restart()
                                 *   (2) Other non-recoverable errors -> Show a failure message
                                 *   (3) Successful transaction -> Show confirmation or thank you
                                 * orderData is v2/checkout/orders capture response, propagated from the server
                                 * You could use a different API or structure for your 'orderData'
                                 */
                                var errorDetail = Array.isArray(orderData.details) && orderData.details[0];

                                if (errorDetail && errorDetail.issue === 'INSTRUMENT_DECLINED') {
                                    return actions.restart(); // Recoverable state, per:
                                    // https://developer.paypal.com/docs/checkout/integration-features/funding-failure/
                                }

                                if (errorDetail) {
                                    let msg = '${uiLabelMap.InterfacePayPalTransactionError}.';
                                    if (errorDetail.description) msg += '\n\n' + errorDetail.description;
                                    if (orderData.debug_id) msg += ' (' + orderData.debug_id + ')';
                                    setErrorMessage("<p>${uiLabelMap.InterfacePayPalError} => " + msg + "</p>");
                                    showErrorMessageContainer();
                                    hidePayPalMessageContainer();
                                    return; 
                                }

                                let err = orderData.error;
                                if (err !== null && (typeof err !== 'undefined') && (err === "true")) {
                                    let err_msg = orderData.ERROR_MSG;
                                    setErrorMessage("<p>${uiLabelMap.InterfacePayPalError} => " + err_msg + "</p>");
                                    showErrorMessageContainer();
                                    hidePayPalMessageContainer();
                                    return;
                                }

                                // Successful capture!
                                console.log('Capture result', orderData, JSON.stringify(orderData, null, 2));
                                let transaction = orderData.purchase_units[0].payments.captures[0];
                                let successMsg = "${uiLabelMap.InterfacePayPalTransactionCompleted} : " + transaction.id + "/" + transaction.status + "/" + transaction.custom_id;
                                setSuccessMessage("<p>${uiLabelMap.InterfacePayPalSuccess}! " + successMsg + "  </p>");
                                showSuccessMessageContainer();
                                hidePayPalMessageContainer();

								// push dataLayer
								<#if gtmContainerId?? && gtmContainerId?has_content>
									
									 <#if sendToGtag?? && sendToGtag?has_content>    
										
										trackingGTag();
									
									<#else>	
										
										trackingGaPurchase();
									
									</#if>	
								
								</#if>	
								
								// tracking MagNews
								<#if magnewsApiKey?? && magnewsApiKey?has_content>
									trackingMagNews()
								</#if>
								
								// facebook purchase event
								<#if fbPixelId?? && fbPixelId?has_content>
									fbPurchase()
								</#if>

                                //Empty the button and card container
                                setButtonContainerContent('');
                                setCardContainerContent('');
                                setContainerTitle('');
                                setContainerTitle('${uiLabelMap.InterfaceTitleThankyouForYourOrder}');
                                setButtonContainerContent('<div class="row" style="display:flex; flex-direction:column;text-align:center"><p>${uiLabelMap.InterfaceMessageEmailConfirmOrder}</p><a class="btn btn-sm btn-success" href="<@ofbizUrl>index</@ofbizUrl>" style="margin: 0 auto;">${uiLabelMap.BackToSite}</a></div>');
                                showButtonContainer();
                                hideProcessingMessageContainer();

                            });
                        },
                        //Transaction/Order cancellation event
                        onCancel: function(data) {
                            console.log("Button On Cancel event data: " + JSON.stringify(data));
                            return fetch('/${merchantWebappName!}/control/${fetchCancelPaypalOrderSDKUrl!}/', {
                                    method: 'post',
                                headers: {
                                    'Content-Type': 'application/json',
                                    'Accept': 'application/json',
                                },
                                body: JSON.stringify({
                                    paypalOrderId: data.orderID,
                                    orderId: "${orderId}"
                                })
                            })
                            .then(function(res) {
                                console.log("Cancel order res: " + JSON.stringify(res));
                            }).then(function(orderData) {
                                /* Redirect to index */
                                window.location.href = "<@ofbizUrl>index</@ofbizUrl>";
                            });
                        },
                        //Generic error handler
                        onError: function(data, actions) {
                            console.log("on error: " + JSON.stringify(data));
                            let errHtml = document.getElementById("paypal-error-container").innerHTML;

                            setButtonContainerContent('');
                            setCardContainerContent('');
                            setContainerTitle('');
                            setContainerTitle('${uiLabelMap.InterfaceTitleError}');
                            setButtonContainerContent('<div class="row" style="text-align:center"><a class="btn btn-sm btn-danger" href="<@ofbizUrl>index</@ofbizUrl>" style="margin: 0 auto;">${uiLabelMap.BackToSite}</a></div>');
                            let baseErrMsg = '<p><b>${uiLabelMap.InterfacePayPalErrorContactForHelp}.</b></p>';
                            setErrorMessage(baseErrMsg + errHtml + "<p>${uiLabelMap.InterfacePayPalError} => " + data + "</p>");
                            showErrorMessageContainer();
                            hidePayPalMessageContainer();
                        }

                    }).render('#paypal-button-container');

                    // Check eligibility for advanced credit and debit card payments
                if (paypal.HostedFields.isEligible()) {
                    // Renders card fields
                    paypal.HostedFields.render({

                        createOrder: () => {

                            let cardHolderName = document.getElementById("card-holder-name").value;
                            
                            if (cardHolderName == "") {
                                document.getElementById("cardHolderNameMissingField").style.display = 'block';
                                return;
                            }

                            return fetch('/${merchantWebappName!}/control/${fetchCreatePaypalOrder3DSHostedSDKUrl!}/', {
                                method: 'post',
                                headers: {
                                    'Content-Type': 'application/json',
                                    'Accept': 'application/json'
                                },
                                body: JSON.stringify({
                                    orderId: "${orderId}",
                                    //Cardholder's first and last name
                                    cardHolderName: cardHolderName,
                                    //Additional parameter
                                    paymentMethod: "hosted-fields"
                                })
                            }).then(function(res) {
                                return res.json();
                            }).then(function(orderData) {
                                orderId = orderData.id;
                                return orderId;
                            });
                        },

                        styles: {
                            ".valid": {
                              color: "green",
                            },
                            ".invalid": {
                              color: "red",
                            },
                          },

                        fields: {

                            number: {
                                selector: '#card-number',
                                placeholder: '${uiLabelMap.CardNumber!}'
                            },
                            cvv: {
                                selector: '#cvv',
                                placeholder: 'CVV',
                            },
                            expirationDate: {
                                selector: '#expiration-date',
                                placeholder: 'MM/YYYY'
                            }
                        },
                        onCancel: function(data) {
                            console.log("On Cancel event data: " + JSON.stringify(data));
                            return fetch('/${merchantWebappName!}/control/${fetchCancelPaypalOrderSDKUrl!}/', {
                                    method: 'post',
                                headers: {
                                    'Content-Type': 'application/json',
                                    'Accept': 'application/json',
                                },
                                body: JSON.stringify({
                                    paypalOrderId: data.orderID,
                                    orderId: "${orderId}"
                                })
                            }).then(function(res) {
                                    console.log("Cancel order res: " + res);
                            }).then(function(orderData) {
                                /* Redirect to index */
                                window.location.href = "<@ofbizUrl>index</@ofbizUrl>";
                            }).catch((error) => {
                              console.log("onCancel catch error => " + error);  
                            });
                        },
                        //Generic error handler
                        onError: function(data) {
                            alert("On Error => " + JSON.stringify(data));
                            console.log("On Error => " + JSON.stringify(data));
                        }

                    }).then(function (hf) {

                        //card form submit with 3DS 
                        document.querySelector('#card-form').addEventListener('submit', (event) => {

                            event.preventDefault();
                            
                            let cardHolderName = document.getElementById("card-holder-name").value;
                            
                            if (cardHolderName == "") {
                                document.getElementById("cardHolderNameMissingField").style.display = 'block';
                                return;
                            }

                            hideButtonContainer();
                            hideCardContainer();
                            hidePayPalMessageContainer();
                            setContainerTitle('');
                            showProcessingMessageContainer();

                            hf.submit({
                                //Cardholder's first and last name
                                cardholderName: document.getElementById("card-holder-name").value,

                                // Trigger 3D Secure authentication
                                contingencies: ['SCA_ALWAYS']

                                }).then(function (payload) {
                                    //console.log("### Payload Liability Shift: " + payload.liabilityShift);

                                    /* 3DS verification handled in CAPTURE server event*/
                                return fetch('/${merchantWebappName!}/control/${fetchCapturePaypalOrderSDKUrl!}/', {
                                    method: 'post',
                                    headers: {

                                        'Content-Type': 'application/json',
                                        'Accept': 'application/json',
                                    },
                                    body: JSON.stringify({
                                        paypalOrderId: payload.orderId,
                                                orderId: "${orderId}",
                                                check3ds: "Y"
                                    })
                                }).then(function(res) {
                                    let jsonres = res.json();
                                    console.log(jsonres);
                                    return jsonres;
                                }).then(function(orderData) {

                                    console.log("createOrder orderData: " + JSON.stringify(orderData));
                                    let err = orderData.error;
                                    if (err !== null && (typeof err !== 'undefined') && (err === "true")) {
                                        let err_msg = orderData.ERROR_MSG;

                                        setButtonContainerContent('');
                                        setCardContainerContent('');
                                        setContainerTitle('');
                                        setContainerTitle('${uiLabelMap.InterfaceTitleError}');

                                        let baseErrMsg = '<p><b>${uiLabelMap.InterfacePayPalErrorContactForHelp}.</b></p>';
                                        setErrorMessage("<p>${uiLabelMap.InterfacePayPalError} => " + err_msg + "</p>");
                                        showErrorMessageContainer();
                                        hidePayPalMessageContainer();
                                        setButtonContainerContent('<div class="row" style="text-align:center"><a class="btn btn-sm btn-danger" href="<@ofbizUrl>index</@ofbizUrl>" style="margin: 0 auto;">${uiLabelMap.BackToSite}</a></div>');
                                        showButtonContainer();
                                        hideProcessingMessageContainer();

                                        return;
                                    } else {
                                        /* Three cases to handle:
                                         *   (1) Recoverable INSTRUMENT_DECLINED -> call actions.restart()
                                         *   (2) Other non-recoverable errors -> Show a failure message
                                         *   (3) Successful transaction -> Show confirmation or thank you
                                         * orderData reads a v2/checkout/orders capture response, propagated from the server
                                         * You could use a different API or structure for your 'orderData'
                                         */
                                        var errorDetail = Array.isArray(orderData.details) && orderData.details[0];

                                        if (errorDetail && errorDetail.issue === 'INSTRUMENT_DECLINED') {
                                            return actions.restart(); // Recoverable state, per:
                                            // https://developer.paypal.com/docs/checkout/integration-features/funding-failure/
                                        }

                                        if (errorDetail) {
                                            let msg = '${uiLabelMap.InterfacePayPalTransactionError}.';
                                            if (errorDetail.description) msg += '\n\n' + errorDetail.description;
                                            if (orderData.debug_id) msg += ' (' + orderData.debug_id + ')';
                                            setErrorMessage("<p>${uiLabelMap.InterfacePayPalError} => " + msg + "</p>");
                                            showErrorMessageContainer();
                                            hidePayPalMessageContainer();
                                            return;
                                        }

                                        let transaction = orderData.purchase_units[0].payments.captures[0];
                                        let successMsg = "${uiLabelMap.InterfacePayPalTransactionCompleted} : " + transaction.id + "/" + transaction.status + "/" + transaction.custom_id;
                                        setSuccessMessage(successMsg);
                                        hidePayPalMessageContainer();
                                        
                                        // push dataLayer
										<#if gtmContainerId?? && gtmContainerId?has_content>
											 
											 <#if sendToGtag?? && sendToGtag?has_content>    
										
												trackingGTag();
									
											<#else>	
										
												trackingGaPurchase();
									
											</#if>	
										
										</#if>	
										
										// tracking MagNews
										<#if magnewsApiKey?? && magnewsApiKey?has_content>
											trackingMagNews()
										</#if>
										
										// facebook purchase event
										<#if fbPixelId?? && fbPixelId?has_content>
											fbPurchase()
										</#if>
                                        
                                        setButtonContainerContent('');
                                        setCardContainerContent('');
                                        setContainerTitle('');
                                        setContainerTitle('${uiLabelMap.InterfaceTitleThankyouForYourOrder}');
                                        setButtonContainerContent('<div class="row" style="display;flex; flex-direction:column;text-align:center"><p>${uiLabelMap.InterfaceMessageEmailConfirmOrder}</p><a class="btn btn-sm btn-success" href="<@ofbizUrl>index</@ofbizUrl>" style="margin: 0 auto;">${uiLabelMap.BackToSite}</a></div>');
                                        showButtonContainer();
                                        showSuccessMessageContainer();
                                        hideProcessingMessageContainer();

                                    }
                                });
                            }).catch((error) => {
                                console.log("hosted-field submit catch => " + error);
                                hidePayPalMessageContainer();
                                setButtonContainerContent('');
                                setCardContainerContent('');
                                setContainerTitle('');
                                setContainerTitle('${uiLabelMap.InterfaceTitleError}');
                                setButtonContainerContent('<div class="row" style="text-align:center"><a class="btn btn-sm btn-danger" href="<@ofbizUrl>index</@ofbizUrl>" style="margin: 0 auto;">${uiLabelMap.BackToSite}</a></div>');
                                let baseErrMsg = '<p><b>${uiLabelMap.InterfacePayPalErrorContactForHelp}.</b></p>';
                                setErrorMessage("<p>${uiLabelMap.InterfacePayPalError} => " + error + "</p>");
                                showErrorMessageContainer();
                                showButtonContainer();
                                hideProcessingMessageContainer();
                            });

                        });
                    });

                } else {
                    /*
                     * Handle experience when advanced credit and debit card payments
                     * card fields are not eligible
                     */
                     // Check eligibility for advanced credit anure to enable the account to receive the payments by cards");
                    console.log("Please, be sure to enable the account to receive the payments by cards");
                    hideCardContainer();
                }

                /* ########## Utility functions ########## */
                function showButtonContainer() {
                    document.getElementById('paypal-button-container').style.display = "block";
                }
                function hideButtonContainer() {
                    document.getElementById('paypal-button-container').style.display = "none";
                }
                function showCardContainer() {
                    document.getElementById("card-container").style.display = "block";
                }
                function hideCardContainer() {
                    document.getElementById("card-container").style.display = "none";
                }
                function showErrorMessageContainer() {
                    document.getElementById("paypal-error-container").style.display = "block";
                }
                function hideErrorMessageContainer() {
                    document.getElementById("paypal-error-container").style.display = "none";
                }
                function showSuccessMessageContainer() {
                    document.getElementById("paypal-success-container").style.display = "block";
                }
                function hideSuccessMessageContainer() {
                    document.getElementById("paypal-success-container").style.display = "none";
                }
                function showProcessingMessageContainer() {
                    document.getElementById('processing-message').style.display = "block";
                }
                function hideProcessingMessageContainer() {
                    document.getElementById('processing-message').style.display = "none";
                }
                function showPayPalMessageContainer() {
                    document.getElementById("paypal-message-container").style.display = "block";
                }
                function hidePayPalMessageContainer() {
                    document.getElementById("paypal-message-container").style.display = "none";
                }

                function setSuccessMessage(msg) {
                    document.getElementById("paypal-success-container").innerHTML = msg;
                }
                function setErrorMessage(msg) {
                    document.getElementById("paypal-error-container").innerHTML = msg;
                }

                function setButtonContainerContent(cnt) {
                    document.getElementById('paypal-button-container').innerHTML = cnt;
                }
                function setCardContainerContent(cnt) {
                    document.getElementById('card-container').innerHTML = cnt;
                }
                function setContainerTitle(title) {
                    document.getElementById('paypal-title').innerHTML = title;
                }

            </script>
        </#if>
    </body>
</html>