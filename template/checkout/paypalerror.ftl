<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>${uiLabelMap.PaypalCheckoutErrorTitle!}</title>
    <#assign asset = "/images/ch_asset" />
    <link rel="icon" href="<@ofbizContentUrl>${asset}/img/cropped-favicon-180x180.png</@ofbizContentUrl>">
    <link href="https://fonts.googleapis.com/css?family=Montserrat:400,500,700" rel="stylesheet">
	<link href="https://fonts.googleapis.com/css?family=Poppins:300,400&display=swap" rel="stylesheet">
	<link href="https://fonts.googleapis.com/css?family=DM+Serif+Display&display=swap" rel="stylesheet">
	<link href="https://fonts.googleapis.com/css?family=Cardo&display=swap" rel="stylesheet"> 
	<link href="<@ofbizContentUrl>${asset}/css/vendor/bootstrap.min.css</@ofbizContentUrl>" rel="stylesheet">
	
  </head>
  <body>
    <main>
    	<div class="container-fluid page-container">
			<div class="row mt-2">
				<div class="col-lg-8 col-lg-offset-2 col-xs-12 text-center">
					<div class="alert alert-danger">
						<h1>${uiLabelMap.PaypalCheckoutErrorTitle}</h1>
						<p><b>${uiLabelMap.PaypalIssueName}</b>: ${paypalIssueName!}</p>
						<p><b>${uiLabelMap.PaypalIssueMessage}</b>: ${paypalIssueMessage!}</p>
						<p><b>${uiLabelMap.PaypalIssueDetail}</b>: ${paypalIssueDetail!}</p>
					</div>
					<a class="btn btn-dark" href="<@ofbizUrl>${returnUrl!}</@ofbizUrl>">${uiLabelMap.BackToSite}</a>
				</div>
			</div>  	
		</div>         
    </main>
	
  </body>
</html>