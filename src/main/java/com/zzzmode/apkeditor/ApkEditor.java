package com.zzzmode.apkeditor;


import com.zzzmode.apkeditor.apksigner.SignApk;
import com.zzzmode.apkeditor.apksigner.ZipManager;
import com.zzzmode.apkeditor.axmleditor.editor.MetaDataEditor;
import com.zzzmode.apkeditor.axmleditor.editor.PackageInfoEditor;
import com.zzzmode.apkeditor.axmleditor.editor.PermissionEditor;
import com.zzzmode.apkeditor.axmleditor.utils.NodeValue;
import com.zzzmode.apkeditor.utils.FileUtils;
import com.zzzmode.apkeditor.axmleditor.decode.AXMLDoc;
import com.zzzmode.apkeditor.axmleditor.editor.ApplicationInfoEditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by zl on 15/9/11.
 */
public class ApkEditor {

    private static final String WORK_DIR;
    static {
        String dir=null;
        try {
            dir = File.createTempFile(ApkEditor.class.getName(),null).getParentFile()+"/apkeditor_work";
        }catch (Throwable e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }finally {
            WORK_DIR = dir;
        }

    }
    private static final String A_XML = WORK_DIR + "/AndroidManifest.xml";

    public ApkEditor(String privateKey,String sigPrefix){
        File file=new File(WORK_DIR);
        if(!file.exists()){
            file.mkdirs();
        }
        this.privateKey=privateKey;
        this.sigPrefix = sigPrefix;
    }

    private String privateKey;
    private String sigPrefix;

    private String origFile;
    private String outFile;
    private String appName;
    private String appIcon;

    public void setOrigFile(String origFile) {
        this.origFile = origFile;
    }

    public void setOutFile(String outFile) {
        this.outFile = outFile;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAppIcon(String appIcon) {
        this.appIcon = appIcon;
    }


    public String getOrigFile() {
        return origFile;
    }

    public String getOutFile() {
        return outFile;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppIcon() {
        return appIcon;
    }

    public boolean create() throws Exception {
        File tmpFile=null;
        File newXML=null;
        try {

            File origAPK = new File(origFile);
            //复制文件
            tmpFile = new File(WORK_DIR+"/tmp.apk");
            newXML=new File(A_XML);

            FileUtils.copyFile(origAPK, tmpFile);
            //提取AndroidManifest.xml文件
            ZipManager.extraZipEntry(tmpFile, new String[]{"AndroidManifest.xml"}, new String[]{A_XML});

            if (appName != null) {
                //修改app name
                AXMLDoc doc = new AXMLDoc();
                doc.parse(new FileInputStream(newXML));


                ApplicationInfoEditor applicationInfoEditor = new ApplicationInfoEditor(doc);
                applicationInfoEditor.setEditorInfo(new ApplicationInfoEditor.EditorInfo(appName, false));
                applicationInfoEditor.commit();

                //更多修改可以在下面添加
                /*
                PackageInfoEditor packageInfoEditor = new PackageInfoEditor(doc);
                packageInfoEditor.setEditorInfo(new PackageInfoEditor.EditorInfo(12563, "abcde", null));
                packageInfoEditor.commit();


                PermissionEditor permissionEditor = new PermissionEditor(doc);
                permissionEditor.setEditorInfo(new PermissionEditor.EditorInfo()
                                .with(new PermissionEditor.PermissionOpera("android.permission.ACCESS_FINE_LOCATION").remove())
                                .with(new PermissionEditor.PermissionOpera("android.permission.WRITE_SETTINGS").remove())
                                .with(new PermissionEditor.PermissionOpera("android.permission.INTERNET").add())
                );
                permissionEditor.commit();

                MetaDataEditor metaDataEditor = new MetaDataEditor(doc);
                metaDataEditor.setEditorInfo(new MetaDataEditor.EditorInfo("UMENG_CHANNEL", "apkeditor"));
                metaDataEditor.commit();
                */

                doc.build(new FileOutputStream(newXML));
                doc.release();

            }
            // 替换apk 中的文件
            ZipManager.replaceZipEntry(tmpFile, new String[]{"AndroidManifest.xml", "res/drawable/ic_launcher.png"},
                    new String[]{A_XML, appIcon});

            //重新签名
            SignApk signApk = new SignApk(privateKey,sigPrefix);

            return signApk.sign(tmpFile.getAbsolutePath(), outFile);
        } catch (Exception e) {
            throw e;
        } finally {
            if(tmpFile!= null){
                tmpFile.delete();
            }
            if(newXML != null){
                newXML.delete();
            }
        }
    }

    public static File getWorkDir(){
        return new File(WORK_DIR);
    }

}
