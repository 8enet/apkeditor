package com.zzzmode.apkeditor;

import java.io.*;

/**
 * Created by zl on 15/11/17.
 */
public class TestUtils {

    public static File getResourceFile(String resourceName) throws Exception {
        return new File(ClassLoader.getSystemClassLoader().getResource(resourceName).toURI());
    }
}
