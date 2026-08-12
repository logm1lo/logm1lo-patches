package app.morphe.extension.mtmanager;

import com.android.apksig.ApkSigner;
import com.android.apksig.ApkSigner.SignerConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

/**
 * Pure-Java reimplementation of MT Manager's "Sign file" tool.
 *
 * Uses Android's official apksig library. The signing key is an embedded
 * PKCS#12 keystore (Base64, generated at build time), which re-signs APKs with
 * a stable self-signed key. This mirrors MT Manager's "Signing Key Default"
 * behaviour: a fixed key, not the stock MT key.
 */
final class MtApkSigner {

    private static final String KEYSTORE_PASSWORD = "morphemt";
    private static final String KEY_ALIAS = "mt";
    private static final String KEYSTORE_B64 =
        "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";

    private MtApkSigner() {
    }

    static boolean sign(String apkIn, String apkOut) throws Exception {
        File inFile = new File(apkIn);
        if (!inFile.isFile()) throw new IllegalArgumentException("Input is not a file: " + apkIn);

        File outFile = new File(apkOut);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Cannot create output dir: " + parent);
        }

        KeyStore ks = loadKeyStore();
        SignerConfig signerConfig = buildSignerConfig(ks);

        ApkSigner.Builder builder = new ApkSigner.Builder(Collections.singletonList(signerConfig));
        builder.setInputApk(inFile);
        builder.setOutputApk(outFile);
        builder.setV1SigningEnabled(true);
        builder.setV2SigningEnabled(true);
        builder.setV3SigningEnabled(true);
        builder.setMinSdkVersion(21);
        builder.build().sign();

        return outFile.isFile() && outFile.length() > 0;
    }

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        byte[] bytes = android.util.Base64.decode(KEYSTORE_B64, android.util.Base64.DEFAULT);
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            ks.load(in, KEYSTORE_PASSWORD.toCharArray());
        }
        return ks;
    }

    private static SignerConfig buildSignerConfig(KeyStore ks) throws Exception {
        PrivateKey privateKey = (PrivateKey) ks.getKey(KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray());
        if (privateKey == null) throw new IllegalStateException("No key for alias " + KEY_ALIAS);

        // Build the certificate chain from the keystore (in signing order).
        java.security.cert.Certificate[] chain = ks.getCertificateChain(KEY_ALIAS);
        if (chain == null || chain.length == 0) {
            chain = new java.security.cert.Certificate[]{ks.getCertificate(KEY_ALIAS)};
        }
        List<X509Certificate> certs = new java.util.ArrayList<>(chain.length);
        for (java.security.cert.Certificate c : chain) {
            certs.add((X509Certificate) c);
        }

        return new SignerConfig.Builder(KEY_ALIAS, privateKey, certs).build();
    }
}
