package com.zzzmode.apkeditor;

import com.zzzmode.apkeditor.apksigner.*;
import com.zzzmode.apkeditor.axmleditor.decode.*;
import com.zzzmode.apkeditor.axmleditor.editor.*;
import com.zzzmode.apkeditor.utils.*;
import org.junit.*;

import java.io.*;

/**
 * Created by zl on 15/11/17.
 */
public class AxmlTest {

    @Test
    public void axmlReaderTest() throws Exception {
        File apkFile=TestUtils.getResourceFile("tap_unsign.apk");

        File axmlFile=File.createTempFile("AndroidManifest", ".xml",null);
        ZipManager.extraZipEntry(apkFile, new String[]{"AndroidManifest.xml"}, new String[]{axmlFile.getAbsolutePath()});
        AXMLDoc doc = new AXMLDoc();
        doc.parse(FileUtils.openInputStream(axmlFile));
        doc.print();
    }

    @Test
    public void axmlEditTest()throws Exception{
        File apkFile=TestUtils.getResourceFile("tap_unsign.apk");
        File axmlFile=File.createTempFile("AndroidManifest", ".xml",null);
        ZipManager.extraZipEntry(apkFile, new String[]{"AndroidManifest.xml"}, new String[]{axmlFile.getAbsolutePath()});
        AXMLDoc doc = new AXMLDoc();
        doc.parse(FileUtils.openInputStream(axmlFile));

        ApplicationInfoEditor applicationInfoEditor = new ApplicationInfoEditor(doc);
        applicationInfoEditor.setEditorInfo(new ApplicationInfoEditor.EditorInfo("testAppName", false));
        applicationInfoEditor.commit();

        doc.build(new FileOutputStream(axmlFile));
        doc.print();
        doc.release();
    }
}
