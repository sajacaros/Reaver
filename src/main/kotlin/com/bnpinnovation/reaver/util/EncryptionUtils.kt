package com.bnpinnovation.reaver.util

import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import javax.crypto.Cipher

class EncryptionUtils {
    companion object{
        fun encrypt(privateKey: PrivateKey, plainData: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(plainData)
        }

        fun decrypt(publicKey: PublicKey, encryptData: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return cipher.doFinal(encryptData)
        }

        fun sign(privateKey: PrivateKey, plainData: ByteArray): ByteArray {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey);
            signature.update(plainData);
            return signature.sign()
        }

        fun verify(publicKey: PublicKey, signatureData: ByteArray, plainData: ByteArray): Boolean {
            val signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey)
            signature.update(plainData)
            return signature.verify(signatureData)
        }
    }
}