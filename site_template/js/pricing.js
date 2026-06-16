/* Cellfire Pricing Page JS */
var cfBilling = 'monthly';

function setBilling(t) {
    cfBilling = t;
    var btns = document.querySelectorAll('#ptoggle .ptbtn');
    var types = ['monthly', 'annual', 'lifetime'];
    btns.forEach(function(b, i) {
        b.className = 'ptbtn ' + (types[i] === t ? 'ptbtn-active' : 'ptbtn-inactive');
    });
    document.querySelectorAll('.pval').forEach(function(el) {
        var v = el.getAttribute('data-' + t);
        if (t === 'lifetime' && v === '0') {
            el.textContent = 'Contact';
            var sup = el.closest('.plan-price') && el.closest('.plan-price').querySelector('sup');
            if (sup) sup.style.display = 'none';
        } else {
            el.textContent = parseFloat(v).toFixed(2);
            var sup = el.closest('.plan-price') && el.closest('.plan-price').querySelector('sup');
            if (sup) sup.style.display = '';
        }
    });
    document.querySelectorAll('.pperiod').forEach(function(el) {
        if (t === 'monthly') el.textContent = 'per month';
        else if (t === 'annual') el.textContent = 'per month, billed annually';
        else el.textContent = 'one-time payment';
    });
}

async function cfSubscribe(plan) {
    try {
        var r = await fetch('/index.php?option=com_cellfireapi&task=stripe.createCheckout&format=json', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({plan_type: plan, billing: cfBilling})
        });
        var d = await r.json();
        if (d.data && d.data.url) window.location.href = d.data.url;
        else window.location.href = '/account';
    } catch(e) {
        window.location.href = '/account';
    }
}