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

package com.bambora.paymentdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.bambora.nativepayment.interfaces.ICardRegistrationCallback;
import com.bambora.nativepayment.models.creditcard.CreditCard;
import com.bambora.nativepayment.network.RequestError;
import com.bambora.nativepayment.widget.CardRegistrationFormLayout;

/**
 * TODO
 */
public class NativeCardRegistrationActivity extends AppCompatActivity implements ICardRegistrationCallback {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_card_registration);
        CardRegistrationFormLayout registrationForm = (CardRegistrationFormLayout) findViewById(R.id.registration_form);
        registrationForm.setRegistrationResultListener(this);
    }

    @Override
    public void onRegistrationSuccess(CreditCard creditCard) {
        System.out.println("========================================");
        System.out.println("Token: "+ creditCard.getCreditCardToken());
        finish();
    }

    @Override
    public void onRegistrationError(RequestError error) {
        Toast.makeText(this, "Card registration failed.", Toast.LENGTH_SHORT).show();
    }
}
