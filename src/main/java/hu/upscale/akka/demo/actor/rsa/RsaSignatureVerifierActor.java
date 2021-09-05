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
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import lombok.Builder;
import lombok.Data;

/**
 * @author László Zoltán
 */
public class RsaSignatureVerifierActor extends AbstractActor {

    private static final PublicKey PUBLIC_KEY = getRsaPublicKeyFromX509Certificate(ResourceUtil.readResourceFile("rsa/publicKey.der"));

    public static Props props() {
        return Props.create(RsaSignatureVerifierActor.class, RsaSignatureVerifierActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(RsaSignatureVerifierRequest.class, rsaSignatureVerifierRequest ->
                getSender().tell(
                    RsaSignatureVerifierResponse.builder().verified(verifySignature(rsaSignatureVerifierRequest.getSignature())).build(),
                    getSelf()
                )
            )
            .build();
    }

    private boolean verifySignature(byte[] signature) {
        try {
            Signature rsaSignature = Signature.getInstance(SIGNATURE_ALGORITHM);
            rsaSignature.initVerify(PUBLIC_KEY);
            rsaSignature.update(signature);

            return rsaSignature.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new CryptographyException("Failed to verify signature", e);
        }
    }

    private static PublicKey getRsaPublicKeyFromX509Certificate(byte[] certificateBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_FACTORY_ALGORITHM);
            return keyFactory.generatePublic(new X509EncodedKeySpec(certificateBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptographyException("Failed to load RSA public key", e);
        }
    }

    @Data
    @Builder
    public static final class RsaSignatureVerifierRequest {

        private final byte[] signature;

    }

    @Data
    @Builder
    public static final class RsaSignatureVerifierResponse {

        private final boolean verified;

    }
}
