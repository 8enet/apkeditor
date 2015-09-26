/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zzzmode.apkeditor.apksigner;


import com.zzzmode.apkeditor.utils.Base64;

import java.io.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Pattern;

/**
 * HISTORICAL NOTE:
 * <p/>
 * Prior to the keylimepie release, SignApk ignored the signature
 * algorithm specified in the certificate and always used SHA1withRSA.
 * <p/>
 * Starting with keylimepie, we support SHA256withRSA, and use the
 * signature algorithm in the certificate to select which to use
 * (SHA256withRSA or SHA1withRSA).
 * <p/>
 * Because there are old keys still in use whose certificate actually
 * says "MD5withRSA", we treat these as though they say "SHA1withRSA"
 * for compatibility with older releases.  This can be changed by
 * altering the getAlgorithm() function below.
 */
/**
 *  原始代码见aosp项目目录 build/tools/signapk/SignApk.java
 *  如何生成privateKey 和 sigPrefix 见{@see KeyHelper}
 */
public class SignApk {
    private static final String META_INF = "META-INF/";

    // prefix for new signature-related files in META-INF directory
    private static final String SIG_PREFIX = META_INF + "SIG-";


    private static final String CERT_SF_NAME = META_INF+"CERT.SF";
    private static final String CERT_RSA_NAME = META_INF+"CERT.RSA";


    // Files matching this pattern are not copied to the output.
    private static Pattern stripPattern =
            Pattern.compile("^(META-INF/((.*)[.](SF|RSA|DSA)))|(" +
                    Pattern.quote(JarFile.MANIFEST_NAME) + ")$");


    private String privateKey;
    private String sigPrefix;

    public SignApk(String privateKey, String sigPrefix) {
        this.privateKey = privateKey;
        this.sigPrefix = sigPrefix;
    }


    /**
     * Add the hash(es) of every file to the manifest, creating it if
     * necessary.
     */
    private Manifest addDigestsToManifest(JarFile jar)
            throws IOException, GeneralSecurityException {
        Manifest input = jar.getManifest();
        Manifest output = new Manifest();
        Attributes main = output.getMainAttributes();
        if (input != null) {
            main.putAll(input.getMainAttributes());
        } else {
            main.putValue("Manifest-Version", "1.0");
            main.putValue("Created-By", "1.0 (Android SignApk)");
        }

        MessageDigest md_sha1 = MessageDigest.getInstance("SHA1");

        byte[] buffer = new byte[4096];
        int num;

        // We sort the input entries by name, and add them to the
        // output manifest in sorted order.  We expect that the output
        // map will be deterministic.

        TreeMap<String, JarEntry> byName = new TreeMap<String, JarEntry>();

        for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
            JarEntry entry = e.nextElement();
            byName.put(entry.getName(), entry);
        }

        for (JarEntry entry : byName.values()) {
            String name = entry.getName();
            if (!entry.isDirectory() &&
                    (stripPattern == null || !stripPattern.matcher(name).matches())) {
                InputStream data = jar.getInputStream(entry);
                while ((num = data.read(buffer)) > 0) {
                    md_sha1.update(buffer, 0, num);
                }

                Attributes attr = null;
                if (input != null) attr = input.getAttributes(name);
                attr = attr != null ? new Attributes(attr) : new Attributes();
                attr.putValue("SHA1-Digest", new String(Base64.encodeBase64(md_sha1.digest()), "ASCII"));
                output.getEntries().put(name, attr);
            }
        }

        return output;
    }


    /**
     * 此处会有bug，在jdk和android上出现的结果不同，建议只重写write(int b)即可，
     * 在android 上会调用write(int b)后再次调用write(byte[] b, int off, int len)导致数据重复出错
     *
     * Write to another stream and track how many bytes have been
     * written.
     */
    private static class CountOutputStream extends FilterOutputStream {
        private int mCount;
        private Signature mSignature;

        public CountOutputStream(OutputStream out, Signature sig) {
            super(out);
            mCount = 0;
            mSignature = sig;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                mSignature.update((byte) b);
            } catch (SignatureException e) {
                throw new IOException("SignatureException: " + e);
            }
            super.write(b);
            mCount++;
        }


