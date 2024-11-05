package com.example.download_upload_gcp.config;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// CRIPTOGRAFIA DO ARQUIVO
@Configuration
public class SecretKeyConfiguration {

    @Value("${file.salt}")
    private String salt;

    @Value("${file.secretKey}")
    private String secretKey;

    @Bean
    public SecretKeySpec secretKeySpec() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        final KeySpec spec =
              new PBEKeySpec(this.secretKey.toCharArray(), this.salt.getBytes(), 65536, 256);
        final SecretKey temporary = factory.generateSecret(spec);
        return new SecretKeySpec(temporary.getEncoded(), "AES");
    }

}
