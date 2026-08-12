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

    /**
     * Dispatch entry point injected at the tool runner `Ll/ᩴۜܶ;->ܳ()V`.
     *
     * The runner is constructed with a tool descriptor (`Ll/۫ۜܶ;`) stored in
     * the obfuscated field `ۤ`. All field/method names are resolved by
     * reflection so the injected smali stays clean (no obfuscated references).
     * See {@link #dispatchByOutput} for the actual routing.
     *
     * @param runner the `l/ᩴۜܶ` tool-runner instance
     * @return true when the extension handled the tool
     */
    @SuppressWarnings("unused")
    public static boolean dispatchRunner(Object runner) {
        try {
            if (runner == null) return false;
            Object descriptor = getField(runner, "\u06e4"); // ۤ
            if (descriptor == null) return false;
            Object fileNode = callObject(descriptor, "\u06df"); // ۟()
            if (fileNode == null) return false;
            String outputPath = callString(fileNode, "\u05ab\u06df"); // ֫۟()
            if (outputPath == null || outputPath.isEmpty()) return false;
            return dispatchByOutput(outputPath);
        } catch (Throwable t) {
            android.util.Log.e("MtTools", "dispatchRunner failed", t);
            return false;
        }
    }

    /**
     * Routes a conversion by MT Manager's output-path convention.
     * The input file is derived from the output path by scanning the same
     * directory for a dex source.
     */
    @SuppressWarnings("unused")
    public static boolean dispatchByOutput(String outputPath) {
        try {
            if (outputPath == null) return false;
            String out = outputPath.toLowerCase();

            if (out.endsWith("_smali.zip")) {
                String input = findDexInput(outputPath);
                if (input == null) return false;
                return dex2Smali(input, outputPath);
            }
            if (out.endsWith(".jar")) {
                String input = findDexInput(outputPath);
                if (input == null) return false;
                return dex2Jar(input, outputPath);
            }
            if (out.endsWith(".apk")) {
                return signApk(outputPath, siblingPath(outputPath, "_signed.apk"));
            }
            return false;
        } catch (Throwable t) {
            android.util.Log.e("MtTools", "dispatchByOutput failed", t);
            return false;
        }
    }

    /**
     * Finds the source dex for a conversion output path. MT Manager names
     * outputs after the input: `test.dex.bak` -> `test.dex_smali.zip`, so we
     * strip the `_smali.zip`/`.jar` suffix and look for a sibling dex/bak.
     */
    private static String findDexInput(String outputPath) {
        File out = new File(outputPath);
        File dir = out.getParentFile();
        if (dir == null) dir = new File(".");
        String name = out.getName().toLowerCase();
        String base = name;
        if (base.endsWith("_smali.zip")) base = base.substring(0, base.length() - "_smali.zip".length());
        else if (base.endsWith(".zip")) base = base.substring(0, base.length() - ".zip".length());
        else if (base.endsWith(".jar")) base = base.substring(0, base.length() - ".jar".length());

        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (!f.isFile()) continue;
            String fn = f.getName().toLowerCase();
            if (fn.startsWith(base) && (fn.endsWith(".dex") || fn.endsWith(".bak") || fn.endsWith(".apk"))) {
                return f.getAbsolutePath();
            }
        }
        // Fall back: any single .dex/.bak in the directory
        java.util.List<String> candidates = new java.util.ArrayList<>();
        for (File f : files) {
            if (!f.isFile()) continue;
            String fn = f.getName().toLowerCase();
            if (fn.endsWith(".dex") || fn.endsWith(".bak")) candidates.add(f.getAbsolutePath());
        }
        if (candidates.size() == 1) return candidates.get(0);
        return null;
    }

    private static Object getField(Object target, String name) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static String callString(Object target, String name) {
        Object v = callObject(target, name);
        return v instanceof String ? (String) v : null;
    }

    private static Object callObject(Object target, String name) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterTypes().length == 0) {
                        m.setAccessible(true);
                        return m.invoke(target);
                    }
                }
            } catch (Exception e) {
                return null;
            }
            c = c.getSuperclass();
        }
        return null;
    }


    /**
     * Sign-tool dispatch entry injected at `Ll/ܿۜܶ;->۟(...)`. Empirically
     * (frida): arg0 = the OUTPUT apk path ("..._sign.apk"), arg1 = the
     * SigningKey options object. The input apk is the sibling without the
     * "_sign" suffix (test.apk -> test_sign.apk). We derive it and sign.
     */
    @SuppressWarnings("unused")
    public static boolean dispatchSign(Object arg0, Object arg1) {
        try {
            String out = null;
            if (arg0 instanceof String) out = (String) arg0;
            else if (arg0 != null) out = callString(arg0, "\u05ab\u06df"); // ֫۟()
            if (out == null || !out.toLowerCase().endsWith(".apk")) return false;
            String in = out.replaceAll("_sign\\.apk$", ".apk");
            if (in.equals(out)) in = out.replaceAll("\\.apk$", ".apk");
            if (!new File(in).isFile()) {
                // Fall back to sibling scan
                in = null;
                File dir = new File(out).getParentFile();
                if (dir != null) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (!f.isFile()) continue;
                            String fn = f.getName().toLowerCase();
                            if (fn.endsWith(".apk") && !fn.contains("_sign")) {
                                in = f.getAbsolutePath();
                                break;
                            }
                        }
                    }
                }
            }
            if (in == null) return false;
            return signApk(in, out);
        } catch (Throwable t) {
            android.util.Log.e("MtTools", "dispatchSign failed", t);
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
