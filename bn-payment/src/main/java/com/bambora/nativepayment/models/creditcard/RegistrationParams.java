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

import android.content.Context;
import android.util.Base64;

import com.bambora.nativepayment.interfaces.ICertificateLoadCallback;
import com.bambora.nativepayment.interfaces.IJsonRequest;
import com.bambora.nativepayment.logging.BNLog;
import com.bambora.nativepayment.managers.CertificateManager;
import com.bambora.nativepayment.security.Crypto;
import com.bambora.nativepayment.security.EncryptionCertificate;
import com.bambora.nativepayment.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * This class is an API model for the native card registration request.
 * <p>Constructor takes credit card details of the card to be registered and encrypts them. The key
 * is then also encrypted and added as a list of {@link EncryptedSessionKey}s.</p>
 */
public class RegistrationParams implements IJsonRequest {

    private static final String KEY_ENCRYPTED_CARD = "encryptedCard";
    private static final String KEY_ENCRYPTED_SESSION_KEYS = "encryptedSessionKeys";
    private static final String KEY_BIN_NUMBER = "binNumber";

    private static final int BIN_NUMBER_LENGTH = 6;

    public CardDetails encryptedCard;
    public List<EncryptedSessionKey> encryptedSessionKeys;
    public String binNumber;

    // <TEMP>
    public String pan;
    public String expMo;
    public String expYr;
    public String cvd;
    // </TEMP>

    private final Crypto crypto;
    private final CertificateManager certificateManager;

    public interface IOnEncryptionListener {
        void onEncryptionComplete();
        void onEncryptionError();
    }

    public RegistrationParams(Crypto crypto, CertificateManager certificateManager) {
        this.crypto = crypto;
        this.certificateManager = certificateManager;
    }

    /**
     * Takes credit card details of the card to be registered and encrypts them.
     * <p>A private AES key is generated with which the credit card details are encrypted. This key
     * is then encrypted with the public keys of each PSP and added as a parameter.</p>
     *
     * @param context       Context object
     * @param cardNumber    Credit card number
     * @param expiryYear    Expiry year of the card
     * @param expiryMonth   Expiry month of the card
     * @param securityCode  Security code (CVC/CVV)
     */
    public void setParametersAndEncrypt(Context context, String cardNumber, String expiryMonth,
                                        String expiryYear, String securityCode, final IOnEncryptionListener listener) {
        if (cardNumber != null) {
            cardNumber = StringUtils.stripAllNonDigits(cardNumber);
        }
        try {
            SecretKey sessionKey = generateSessionKey();

            // <TEMP>
            this.pan = cardNumber;
            this.expMo = expiryMonth;
            this.expYr = expiryYear;
            this.cvd = securityCode;
            // </TEMP>

            this.encryptedCard = new CardDetails(cardNumber, securityCode, expiryMonth, expiryYear).encrypt(sessionKey);
            encryptSessionKey(context, sessionKey, new ISessionKeyEncryptionListener() {
                @Override
                public void onEncryptionComplete(List<EncryptedSessionKey> encryptedKeys) {
                    encryptedSessionKeys = encryptedKeys;
                    if (listener != null) listener.onEncryptionComplete();
                }

                @Override
                public void onError() {
                    if (listener != null) listener.onEncryptionError();
                }
            });
            this.binNumber = parseBinNumber(cardNumber);
        } catch (Exception exception) {
            BNLog.e(getClass().getSimpleName(), "Failed to encrypt card registration parameters.", exception);
            this.encryptedCard = null;
            this.encryptedSessionKeys = null;
            if (listener != null) listener.onEncryptionError();
        }
    }

    @Override
    public String getSerialized() {
        JSONObject jsonObject = new JSONObject();
        try {
            // <TEMP>
            jsonObject.put("number", pan);
            jsonObject.put("expiry_month", expMo);
            jsonObject.put("expiry_year", expYr);
            jsonObject.put("cvd", cvd);
            // </TEMP>

            /*if (encryptedCard != null) {
                jsonObject.put(KEY_ENCRYPTED_CARD, encryptedCard.getJsonObject());
            }
            if (encryptedSessionKeys != null) {
                JSONArray sessionKeys = new JSONArray();
                for (EncryptedSessionKey key : encryptedSessionKeys) {
                    sessionKeys.put(key.getJsonObject());
                }
                jsonObject.put(KEY_ENCRYPTED_SESSION_KEYS, sessionKeys);
            }
            jsonObject.putOpt(KEY_BIN_NUMBER, this.binNumber);
            */
        } catch (JSONException e) {
            BNLog.jsonParseError(getClass().getSimpleName(), e);
        }
        System.out.println("-----------------------------\nToken Request:\n");
        System.out.println(jsonObject.toString());
        System.out.println("-----------------------------");
        return jsonObject.toString();
    }

    public interface ISessionKeyEncryptionListener {
        void onEncryptionComplete(List<EncryptedSessionKey> encryptedKeys);
        void onError();
    }

