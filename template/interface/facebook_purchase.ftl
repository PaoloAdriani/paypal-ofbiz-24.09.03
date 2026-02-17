<script>

function fbPurchase(){

	var total =  parseFloat('${transactionTotal?default(0.00)}').toFixed(2);
        
    fbq('track', 'Purchase', {value: total, currency: 'EUR'});

}
      
</script>