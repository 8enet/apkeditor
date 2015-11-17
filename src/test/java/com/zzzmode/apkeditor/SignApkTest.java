package com.zzzmode.apkeditor;

import com.zzzmode.apkeditor.apksigner.*;
import org.junit.*;

import java.io.*;

/**
 * Created by zl on 15/11/17.
 */
public class SignApkTest {

    @Test
    public void signApkTest() throws Exception {
        File outFile = File.createTempFile("tap_sign", ".apk",null);

        SignApk signApk = new SignApk(KeyHelper.privateKey,KeyHelper.sigPrefix);
        signApk.sign(TestUtils.getResourceFile("tap_unsign.apk"),outFile.getAbsolutePath());

        assert SignApk.verifyJar(outFile.getAbsolutePath());
        outFile.delete();
    }

}
