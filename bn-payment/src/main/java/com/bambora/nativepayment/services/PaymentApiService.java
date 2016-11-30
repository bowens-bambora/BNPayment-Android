/*
 * Copyright (c) 2016 Bambora ( http://bambora.com/ )
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.bambora.nativepayment.services;

import com.bambora.nativepayment.handlers.BNPaymentHandler;
import com.bambora.nativepayment.interfaces.ICardRegistrationCallback;
import com.bambora.nativepayment.interfaces.ITransactionListener;
import com.bambora.nativepayment.logging.BNLog;
import com.bambora.nativepayment.models.PaymentSettings;
import com.bambora.nativepayment.models.TransactionResponse;
import com.bambora.nativepayment.models.creditcard.CreditCard;
import com.bambora.nativepayment.models.creditcard.RegistrationFormError;
import com.bambora.nativepayment.models.creditcard.RegistrationFormSettings;
import com.bambora.nativepayment.models.creditcard.RegistrationParams;
import com.bambora.nativepayment.models.creditcard.RegistrationResponse;
import com.bambora.nativepayment.network.ApiService;
import com.bambora.nativepayment.network.Callback;
import com.bambora.nativepayment.network.Request;
import com.bambora.nativepayment.network.RequestError;
import com.bambora.nativepayment.network.Response;

import static com.bambora.nativepayment.network.RequestMethod.POST;

/**
 * An {@link ApiService} class for setting up credit card related API endpoints/requests.
 */
public class PaymentApiService extends ApiService {

    /**
     * Creates a {@link Request} for initiating a hosted payment page to be used for registering
     * a credit card for future payments.
     *
     * @param formSettings  {@link RegistrationFormSettings} for customizing the form
     * @return A URL to the initiated page
     */
    public Request<RegistrationResponse> initiateHpp(RegistrationFormSettings formSettings) {
        return new Request<>(this, RegistrationResponse.class)
                .endpoint("hpp/")
                .method(POST)
                .body(formSettings);
    }

    public Request<CreditCard> authorizeCreditCard(RegistrationParams registrationParams) {
        return new Request<>(this, CreditCard.class)
                .endpoint("")//.endpoint("cardregistration/")
                .method(POST)
                .body(registrationParams);
    }

    /**
     * Creates a {@link Request} for making a transaction with given settings.
     *
     * @param paymentId         A unique identifier for the payment
     * @param paymentSettings   {@link PaymentSettings}
     * @return A {@link Request} to be executed
     */
    public Request<TransactionResponse> makeTransaction(String paymentId, PaymentSettings paymentSettings) {
        return new Request<>(this, TransactionResponse.class)
                .endpoint("payments/{paymentId}/card_token/")
                .endpointParameter("paymentId", paymentId)
                .method(POST)
                .body(paymentSettings);
    }

    private static PaymentApiService createService() {
        return BNPaymentHandler.getInstance().createService(PaymentApiService.class);
    }

    public interface IHppResultListener {
        void onHppInitiated(String url);
        void onError(RegistrationFormError registrationFormError);
    }

    /**
     * Helper class for executing the API requests.
     */
    public static class PaymentService {

        public static Request initiateHppRegistration(RegistrationFormSettings formSettings, final IHppResultListener resultListener) {
            Request<RegistrationResponse> request = createService().initiateHpp(formSettings);
            request.execute(new Callback<RegistrationResponse>() {
                @Override
                public void onSuccess(Response<RegistrationResponse> response) {
                    String url = response.getBody().sessionUrl;
                    if (resultListener != null) {
                        if (url != null) {
                            resultListener.onHppInitiated(url);
                        } else {
                            BNLog.e(getClass().getSimpleName(), "Credit card registration failed to initiate. URL is null.");
                            resultListener.onError(RegistrationFormError.PAGE_LOAD_ERROR);
                        }
                    }
                }

                @Override
                public void onError(RequestError error) {
                    if (resultListener != null) {
                        resultListener.onError(RegistrationFormError.PAGE_LOAD_ERROR);
                    }
                }
            });
            return request;
        }

        /**
         * Makes an API call to register a credit card with given parameters for recurring payments.
         *
         * @param registrationParams    {@link RegistrationParams} with encrypted card details
         * @param callback              Result callback
         */
        public static Request registerCreditCard(RegistrationParams registrationParams, final ICardRegistrationCallback callback) {

            Request<CreditCard> request = createService().authorizeCreditCard(registrationParams);
            request.execute(new Callback<CreditCard>() {
                @Override
                public void onSuccess(Response<CreditCard> response) {
                    if (callback != null) {
                        CreditCard creditCard = response.getBody();
                        if (creditCard != null) {
                            callback.onRegistrationSuccess(creditCard);
                        } else {
                            callback.onRegistrationError(null);
                        }
                    }
                }

                @Override
                public void onError(RequestError error) {
                    if (callback != null) {
                        callback.onRegistrationError(error);
                    }
                }
            });
            return request;
        }

        /**
         * A method for making a transaction.
         *
         * @param paymentSettings {@link PaymentSettings} containing transaction details.
         * @param listener Result listener.
         */
        public static Request makeTransaction(String paymentId, PaymentSettings paymentSettings, final ITransactionListener listener) {
            Request<TransactionResponse> request = createService().makeTransaction(paymentId, paymentSettings);
            request.execute(new Callback<TransactionResponse>() {
                @Override
                public void onSuccess(Response<TransactionResponse> response) {
                    if (listener != null) {
                        listener.onTransactionSuccess();
                    }
                }

                @Override
                public void onError(RequestError error) {
                    if (listener != null) {
                        listener.onTransactionError(error);
                    }
                }
            });
            return request;
        }
    }
}
