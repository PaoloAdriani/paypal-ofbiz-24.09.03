<script>

function trackingMagNews() {
	
	var total =  parseFloat('${transactionTotal?default(0.00)}').toFixed(2);

	mna('conversion', {'goal': '${goalBrand}', value: total});	

}

</script>