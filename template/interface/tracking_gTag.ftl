<script>

function trackingGTag(){

	var total =  parseFloat('${transactionTotal?default(0.00)}').toFixed(2);
	
	gtag('event', '${eventNameGtag}', {
	      'send_to': '${sendToGtag}',
	      'value': total,
	      'currency': '${currencyUomId}',
	      'transaction_id': '${orderId?if_exists}'
	  });


}
		
</script>