    /**
     * TODO
     * Encrypts the given session key with the public keys of each PSP.
     * <p>Loads encryption certificates for each PSP from disk and uses their public keys to encrypt
     * the session key. The encrypted values are then added to a list together with the certificate
     * fingerprints.</p>
     *
     * @param context       Context object
     * @param sessionKey    Session key to be added
     * @return List of {@link EncryptedSessionKey}s
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException
     */
    private void encryptSessionKey(Context context, final SecretKey sessionKey,
                final ISessionKeyEncryptionListener listener) {
        certificateManager.getEncryptionCertificates(context, new ICertificateLoadCallback() {
            @Override
            public void onCertificatesLoaded(List<EncryptionCertificate> certificates) {
                if (certificates == null || certificates.isEmpty()) {
                    BNLog.e(RegistrationParams.class.getSimpleName(), "No valid encryption certificates. Encryption aborted.");
                    listener.onError();
                    return;
                }
                List<EncryptedSessionKey> sessionKeyList = new ArrayList<>();
                try {
                    for (EncryptionCertificate certificate : certificates) {
                        byte[] encryptedKey = crypto.RSAEncrypt(sessionKey.getEncoded(), certificate.getPublicKey());
                        String encodedKey = Base64.encodeToString(encryptedKey, Base64.NO_WRAP);
                        sessionKeyList.add(new EncryptedSessionKey(certificate.getFingerprint(), encodedKey));
                    }
                    listener.onEncryptionComplete(sessionKeyList);
                } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                    BNLog.e(RegistrationParams.class.getSimpleName(), "Failed to encrypt data.", e);
                    listener.onError();
                }
            }

            @Override
            public void onError() {
                listener.onError();
            }
        });
    }

    /**
     * Generates a random AES 128 key
     * @return A {@link SecretKey}
     * @throws NoSuchAlgorithmException
     */
    private SecretKey generateSessionKey() throws NoSuchAlgorithmException {
        return crypto.generateRandomAES128();
    }

    /**
     * API model of the card details object.
     */
    public class CardDetails {

        private static final String KEY_CARD_NUMBER = "cardNumber";
        private static final String KEY_CVC_CODE = "cvcCode";
        private static final String KEY_EXPIRY_MONTH = "expiryMonth";
        private static final String KEY_EXPIRY_YEAR = "expiryYear";

        public String cardNumber;
        public String securityCode;
        public String expiryMonth;
        public String expiryYear;

        public CardDetails(String cardNumber, String securityCode, String expiryMonth, String expiryYear)
                throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
                UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException,
                InvalidAlgorithmParameterException {
            this.cardNumber = cardNumber;
            this.securityCode = securityCode;
            this.expiryMonth = expiryMonth;
            this.expiryYear = expiryYear;
        }

        public JSONObject getJsonObject() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(KEY_CARD_NUMBER, cardNumber);
                jsonObject.put(KEY_CVC_CODE, securityCode);
                jsonObject.put(KEY_EXPIRY_MONTH, expiryMonth);
                jsonObject.put(KEY_EXPIRY_YEAR, expiryYear);
            } catch (JSONException e) {
                BNLog.jsonParseError(getClass().getSimpleName(), e);
            }
            return jsonObject;
        }

        /**
         * Encrypts all fields using the given {@link SecretKey}.
         *
         * @param privateKey
         * @return
         * @throws NoSuchPaddingException
         * @throws BadPaddingException
         * @throws InvalidAlgorithmParameterException
         * @throws NoSuchAlgorithmException
         * @throws IllegalBlockSizeException
         * @throws UnsupportedEncodingException
         * @throws InvalidKeyException
         */
        public CardDetails encrypt(SecretKey privateKey) throws NoSuchPaddingException,
                BadPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
                IllegalBlockSizeException, UnsupportedEncodingException, InvalidKeyException {
            cardNumber = crypto.AESEncryptAndEncode(cardNumber, privateKey);
            expiryYear = crypto.AESEncryptAndEncode(expiryYear, privateKey);
            expiryMonth = crypto.AESEncryptAndEncode(expiryMonth, privateKey);
            securityCode = crypto.AESEncryptAndEncode(securityCode, privateKey);
            return this;
        }
    }

    /**
     * API model of the encrypted session key.
     * <p>The model includes the session key and the certificate fingerprint of the certificates
     * used for encryption.</p>
     */
    public class EncryptedSessionKey {

        private static final String KEY_FINGERPRINT = "fingerprint";
        private static final String KEY_SESSION_KEY = "sessionKey";

        public String fingerprint;
        public String sessionKey;

        public EncryptedSessionKey(String fingerprint, String sessionKey) {
            this.fingerprint = fingerprint;
            this.sessionKey = sessionKey;
        }

        public JSONObject getJsonObject() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(KEY_FINGERPRINT, fingerprint);
                jsonObject.put(KEY_SESSION_KEY, sessionKey);
            } catch (JSONException e) {
                BNLog.jsonParseError(getClass().getSimpleName(), e);
            }
            return jsonObject;
        }
    }

    private String parseBinNumber(String cardNumber) {
        if (cardNumber.length() >= BIN_NUMBER_LENGTH) {
            return cardNumber.substring(0, BIN_NUMBER_LENGTH);
        }
        return null;
    }
}
