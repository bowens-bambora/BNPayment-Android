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

package com.bambora.nativepayment.models;

import com.bambora.nativepayment.interfaces.IJsonResponse;
import com.bambora.nativepayment.logging.BNLog;
import com.bambora.nativepayment.models.creditcard.TransactionErrorResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link TransactionResponse} is a model representing a transaction response in the system.
 */
public class TransactionResponse implements IJsonResponse<TransactionResponse> {

    private static final String KEY_PAYMENT_ID = "payment";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_STATUS = "status";
    private static final String KEY_CODE = "code";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_INFO = "info";

    public String paymentId;

    public Integer amount;

    public String currency;

    /**
     * Error response parameters
     */

    public Integer status;

    public Integer code;

    public String errorMessage;

    public String errorInfo;

    @Override
    public TransactionResponse fromJson(String jsonString) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonString);
            paymentId = jsonObject.optString(KEY_PAYMENT_ID);
            amount = jsonObject.optInt(KEY_AMOUNT);
            currency = jsonObject.optString(KEY_CURRENCY);
            status = jsonObject.optInt(KEY_STATUS);
            code = jsonObject.optInt(KEY_CODE);
            errorMessage = jsonObject.optString(KEY_MESSAGE);
            errorInfo = jsonObject.optString(KEY_INFO);
        } catch (JSONException e) {
            BNLog.jsonParseError(getClass().getSimpleName(), e);
        }
        return this;
    }

    public TransactionErrorResponse getError() {
        return new TransactionErrorResponse(
                this.status,
                this.code,
                this.errorMessage,
                this.errorInfo
        );
    }
}
