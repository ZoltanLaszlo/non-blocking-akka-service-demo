package hu.upscale.akka.demo.actor.rsa;

import static hu.upscale.akka.demo.actor.rsa.RsaActor.RSA_KEY_FACTORY_ALGORITHM;
import static hu.upscale.akka.demo.actor.rsa.RsaActor.SIGNATURE_ALGORITHM;

import akka.actor.AbstractActor;
import akka.actor.Props;
import hu.upscale.akka.demo.exception.CryptographyException;
import hu.upscale.akka.demo.util.ResourceUtil;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author László Zoltán
 */
public class RsaSignerActor extends AbstractActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RsaSignerActor.class);

    private static final PrivateKey PRIVATE_KEY = getRsaPrivateKeyFromPkcs8EncodedKey(ResourceUtil.readResourceFile("rsa/privateKey.der"));

    public static Props props() {
        return Props.create(RsaSignerActor.class, RsaSignerActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(RsaSignerRequest.class, rsaSignerRequest ->
                getSender().tell(
                    RsaSignerResponse.builder().signature(signData(rsaSignerRequest.getData())).build(),
                    getSelf()
                )
            )
            .build();
    }

    private byte[] signData(byte[] data) {
        try {
            Signature rsaSignature = Signature.getInstance(SIGNATURE_ALGORITHM);
            rsaSignature.initSign(PRIVATE_KEY);
            rsaSignature.update(data);

            byte[] signature = rsaSignature.sign();

            LOGGER.info("RSA signature created");

            return signature;
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new CryptographyException("Failed to sign data", e);
        }
    }

    private static PrivateKey getRsaPrivateKeyFromPkcs8EncodedKey(byte[] certificateBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_FACTORY_ALGORITHM);
            PKCS8EncodedKeySpec keySpecPv = new PKCS8EncodedKeySpec(certificateBytes);

            return keyFactory.generatePrivate(keySpecPv);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptographyException("Failed to load RSA private key", e);
        }
    }

    @Data
    @Builder
    public static final class RsaSignerRequest {

        private final byte[] data;

    }

    @Data
    @Builder
    public static final class RsaSignerResponse {

        private final byte[] signature;

    }

}
