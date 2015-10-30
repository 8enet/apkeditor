package com.zzzmode.apkeditor.axmleditor.asrc;

import com.zzzmode.apkeditor.utils.*;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ApkResourceFinder {
    private final long HEADER_START = 0;
    static short RES_STRING_POOL_TYPE = 0x0001;
    static short RES_TABLE_TYPE = 0x0002;
    static short RES_TABLE_PACKAGE_TYPE = 0x0200;
    static short RES_TABLE_TYPE_TYPE = 0x0201;
    static short RES_TABLE_TYPE_SPEC_TYPE = 0x0202;

    String[] valueStringPool = null;
    String[] typeStringPool = null;
    String[] keyStringPool = null;

    private int package_id = 0;
    private List<String> resIdList;

    //// Contains no data.
    //static byte TYPE_NULL = 0x00;
    //// The 'data' holds an attribute resource identifier.
    //static byte TYPE_ATTRIBUTE = 0x02;
    //// The 'data' holds a single-precision floating point number.
    //static byte TYPE_FLOAT = 0x04;
    //// The 'data' holds a complex number encoding a dimension value,
    //// such as "100in".
    //static byte TYPE_DIMENSION = 0x05;
    //// The 'data' holds a complex number encoding a fraction of a
    //// container.
    //static byte TYPE_FRACTION = 0x06;
    //// The 'data' is a raw integer value of the form n..n.
    //static byte TYPE_INT_DEC = 0x10;
    //// The 'data' is a raw integer value of the form 0xn..n.
    //static byte TYPE_INT_HEX = 0x11;
    //// The 'data' is either 0 or 1, for input "false" or "true" respectively.
    //static byte TYPE_INT_BOOLEAN = 0x12;
    //// The 'data' is a raw integer value of the form #aarrggbb.
    //static byte TYPE_INT_COLOR_ARGB8 = 0x1c;
    //// The 'data' is a raw integer value of the form #rrggbb.
    //static byte TYPE_INT_COLOR_RGB8 = 0x1d;
    //// The 'data' is a raw integer value of the form #argb.
    //static byte TYPE_INT_COLOR_ARGB4 = 0x1e;
    //// The 'data' is a raw integer value of the form #rgb.
    //static byte TYPE_INT_COLOR_RGB4 = 0x1f;

    // The 'data' holds a ResTable_ref, a reference to another resource
    // table entry.
    static byte TYPE_REFERENCE = 0x01;
    // The 'data' holds an index into the containing resource table's
    // global value string pool.
    static byte TYPE_STRING = 0x03;


    private Map<String, List<String>> responseMap;

    Map<Integer, List<String>> entryMap = new HashMap<Integer, List<String>>();

    public Map<String, List<String>> initialize(File f) throws Exception{
        byte[] data =new byte[(int)f.length()];
        final FileInputStream fis = FileUtils.openInputStream(f);
        fis.read(data);
        fis.close();
        return this.processResourceTable(data, new ArrayList<String>());
    }

    public Map<String, List<String>> processResourceTable(byte[] data, List<String> resIdList) throws Exception{
        this.resIdList = resIdList;

        responseMap = new HashMap<String, List<String>>();
        int lastPosition;


        final ByteBuffer br = ByteBuffer.wrap(data);

        short type = br.getShort();
        int headerSize = br.getInt();
        int size = br.getInt();
        int packageCount = br.getInt();

        System.out.println(type+"    "+headerSize+"     "+size+"     "+RES_TABLE_TYPE);

        System.out.println(Integer.toHexString(type));

        if (type != RES_TABLE_TYPE) {
            throw new RuntimeException("No RES_TABLE_TYPE found!");
        }
        if (size != data.length) {
            throw new RuntimeException("The buffer size not matches to the resource table size.");
        }

        int realStringPoolCount = 0;
        int realPackageCount = 0;


        while (true) {
            int pos = br.position();
            short t = br.getShort();
            short hs = br.getShort();
            int s = br.getInt();

            if (t == RES_STRING_POOL_TYPE) {
                if (realStringPoolCount == 0) {
                    // Only the first string pool is processed.
                    System.out.println("Processing the string pool ...");


                    byte[] buffer = new byte[s];
                    lastPosition = br.position();

                    br.position(pos);

                    //br.BaseStream.Seek(pos, SeekOrigin.Begin);
                    //buffer = br.ReadBytes(s);
                    br.get(buffer);

                    //br.BaseStream.Seek(lastPosition, SeekOrigin.Begin);

                    br.position(lastPosition);

                    valueStringPool = processStringPool(buffer);

                }
                realStringPoolCount++;

            } else if (t == RES_TABLE_PACKAGE_TYPE) {
                // Process the package
                System.out.println("Processing package " + realPackageCount + " ...");

                byte[] buffer = new byte[s];
                lastPosition = br.position();

                br.position(pos);
                br.get(buffer);
                //br.BaseStream.Seek(pos, SeekOrigin.Begin);
                //buffer = br.ReadBytes(s);
                //br.BaseStream.Seek(lastPosition, SeekOrigin.Begin);

                br.position(lastPosition);
                processPackage(buffer);

                realPackageCount++;

            } else {
                throw new RuntimeException("Unsupported Type");
            }

            br.position(pos+s);

            //br.BaseStream.Seek(pos + (long) s, SeekOrigin.Begin);
            if (br.position() == data.length)
                break;

        }

        if (realStringPoolCount != 1) {
            throw new RuntimeException("More than 1 string pool found!");
        }
        if (realPackageCount != packageCount) {
            throw new RuntimeException(
                    "Real package count not equals the declared count.");
        }

        return responseMap;


    }

    private void processPackage(byte[] data) {
        int lastPosition = 0;

        final ByteBuffer br = ByteBuffer.wrap(data);

        //HEADER
        short type = br.getShort();
        short headerSize = br.getShort();
        int size = br.getInt();

        int id = br.getInt();
        package_id = id;

        //PackageName
        char[] name = new char[256];
        for (int i = 0; i < 256; ++i) {
            name[i] = br.getChar();
        }
        int typeStrings = br.getInt();
        int lastPublicType = br.getInt();
        int keyStrings = br.getInt();
        int lastPublicKey = br.getInt();

        if (typeStrings != headerSize) {
            throw new RuntimeException("TypeStrings must immediately follow the package structure header.");
        }

        System.out.println("Type strings:");
        lastPosition = br.position();
        br.position(typeStrings);

        //br.BaseStream.Seek(typeStrings, SeekOrigin.Begin);
        byte[] bbTypeStrings = new byte[data.length-typeStrings];
        br.get(bbTypeStrings);
        //br.ReadBytes((int) (br.BaseStream.Length - br.BaseStream.Position));

        //br.BaseStream.Seek(lastPosition, SeekOrigin.Begin);
        br.position(lastPosition); //还原

        typeStringPool = processStringPool(bbTypeStrings);

        System.out.println("Key strings:");

        //br.BaseStream.Seek(keyStrings, SeekOrigin.Begin);

        br.position(keyStrings);


        short key_type = br.getShort();
        short key_headerSize = br.getShort();
        int key_size = br.getInt();

        lastPosition = br.position();
        //br.BaseStream.Seek(keyStrings, SeekOrigin.Begin);

        br.position(keyStrings);


        byte[] bbKeyStrings = new byte[data.length-br.position()];
        br.get(bbKeyStrings);

        //br.ReadBytes((int) (br.BaseStream.Length - br.BaseStream.Position));
        //br.BaseStream.Seek(lastPosition, SeekOrigin.Begin);
        br.position(lastPosition);

        keyStringPool = processStringPool(bbKeyStrings);


        // Iterate through all chunks
        //
        int typeSpecCount = 0;
        int typeCount = 0;

        //br.BaseStream.Seek((keyStrings + key_size), SeekOrigin.Begin);

        br.position(keyStrings+key_size);

        while (true) {
            int pos =br.position();
            short t = br.getShort();
            short hs = br.getShort();
            int s = br.getInt();

            if (t == RES_TABLE_TYPE_SPEC_TYPE) {
                // Process the string pool
                byte[] buffer = new byte[s];
                lastPosition=br.position();
                //br.BaseStream.Seek(pos, SeekOrigin.Begin);
                br.position(pos);
                //buffer = br.ReadBytes(s);
                br.get(buffer);

                br.position(lastPosition);
                processTypeSpec(buffer);

                typeSpecCount++;
            } else if (t == RES_TABLE_TYPE_TYPE) {
                // Process the package
                byte[] buffer = new byte[s];
                //br.BaseStream.Seek(pos, SeekOrigin.Begin);
                lastPosition=br.position();

                br.position(pos);
                br.get(buffer);

                br.position(lastPosition);
                processType(buffer);

                typeCount++;
            }

            //br.BaseStream.Seek(pos + s, SeekOrigin.Begin);
            br.position(pos+s);
            if (br.position() == data.length)
                break;
        }


    }

    private void putIntoMap(String resId, String value) {
        List<String> valueList = null;
        if (responseMap.containsKey(resId.toUpperCase()))
            valueList = responseMap.get(resId.toUpperCase());
        if (valueList == null) {
            valueList = new ArrayList<String>();
        }
        valueList.add(value);
        if (responseMap.containsKey(resId.toUpperCase()))
            responseMap.put(resId.toUpperCase(), valueList);
        else
            responseMap.put(resId.toUpperCase(), valueList);

    }

    private void processType(byte[] typeData) {
        final ByteBuffer br = ByteBuffer.wrap(typeData);


        int lastPosition=0;
        short type = br.getShort();
        short headerSize = br.getShort();
        int size = br.getInt();
        byte id = br.get();
        byte res0 = br.get();
        short res1 = br.getShort();
        int entryCount = br.getInt();
        int entriesStart = br.getInt();

        Map<String, Integer> refKeys = new HashMap<String, Integer>();

        int config_size = br.getInt();

        // Skip the config data
        //br.BaseStream.Seek(headerSize, SeekOrigin.Begin);

        br.position(headerSize);

        if (headerSize + entryCount * 4 != entriesStart) {
            throw new RuntimeException("HeaderSize, entryCount and entriesStart are not valid.");
        }

        // Start to get entry indices
        int[] entryIndices = new int[entryCount];
        for (int i = 0; i < entryCount; ++i) {
            entryIndices[i] = br.getInt();
        }

        br.position(lastPosition);

        // Get entries
        for (int i = 0; i < entryCount; ++i) {
            if (entryIndices[i] == -1)
                continue;

            int resource_id = (package_id << 24) | (id << 16) | i;

            long pos = br.position();
            short entry_size = br.getShort();
            short entry_flag = br.getShort();
            int entry_key = br.getInt();

            // Get the value (simple) or map (complex)
            int FLAG_COMPLEX = 0x0001;

            if ((entry_flag & FLAG_COMPLEX) == 0) {
                // Simple case
                short value_size = br.getShort();
                byte value_res0 = br.get();
                byte value_dataType = br.get();
                int value_data = br.getInt();

                String idStr = Integer.toHexString(resource_id);
                String keyStr = keyStringPool[entry_key];
                String data = null;

                System.out.println("Entry 0x" + idStr + ", key: " + keyStr + ", simple value type: ");

                List<String> entryArr = null;
                if (entryMap.containsKey(toHex(idStr)))
                    entryArr = entryMap.get(toHex(idStr));

                if (entryArr == null)
                    entryArr = new ArrayList<String>();

                entryArr.add(keyStr);
                if (entryMap.containsKey(toHex(idStr)))
                    entryMap.put(toHex(idStr), entryArr);
                else
                    entryMap.put(toHex(idStr), entryArr);

                if (value_dataType == TYPE_STRING) {
                    data = valueStringPool[value_data];
                    System.out.println(", data: " + valueStringPool[value_data] + "");
                } else if (value_dataType == TYPE_REFERENCE) {
                    String hexIndex = Integer.toHexString(value_data);
                    refKeys.put(idStr, value_data);
                } else {
                    data = value_data+"";
                    System.out.println(", data: " + value_data + "");
                }

                // if (inReqList("@" + idStr)) {
                putIntoMap("@" + idStr, data);
            } else {
                int entry_parent = br.getInt();
                int entry_count = br.getInt();

                for (int j = 0; j < entry_count; ++j) {
                    int ref_name = br.getInt();
                    short value_size = br.getShort();
                    byte value_res0 = br.get();
                    byte value_dataType = br.get();
                    int value_data = br.getInt();
                }

                System.out.println("Entry 0x"
                        + Integer.toHexString(resource_id) + ", key: "
                        + keyStringPool[entry_key]
                        + ", complex value, not printed.");
            }

        }
        HashSet<String> refKs = new HashSet<String>(refKeys.keySet());

        for (String refK : refKs) {
            List<String> values = null;
            if (responseMap.containsKey("@" + Integer.toHexString(refKeys.get(refK)).toUpperCase()))
                values = responseMap.get("@" + Integer.toHexString(refKeys.get(refK)).toUpperCase());

            if (values != null)
                for (String value : values) {
                    putIntoMap("@" + refK, value);
                }
        }

    }


    private String[] processStringPool(byte[] data) {
        int lastPosition = 0;

        final ByteBuffer br = ByteBuffer.wrap(data);

        short type = br.getShort();
        short headerSize = br.getShort();
        int size = br.getInt();
        int stringCount = br.getInt();
        int styleCount = br.getInt();
        int flags = br.getInt();
        int stringsStart = br.getInt();
        int stylesStart = br.getInt();

        boolean isUTF_8 = (flags & 256) != 0;

        int[] offsets = new int[stringCount];
        for (int i = 0; i < stringCount; ++i) {
            offsets[i] = br.getInt();
        }
        String[] strings = new String[stringCount];

        for (int i = 0; i < stringCount; i++) {
            int pos = stringsStart + offsets[i];
            lastPosition=br.position();
            //br.BaseStream.Seek(pos, SeekOrigin.Begin);
            br.position(pos);

            strings[i] = "";
            if (isUTF_8) {
                int u16len = br.get(); // u16len
                if ((u16len & 0x80) != 0) {// larger than 128
                    u16len = ((u16len & 0x7F) << 8) + br.get();
                }

                int u8len = br.get(); // u8len
                if ((u8len & 0x80) != 0) {// larger than 128
                    u8len = ((u8len & 0x7F) << 8) + br.get();
                }

                if (u8len > 0) {
                    byte[] b=new byte[u8len];
                    br.get(b);
                    strings[i] =new String(b,StandardCharsets.UTF_8);
                    //strings[i] = Encoding.UTF8.GetString(br.ReadBytes(u8len));
                }
                else
                    strings[i] = "";
            }
//            else // UTF_16
//            {
//                int u16len = br.ReadUInt16();
//                if ((u16len & 0x8000) != 0) {// larger than 32768
//                    u16len = ((u16len & 0x7FFF) << 16) + br.ReadUInt16();
//                }
//
//                if (u16len > 0) {
//                    strings[i] = Encoding.Unicode.GetString(br.ReadBytes(u16len * 2));
//                }
//            }
            br.position(lastPosition);
            System.out.println("Parsed value: " + strings[i]);
        }
        return strings;
    }

    private void processTypeSpec(byte[] data) {

        final ByteBuffer br = ByteBuffer.wrap(data);

        short type = br.getShort();
        short headerSize = br.getShort();
        int size = br.getInt();
        byte id = br.get();
        byte res0 = br.get();
        short res1 = br.getShort();
        int entryCount = br.getInt();


        System.out.println("Processing type spec " + typeStringPool[id - 1]);

        int[] flags = new int[entryCount];
        for (int i = 0; i < entryCount; ++i) {
            flags[i] = br.getInt();
        }

    }


    private static Integer toHex(String s){
        return Integer.valueOf(s, 16);
    }

}