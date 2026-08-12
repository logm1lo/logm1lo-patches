package app.morphe.extension.mtmanager;

import android.content.Context;

import java.io.File;

/**
 * Entry points injected into MT Manager's native tool dispatch sites.
 *
 * MT Manager 2.26.8 relies on `libmtprotect.so` natives for its conversion
 * tools (Dex2Smali, Dex2Jar, Sign, Dex Editor parser). Those natives are
 * absent on re-signed builds (the Morphe patch stubs all `l.*` natives), which
 * leaves the tools "hollow" — they open but never produce output.
 *
 * This extension re-implements the tool kernels in pure Java:
 *   - {@link #dex2Smali} — baksmali (dexlib2)
 *   - {@link #dex2Jar}   — dex2jar (d2j)
 *   - {@link #signApk}   — apksig
 *
 * The Morphe patch injects `invoke-static` calls to these methods at the
 * native dispatch sites so the app's own (broken) native path is bypassed and
 * the conversion is performed by this code instead.
 */
public final class MtTools {

    private MtTools() {
    }

    /** Initialized by the injected call site (app Context). */
    private static volatile Context context;

    /**
     * Called by the patch once at startup to let the extension cache the app
     * context (used for keystore access and temp dirs).
     */
    @SuppressWarnings("unused")
    public static void attach(Context ctx) {
        context = ctx != null ? ctx.getApplicationContext() : null;
    }

    public static Context getContext() {
        return context;
    }

    /**
     * Reimplements the Dex2Smali tool: converts a .dex file to a .zip
     * containing the disassembled smali sources (baksmali layout).
     *
     * @param dexPath  absolute path of the input .dex file
     * @param zipPath  absolute path of the output .zip file
     * @return true on success, false on failure (logs the error)
     */
    @SuppressWarnings("unused")
    public static boolean dex2Smali(String dexPath, String zipPath) {
        try {
            return Dex2SmaliConverter.convert(dexPath, zipPath);
        } catch (Throwable t) {
            android.util.Log.e("MtTools", "dex2Smali failed", t);
            return false;
        }
    }

    /**
     * Reimplements the Dex2Jar tool: converts a .dex file to a .jar of
     * .class files (dex2jar translation).
     *
     * @param dexPath absolute path of the input .dex file
     * @param jarPath absolute path of the output .jar file
     * @return true on success, false on failure
     */
    @SuppressWarnings("unused")
    public static boolean dex2Jar(String dexPath, String jarPath) {
        try {
            return Dex2JarConverter.convert(dexPath, jarPath);
        } catch (Throwable t) {
            android.util.Log.e("MtTools", "dex2Jar failed", t);
            return false;
        }
    }

    /**
     * Reimplements the Sign tool: signs an APK with the bundled
     * MT-Extension keystore (V1+V2+V3).
     *
     * @param apkIn  absolute path of the unsigned input APK
     * @param apkOut absolute path of the signed output APK
     * @return true on success, false on failure
     */
    @SuppressWarnings("unused")
    public static boolean signApk(String apkIn, String apkOut) {
        try {
            return MtApkSigner.sign(apkIn, apkOut);
        } catch (Throwable t) {
            android.util.Log.e("MtTools", "signApk failed", t);
            return false;
        }
    }

    /**
     * Convenience: derives a sibling output path from an input path by
     * replacing the extension (e.g. /a/b.dex + ".zip" -> /a/b.zip).
     */
    @SuppressWarnings("unused")
    public static String siblingPath(String inputPath, String newExt) {
        if (inputPath == null) return null;
        int dot = inputPath.lastIndexOf('.');
        String base = dot > inputPath.lastIndexOf('/') ? inputPath.substring(0, dot) : inputPath;
        return base + newExt;
    }

    /**
     * Dispatch entry point injected at the conversion bridge
     * `Ll/᩸ۖ֡;->۟(Ll/᩸ۖ֡;Ljava/lang/String;Ljava/lang/String;Ll/ܺܽܺ;)V`.
     *
     * Both the Dex2Smali task (`l/ۙۖ֡`) and the Dex2Jar task (`l/ۛۖ֡`) funnel
     * through that bridge with the resolved input/output paths. The output
     * extension tells us which tool was requested:
     *   - ".jar"  -> Dex2Jar
     *   - ".zip"  -> Dex2Smali
     *   - ".apk"  -> Sign (input must also be an apk)
     *
     * Returns true when this extension handled the conversion, in which case
     * the patch skips the broken native flow.
     */
    @SuppressWarnings("unused")
    public static boolean dispatch(String inputPath, String outputPath) {
        try {
            if (inputPath == null || outputPath == null) return false;
            String in = inputPath.toLowerCase();
            String out = outputPath.toLowerCase();

            // Dex2Jar: input must be a dex file, output a jar.
            if (in.endsWith(".dex") && out.endsWith(".jar")) return dex2Jar(inputPath, outputPath);
            // Dex2Smali: input must be a dex file, output a zip (smali.zip).
            if (in.endsWith(".dex") && out.endsWith(".zip")) return dex2Smali(inputPath, outputPath);
            // Sign: only act when BOTH input and output are apks (file dispatcher
            // passes path==path). Output equal to input -> sibling _signed.apk.
            if (in.endsWith(".apk") && out.endsWith(".apk")) {
                String signOut = outputPath.equals(inputPath) ? siblingPath(inputPath, "_signed.apk") : outputPath;
                return signApk(inputPath, signOut);
            }
            return false;
        } catch (Throwable t) {
            android.util.Log.e("MtTools", "dispatch failed", t);
            return false;
        }
    }

    /** Creates a temp directory under the app cache (fallback to java.io.tmpdir). */
    static File tempDir(String prefix) {
        File cache = context != null ? context.getCacheDir() : new File(System.getProperty("java.io.tmpdir", "/data/local/tmp"));
        File dir = new File(cache, prefix + System.nanoTime());
        if (!dir.mkdirs()) dir = new File(System.getProperty("java.io.tmpdir", "/data/local/tmp"), prefix + System.nanoTime());
        return dir;
    }

    static void deleteRecursive(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
