package cl.elvolcan.usuarios.security;

import cl.elvolcan.common.security.SecuritySupport;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Par de claves RSA para firmar el JWT (RS256).
 *  - La clave PRIVADA solo la tiene este servicio y sirve para firmar.
 *  - La clave PÚBLICA se publica en GET /api/v1/auth/jwks; con ella los demás servicios
 *    comprueban que el token lo firmó usuarios, sin poder fabricar tokens.
 * Se generan la primera vez en app.keys-directory (./data/keys, no se sube a Git) y se
 * reutilizan después, así los tokens siguen siendo válidos si el servicio se reinicia.
 */
@Component
public class JwtKeys {

  private final RSAKey rsa;

  public JwtKeys(@Value("${app.keys-directory}") String carpeta) throws Exception {
    Path dir = Path.of(carpeta);
    Files.createDirectories(dir);
    Path privada = dir.resolve("private.pk8");
    Path publica = dir.resolve("public.der");
    if (!Files.exists(privada) || !Files.exists(publica)) {
      KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
      generador.initialize(2048);
      KeyPair par = generador.generateKeyPair();
      Files.write(privada, par.getPrivate().getEncoded());
      Files.write(publica, par.getPublic().getEncoded());
    }
    KeyFactory fabrica = KeyFactory.getInstance("RSA");
    RSAPrivateKey priv =
        (RSAPrivateKey) fabrica.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(privada)));
    RSAPublicKey pub =
        (RSAPublicKey) fabrica.generatePublic(new X509EncodedKeySpec(Files.readAllBytes(publica)));
    // kid = huella de la clave pública: permite cambiar la clave en el futuro sin confusiones.
    String kid =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(pub.getEncoded()));
    rsa =
        new RSAKey.Builder(pub)
            .privateKey(priv)
            .keyID(kid)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .build();
  }

  /** Firma tokens con la clave privada. */
  public JwtEncoder encoder() {
    return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsa)));
  }

  /**
   * Valida los tokens que llegan a este mismo servicio (/auth/me y /usuarios) con la clave
   * pública en memoria, sin llamarse a sí mismo por HTTP. Revisa lo mismo que
   * SecuritySupport.decoder en los otros servicios: firma, vencimiento, emisor y audiencia.
   */
  public JwtDecoder decoder(String issuer) throws Exception {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsa.toRSAPublicKey()).build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(SecuritySupport.AUDIENCIA)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Audiencia incorrecta", null))));
    return decoder;
  }

  /** JWKS público: {"keys":[{"kty":"RSA","kid":...,"n":...,"e":"AQAB",...}]}. Sin la parte privada. */
  public Map<String, Object> jwksPublico() {
    return new JWKSet(rsa.toPublicJWK()).toJSONObject();
  }
}
