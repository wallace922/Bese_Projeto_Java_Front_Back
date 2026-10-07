package com.bese.tesouraria.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia de dados sensíveis em repouso (CPF/CNPJ) + índice cego (HMAC).
 *
 * <p>Chave vinda de {@code CRYPTO_KEY} no .env (mínimo 32 chars, fail-fast no
 * boot). Dela derivam duas subchaves via SHA-256 (domínios separados):
 * <ul>
 * <li>AES-256/GCM com IV aleatório para o valor (coluna original).</li>
 * <li>HMAC-SHA256 sobre os dígitos normalizados para busca exata
 * (coluna {@code *_hash}), já que valor cifrado com IV aleatório não
 * permite comparação no banco.</li>
 * </ul>
 *
 * <p>Transição: {@link #decrypt(String)} devolve o próprio valor quando ele
 * não é um envelope válido (linha legada em claro). Os services fazem
 * fallback para a busca legada e regravam cifrado + hash no primeiro uso.
 */
@Service
public class CryptoService {

    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private static volatile CryptoService instance;

    @Value("${CRYPTO_KEY:}")
    private String cryptoKey;

    private SecretKeySpec aesKey;
    private SecretKeySpec hmacKey;
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    void init() {
        if (cryptoKey == null || cryptoKey.isBlank() || cryptoKey.length() < 32) {
            throw new IllegalStateException(
                    "CRYPTO_KEY ausente ou curta (mínimo 32 caracteres). "
                    + "Defina no .env; gere com: openssl rand -hex 32");
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            aesKey = new SecretKeySpec(
                    sha.digest(("aes|" + cryptoKey).getBytes(StandardCharsets.UTF_8)), "AES");
            hmacKey = new SecretKeySpec(
                    sha.digest(("hmac|" + cryptoKey).getBytes(StandardCharsets.UTF_8)), "HmacSHA256");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chaves de CRYPTO_KEY", e);
        }
        instance = this;
    }

    /** Acesso para os JPA converters (não gerenciados pelo Spring). */
    public static CryptoService get() {
        return instance;
    }

    /** Somente dígitos (normalização antes de cifrar/indexar). */
    public static String digits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array();
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar dado sensível", e);
        }
    }

    public String decrypt(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(payload);
            if (all.length <= GCM_IV_BYTES) {
                return payload;
            }
            ByteBuffer buf = ByteBuffer.wrap(all);
            byte[] iv = new byte[GCM_IV_BYTES];
            buf.get(iv);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Linha legada em claro ou envelope inválido: devolve como está.
            return payload;
        }
    }

    /** HMAC-SHA256 hexadecimal (minúsculo) para busca exata. */
    public String hmacHex(String normalized) {
        if (normalized == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] out = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar índice HMAC", e);
        }
    }
}
