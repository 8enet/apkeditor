package com.zzzmode.apkeditor;

import com.zzzmode.apkeditor.apksigner.*;
import com.zzzmode.apkeditor.axmleditor.decode.*;

import java.io.*;

/**
 * Created by zl on 15/10/30.
 */
public class ApkManifestPrinter {

    private static final String WORK_DIR;
    static {
        String dir=null;
        try {
            dir = File.createTempFile(ApkManifestPrinter.class.getName(),null).getParentFile()+"/apkeditor_work";
        }catch (Throwable e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }finally {
            WORK_DIR = dir;
        }

    }
    private static final String A_XML = WORK_DIR + "/AndroidManifest.xml";

    public static void main(String args[]){
        File file=new File(args[0]);
        if(!file.exists()){
            System.out.println("file "+args[0]+"     not fount !!");
            System.exit(0);
        }

        try {
            ZipManager.extraZipEntry(file, new String[]{"AndroidManifest.xml"}, new String[]{A_XML});
            File newXML=new File(A_XML);
            AXMLDoc doc = new AXMLDoc();
            doc.parse(new FileInputStream(newXML));
            doc.print();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
