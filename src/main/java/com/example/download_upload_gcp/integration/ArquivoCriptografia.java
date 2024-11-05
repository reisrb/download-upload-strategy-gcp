package com.example.download_upload_gcp.integration;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ArquivoCriptografia {

    private ArquivoCriptografia() {
    }

    public static Cipher getCipher(final Integer encryptMode,
          final SecretKeySpec fileSecretKeySpec) {
        try {
            final Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aes.init(encryptMode, fileSecretKeySpec, new IvParameterSpec(new byte[16]));
            return aes;
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                       | InvalidAlgorithmParameterException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }
}
