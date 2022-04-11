async function initCheckout(){
    try{
        const clientKey = document.getElementById('clientKey').innerHtml;
        const paymentMethodsResponse = await callServer('/api/getPaymentMethods');
        const config ={
            paymentMethodsResponse,
            clientKey,
            locale: 'en_US',
            environment: 'test',
            showPayButton: true,
            paymentMethodsConfiguration:{
                idea: {
                    showImage: true,
                },
                card:{
                    hasHolderName: true,
                    holderNameRequire: true,
                    name: 'Credit or debit card',
                    amount:{
                        value: 1000,
                        currency: 'Eur',
                    },

                }
            },
            onSubmit:(state, component) => {
                if(state.isValid){
                     handleSubmission(state, component, '/api/initiatePayment');
                }

            },
            onAdditionalDetails:(state, component) => {
                handleSubmission(state, component, '/api/submitAdditionalDetails');

            },
        };

        const  checkout = new AdyenCheckout(config);
        checkout.create("dropin").mount(document.getElementById("dropin"))

    } catch (error){
        console.error(error);
        alert("Error Occured. Look at the console for details");
    }
}

initCheckout();

// Calls your server endpoints
async function callServer(url, data) {
    const res = await fetch(url, {
        method: "POST",
        body: data ? JSON.stringify(data) : "",
        headers: {
            "Content-Type": "application/json",
        },
    });

    return await res.json();
}
// handler

async function handleSubmission(state, component, url){
    try{
        const res = await  callServer(url, state.data);
        handleServerResponse(res, component);
    } catch (error){
        console.error(error);
        alert('Error occured. Look at console for details');
    }
}


function handleServerResponse(res, component){
    if (res.action){
        component.handleAction(res.action);
    } else{
        switch (res.resultCode){
            case 'Authorised':
            window.location.href= '/result/success';
            break;
            case 'Pending':
            case 'Receive':
                window.location.href ='/result/pending';
                break;
            case 'Refuse':
                window.location.href = '/result/failed';
                break;
            default:
                window.location.href = '/result/error';
                break;

        }
    }
}