//        @Override
//        public void write(byte[] b, int off, int len) throws IOException {
//            try {
//                mSignature.update(b, off, len);
//            } catch (SignatureException e) {
//                throw new IOException("SignatureException: " + e);
//            }
//            super.write(b, off, len);
//            mCount += len;
//        }

        public int size() {
            return mCount;
        }
    }

    /**
     * Write a .SF file with a digest of the specified manifest.
     */
    private byte[] writeSignatureFile(Manifest manifest, OutputStream out)
            throws Exception {
        Manifest sf = new Manifest();
        Attributes main = sf.getMainAttributes();
        main.putValue("Signature-Version", "1.0");
        main.putValue("Created-By", "1.0 (Android SignApk)");

        MessageDigest md = MessageDigest.getInstance("SHA1");
        PrintStream print = new PrintStream(
                new DigestOutputStream(new ByteArrayOutputStream(), md),
                true, "UTF-8");

        // Digest of the entire manifest
        manifest.write(print);
        print.flush();
        main.putValue("SHA1-Digest-Manifest", new String(Base64.encodeBase64(md.digest()), "ASCII"));

        Map<String, Attributes> entries = manifest.getEntries();
        for (Map.Entry<String, Attributes> entry : entries.entrySet()) {
            // Digest of the manifest stanza for this entry.
            print.print("Name: " + entry.getKey() + "\r\n");
            for (Map.Entry<Object, Object> att : entry.getValue().entrySet()) {
                print.print(att.getKey() + ": " + att.getValue() + "\r\n");
            }
            print.print("\r\n");
            print.flush();

            Attributes sfAttr = new Attributes();
            sfAttr.putValue("SHA1-Digest-Manifest",
                    new String(Base64.encodeBase64(md.digest()), "ASCII"));
            sf.getEntries().put(entry.getKey(), sfAttr);
        }
        Signature signature = instanceSignature();
        CountOutputStream cout = new CountOutputStream(out, signature);
        sf.write(cout);

        // A bug in the java.util.jar implementation of Android platforms
        // up to version 1.6 will cause a spurious IOException to be thrown
        // if the length of the signature file is a multiple of 1024 bytes.
        // As a workaround, add an extra CRLF in this case.
        if ((cout.size() % 1024) == 0) {
            cout.write('\r');
            cout.write('\n');
        }

        return signature.sign();
    }


    private Signature instanceSignature() throws Exception {
        byte[] data = dBase64(privateKey);
        KeyFactory rSAKeyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = rSAKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(data));
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        return signature;
    }

    /**
     * Copy all the files in a manifest from input to output.  We set
     * the modification times in the output to a fixed time, so as to
     * reduce variation in the output file and make incremental OTAs
     * more efficient.
     */
    private void copyFiles(Manifest manifest,
                           JarFile in, JarOutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int num;

        Map<String, Attributes> entries = manifest.getEntries();
        ArrayList<String> names = new ArrayList<String>(entries.keySet());
        Collections.sort(names);
        for (String name : names) {
            JarEntry inEntry = in.getJarEntry(name);
            JarEntry outEntry = null;
            if (inEntry.getMethod() == JarEntry.STORED) {
                // Preserve the STORED method of the input entry.
                outEntry = new JarEntry(inEntry);
            } else {
                // Create a new entry so that the compressed len is recomputed.
                outEntry = new JarEntry(name);
            }
            //outEntry.setTime(timestamp);
            out.putNextEntry(outEntry);

            InputStream data = in.getInputStream(inEntry);
            while ((num = data.read(buffer)) > 0) {
                out.write(buffer, 0, num);
            }
            out.flush();
        }
    }


    private void signFile(Manifest manifest, JarOutputStream outputJar)
            throws Exception {
        // Assume the certificate is valid for at least an hour.

        // MANIFEST.MF
        JarEntry je = new JarEntry(JarFile.MANIFEST_NAME);
        outputJar.putNextEntry(je);
        manifest.write(outputJar);

        // CERT.SF
        je = new JarEntry(CERT_SF_NAME);
        outputJar.putNextEntry(je);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] sign = writeSignatureFile(manifest, baos);
        byte[] signedData = baos.toByteArray();
        outputJar.write(signedData);

        // CERT.RSA
        je = new JarEntry(CERT_RSA_NAME);
        outputJar.putNextEntry(je);

        outputJar.write(dBase64(sigPrefix));

        //System.out.println("sigPrefix  --> \n" + HexDumpEncoder.encode(dBase64(sigPrefix)));

        //System.out.println("sign  --> \n" + HexDumpEncoder.encode(sign));

        //System.out.println("signFile -->> signedData  \n" + HexDumpEncoder.encode(signedData));
        outputJar.write(sign);
        outputJar.closeEntry();
    }

    public boolean sign(String inputFilename, String outputFilename){
        return sign(new File(inputFilename),outputFilename);
    }

    public boolean sign(File inputFile, String outputFilename) {
        JarFile inputJar = null;
        FileOutputStream outputFile = null;
        try {
            inputJar = new JarFile(inputFile, false);  // Don't verify.

            outputFile = new FileOutputStream(outputFilename);

            // Set the ZIP file timestamp to the starting valid time
            // of the 0th certificate plus one hour (to match what
            // we've historically done).

            JarOutputStream outputJar = new JarOutputStream(outputFile);

            // For signing .apks, use the maximum compression to make
            // them as small as possible (since they live forever on
            // the system partition).  For OTA packages, use the
            // default compression level, which is much much faster
            // and produces output that is only a tiny bit larger
            // (~0.1% on full OTA packages I tested).
            outputJar.setLevel(9);

            //SignApk signApk = new SignApk(Constants.privateKey, Constants.sigPrefix);

            Manifest manifest = addDigestsToManifest(inputJar);
            copyFiles(manifest, inputJar, outputJar);
            signFile(manifest, outputJar);
            outputJar.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputJar != null)
                    inputJar.close();
                if (outputFile != null)
                    outputFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static byte[] dBase64(String data) throws UnsupportedEncodingException {
        return Base64.decodeBase64(data.getBytes("UTF-8"));
    }


    public static boolean verifyJar(String jarName)
            throws Exception {
        boolean anySigned = false;
        JarFile jf = null;

        try {
            jf = new JarFile(jarName, true);
            Vector<JarEntry> entriesVec = new Vector<JarEntry>();
            byte[] buffer = new byte[8192];

            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                entriesVec.addElement(je);
                InputStream is = null;
                try {
                    is = jf.getInputStream(je);
                    int n;
                    while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                        // we just read. this will throw a SecurityException
                        // if  a signature/digest check fails.
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }

            Manifest man = jf.getManifest();

            if (man != null) {
                Enumeration<JarEntry> e = entriesVec.elements();
                while (e.hasMoreElements()) {
                    JarEntry je = e.nextElement();
                    CodeSigner[] signers = je.getCodeSigners();
                    //boolean isSigned = (signers != null);
                    anySigned |= (signers != null);
                }
            }

            if (man == null){
                System.out.println("no manifest.");
                return false;
            }

            if (anySigned) {
                System.out.println("jar verified.");
            } else {
                System.out.println("jar is unsigned. (signatures missing or not parsable)");
            }
            return anySigned;
        } catch (Exception e) {
            System.out.println("jarsigner: " + e);
        } finally {
            if (jf != null) {
                jf.close();
            }
        }
        return false;
    }

}
