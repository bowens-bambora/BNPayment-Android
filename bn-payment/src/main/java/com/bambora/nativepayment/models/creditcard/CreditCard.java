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

package com.bambora.nativepayment.models.creditcard;

import com.bambora.nativepayment.interfaces.IJsonResponse;
import com.bambora.nativepayment.json.JsonContainer;
import com.bambora.nativepayment.utils.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * @author Lovisa Corp
 */
public class CreditCard implements Serializable, IJsonResponse<CreditCard> {

    private static final String KEY_RECURRING_PAYMENT_ID = "recurringPaymentID";
    private static final String KEY_CARD_NUMBER = "cardNumber";
    private static final String KEY_CARD_TYPE = "cardType";
    private static final String KEY_EXPIRY_MONTH = "expiryMonth";
    private static final String KEY_EXPIRY_YEAR = "expiryYear";

    /**
     * Alias of the card
     */
    private String alias;

    /**
     * Truncated credit card number
     */
    private String truncatedCardNumber;

    /**
     * Card expiry month
     */
    private Integer expiryMonth;

    /**
     * Card expiry year
     */
    private Integer expiryYear;

    /**
     * Payment type, such as Visa or MasterCard
     */
    private String paymentType;

    /**
     * ID of transaction created at the time of card registration
     */
    private String transactionId;

    /**
     * Token to be used when making transactions
     */
    private String creditCardToken;

    public CreditCard() {}

    public CreditCard(String truncatedCardNumber, Integer expiryMonth, Integer expiryYear,
                      String paymentType, String transactionId, String creditCardToken) {
        this.truncatedCardNumber = truncatedCardNumber;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.paymentType = paymentType;
        this.transactionId = transactionId;
        this.creditCardToken = creditCardToken;
    }

    public CreditCard(RegistrationResult registrationResult) {
        if (registrationResult != null) {
            this.truncatedCardNumber = registrationResult.truncatedCardNumber;
            this.expiryMonth = registrationResult.expiryMonth;
            this.expiryYear = registrationResult.expiryYear;
            this.paymentType = registrationResult.paymentType;
            this.transactionId = registrationResult.transactionId;
            this.creditCardToken = registrationResult.subscriptionId;
        }
    }

    @Override
    public CreditCard fromJson(JsonContainer jsonContainer) throws JSONException {
        JSONObject jsonObject = jsonContainer.getJsonObject();

        // <TEMP>
        this.creditCardToken = JsonUtils.getStringIfExists(jsonObject, "token");
        System.out.println("=======================\nToken: "+this.creditCardToken+"\n======================");
        this.truncatedCardNumber = "4030 XXXX XXXX 1234";
        this.paymentType = "vi";
        this.expiryMonth = 4;
        this.expiryYear = 20;
        // </TEMP>

        /*this.creditCardToken = JsonUtils.getStringIfExists(jsonObject, KEY_RECURRING_PAYMENT_ID);
        this.truncatedCardNumber = JsonUtils.getStringIfExists(jsonObject, KEY_CARD_NUMBER);
        this.paymentType = JsonUtils.getStringIfExists(jsonObject, KEY_CARD_TYPE);
        this.expiryMonth = JsonUtils.getIntIfExists(jsonObject, KEY_EXPIRY_MONTH);
        this.expiryYear = JsonUtils.getIntIfExists(jsonObject, KEY_EXPIRY_YEAR);*/
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTruncatedCardNumber() {
        return truncatedCardNumber;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCreditCardToken() {
        return creditCardToken;
    }

    /**
     * Checks if given {@link CreditCard} has the same token as this
     * @param otherCard Credit card to compare with
     * @return          True if the tokens are equal
     */
    public boolean isEqualTo(CreditCard otherCard) {
        return otherCard.getCreditCardToken().equals(this.creditCardToken);
    }
}
