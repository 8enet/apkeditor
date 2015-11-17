package com.zzzmode.apkeditor.axmleditor.asrc;

import junit.framework.*;
import org.junit.*;
import org.junit.Test;

import java.io.*;
import java.util.*;

/**
 * Created by zl on 15/11/17.
 */
public class AsrcDecoderTest{



    @Test
    public void testArscDecoder() throws Exception {
        File asrcFile=new File("/Users/zl/develop/github/apkeditor/src/test/resources/resources.arsc");

        FileInputStream file=new FileInputStream(asrcFile);
        ARSCDecoder arsc=ARSCDecoder.read(file);
        StringBlock sb=arsc.mTableStrings;
        List<String> list=new ArrayList<String>();
        sb.getStrings(list);
        for(int i=0;i<list.size();i++)
            System.out.println(i+" "+list.get(i));

    }

    @Test
    public void testArscEncoder()throws Exception{
        File asrcFile=new File(getClass().getResource("/resources.arsc").toURI());
        FileInputStream file=new FileInputStream(asrcFile);
        ARSCDecoder arsc=ARSCDecoder.read(file);
        StringBlock sb=arsc.mTableStrings;
        List<String> list=new ArrayList<String>();
        sb.getStrings(list);
        for(int i=0;i<list.size();i++){
            System.out.println(i+" "+list.get(i));
//            if(i == 21){
//                list.set(21,"device info");
//            }
        }


        ByteArrayOutputStream out=new ByteArrayOutputStream();
        arsc.write(list,new LEDataOutputStream(out));
        FileOutputStream outFile=new FileOutputStream("/Users/zl/develop/github/apkeditor/src/test/resources/resources_ed.arsc");
        outFile.write(out.toByteArray());
        outFile.close();
    }

}
