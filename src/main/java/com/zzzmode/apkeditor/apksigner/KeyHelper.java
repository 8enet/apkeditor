package com.zzzmode.apkeditor.apksigner;


import com.zzzmode.apkeditor.utils.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Key;
import java.security.KeyStore;

/**
 * 用于生成 PrivateKey 和 SigPrefix
 * 用法，先用keytool生成keystore，在用这个keystore签名任意一个apk，
 * 解压出这个apk的META-INF/CERT.RSA文件
 * 复制这生成的keystore文件和CERT.RSA到src/resources目录
 */
public final class KeyHelper {

    /**
     * 生成keystore 命令
     *  keytool -genkey -alias mytestkey -keyalg RSA  -keysize 512 -validity 40000 -keystore demo.keystore
     *
     *   alias mytestkey
     *   pwd 123456
     *
     * 签名apk
     *  jarsigner -verbose -keystore demo.keystore  -digestalg SHA1 -sigalg sha1withrsa -signedjar signed.apk unsign.apk mytestkey
     *
     * 验证apk是否签名成功
     *  jarsigner -verify ~/sign.apk
     */


    public static void main(String[] args) {
        try {
            getPrivateKey();
            getSigPrefix();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 读取 PrivateKey
     * @throws Exception
     */
    public static void getPrivateKey() throws Exception {

        String keystoreFileName = "demo.keystore"; //resources 目录下的keystore文件
        String keystorePassword = "123456";
        String alias = "mytestkey";
        String keyPassword = "123456";

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(ClassLoader.getSystemClassLoader().getResourceAsStream(keystoreFileName), keystorePassword.toCharArray());
        Key key = keystore.getKey(alias, keyPassword.toCharArray());
        String string = new String(Base64.encodeBase64(key.getEncoded()), "UTF-8");
        System.out.println("PrivateKey " + string);
    }

    /**
     * 签名前缀
     * 首先用上面生成的keystore签名任意一个apk，解压出这个apk里面 META-INF/CERT.RSA 的文件
     * @throws IOException
     */
    private static void getSigPrefix() throws IOException, URISyntaxException {
        System.out.println("----------");
        String rsaFileName="CERT.RSA";
        File file = new File(ClassLoader.getSystemClassLoader().getResource(rsaFileName).toURI());
        FileInputStream fis = new FileInputStream(file);

        /**
         * RSA-keysize signature-length
         # 512         64
         # 1024        128
         # 2048        256
         */

        int same = (int) (file.length() - 64);  //当前-keysize 512

        byte[] buff = new byte[same];
        fis.read(buff, 0, same);
        fis.close();

        String string = new String(Base64.encodeBase64(buff), "UTF-8");
        System.out.println("sigPrefix  -->>  " + string);


    }


    public static String privateKey = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAi3iIMEMo0ogaeXFEtooya6n+FZqxyRWpiKD9jg/LE5CxdTy7RlQesmfQ+uVJvMlF4ebpOhQp+aIHOp+UxZAUfQIDAQABAkAMt7jzbaxTRkXjvQhe/MsMNjwNDEYZ5/fFlaiJQ7do2S5dtCZQ966Vb1dLZlVWjPWnR99YiCYxd5qOenyTujgBAiEAwMDm9zEFN/YkFewdi0/4bW0OONlCJWCHqkN0poLIJsECIQC5O/AnP4vr/92+tcdemfROgfmlvK/NWnuEzSRJ1uF4vQIhALTgkBxo0MfZ37T+tB619Z7h1pW8MlkWw1ggIsfaM+5BAiBHSllAUcXBW6V1S7LipvAO8xko/3jN2SAm2Wk4/fmjJQIgEKdiL/87EQkL3unusUZgLyonz7d7FjHonUARloYZboU=";
    public static String sigPrefix = "MIICwwYJKoZIhvcNAQcCoIICtDCCArACAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3DQEHAaCCAckwggHFMIIBb6ADAgECAgQTmjt5MA0GCSqGSIb3DQEBCwUAMFcxCzAJBgNVBAYTAmNuMREwDwYDVQQIEwhzaGFuZ2hhaTERMA8GA1UEBxMIc2hhbmdoYWkxCjAIBgNVBAoTAXoxCjAIBgNVBAsTAXoxCjAIBgNVBAMTAXowIBcNMTUwOTIyMTIyMDUxWhgPMjEyNTAzMjkxMjIwNTFaMFcxCzAJBgNVBAYTAmNuMREwDwYDVQQIEwhzaGFuZ2hhaTERMA8GA1UEBxMIc2hhbmdoYWkxCjAIBgNVBAoTAXoxCjAIBgNVBAsTAXoxCjAIBgNVBAMTAXowXDANBgkqhkiG9w0BAQEFAANLADBIAkEAi3iIMEMo0ogaeXFEtooya6n+FZqxyRWpiKD9jg/LE5CxdTy7RlQesmfQ+uVJvMlF4ebpOhQp+aIHOp+UxZAUfQIDAQABoyEwHzAdBgNVHQ4EFgQUnjtnyFJuunyPb+Ez8F3wuJUxqCgwDQYJKoZIhvcNAQELBQADQQBHWscfuo5oEsCkeva0xg6Ub7qOfOUDqr1R3HJ4x5M17fN2GbBK7j0Wu7aRtabNHnEhGQNvVhLTWzrXpxQj+1/mMYHDMIHAAgEBMF8wVzELMAkGA1UEBhMCY24xETAPBgNVBAgTCHNoYW5naGFpMREwDwYDVQQHEwhzaGFuZ2hhaTEKMAgGA1UEChMBejEKMAgGA1UECxMBejEKMAgGA1UEAxMBegIEE5o7eTAJBgUrDgMCGgUAMA0GCSqGSIb3DQEBAQUABEA=";


}