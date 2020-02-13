package com.example.rot.uitotem;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.inputmethod.InputMethodSubtype;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class StoreAPI {
    private MainActivity mainActivity;
    private SharedPreferences classIDView;
    private SharedPreferences.Editor classIDEditor;
    private SharedPreferences classStructView;
    private SharedPreferences.Editor classStructEditor;
    private int m_blockNum,m_blockOffset;
    private String m_className;
    private ArrayList<String> buffer;
    private int mode_none_null,is_dirty;

    //the number of tuples in a PAGE;
    public static final int PAGESIZE = 32;

    /*-----------------------CONSTRUCTION-----------------------*/
    StoreAPI(MainActivity _mainActivity){
        mainActivity = _mainActivity;
        classIDView = mainActivity.getSharedPreferences("classID",Context.MODE_PRIVATE);
        classIDEditor = mainActivity.getSharedPreferences("classID",Context.MODE_PRIVATE).edit();
        classStructView = mainActivity.getSharedPreferences("classStruct",Context.MODE_PRIVATE);
        classStructEditor = mainActivity.getSharedPreferences("classStruct",Context.MODE_PRIVATE).edit();

        buffer = null;
    }
    private void clearData(){
        classIDEditor.clear();classIDEditor.commit();
        classStructEditor.clear();classStructEditor.commit();
    }

    @Override
    protected void finalize() throws Throwable {
        flushToDisk();
        super.finalize();
    }

    /*-----------------------REG Class-----------------------*/
    public boolean existClass(String className){
        String rs = classIDView.getString(className,"");
        return !rs.equals("");
    }

   private boolean regClass(String className){
        String rs = classIDView.getString(className,"");
        if(rs.equals("")){
            classIDEditor.putString(className,className);
            classIDEditor.commit();
            return true;
        }
        return false;
    }

    private boolean delClass(String className){
        String rs = classIDView.getString(className,"");
        if(rs.equals(""))return false;
        classIDEditor.remove(className);
        classIDEditor.commit();
        return true;
    }

    /*-----------------------Coding-----------------------*/
    public String encode(ArrayList<String> data){
        if(data==null)return "";
        int len = data.size();
        String rs = "";
        for(int i=0;i<len;i++){
            String tmp = data.get(i);
            int n = 0;
            if(tmp!=null)n = tmp.length();
            for(int j=0;j<n;j++){
                if(tmp.charAt(j)==';'){
                    rs += "!;";
                }else if(tmp.charAt(j)=='!'){
                    rs += "!!";
                }else{
                    rs += tmp.charAt(j);
                }
            }
            rs += ";";
        }
        return rs;
    }

    public ArrayList<String> decode(String code){
        try{
            ArrayList<String> rs = new ArrayList<>();
            int n = code.length();
            for(int i=0;i<n;i++){
                String tmp = "";
                for(;code.charAt(i)!=';';i++){
                    if(code.charAt(i)=='!'){
                        i++;
                    }
                    tmp += code.charAt(i);
                }
                rs.add(tmp);
            }
            return rs;
        }catch (Exception e){
            return new ArrayList<>();
        }
    }

    /*-----------------------FILE I/O-----------------------*/
    public boolean writeString(String fileName,String data){
        try {
            FileOutputStream out = mainActivity.openFileOutput(fileName,Context.MODE_PRIVATE);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(data);
            writer.flush();
            writer.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String readString(String fileName){
        try {
            FileInputStream ins = mainActivity.openFileInput(fileName);
            byte[] buf = new byte[8096];
            StringBuilder sb = new StringBuilder("");
            int len = 0;
            while((len = ins.read(buf))>0){
                sb.append(new String(buf,0,len));
            }
            ins.close();
            return sb.toString();
        } catch (Exception e){
            return "";
        }
    }

    /*--------------------Create & Drop & Parse Class-----------------------*/
    private void saveClassStruct(String className,ClassStruct classStruct){
        Log.d("Hello", "saveClassStruct: ");
        /*className--selectClassName--tupleNum--children--Attr */
        int n;
        ArrayList<String> data = new ArrayList<>();

        data.add(className);
        if(classStruct.selectClassName==null)classStruct.selectClassName="";
        data.add(classStruct.selectClassName);
        data.add(Integer.toString(classStruct.tupleNum));

        if(classStruct.children!=null){
            n = classStruct.children.size();
        }else{
            n = 0;
        }
        data.add(Integer.toString(n));
        for(int i=0;i<n;i++){
            data.add(classStruct.children.get(i));
        }

        if(classStruct.attrList != null){
            n = classStruct.attrList.size();
        }else{
            n = 0;
        }
        data.add(Integer.toString(n));
        for(int i=0;i<n;i++){
            data.add(classStruct.attrList.get(i).attrName);
            data.add(Integer.toString(classStruct.attrList.get(i).attrType));
            data.add(Integer.toString(classStruct.attrList.get(i).attrSize));
            data.add(classStruct.attrList.get(i).defaultVal);
        }
        data.add(classStruct.condition);
        n = 0;
        if(classStruct.virtualAttr!=null)n=classStruct.virtualAttr.size();
        data.add(Integer.toString(n));
        for(int i=0;i<n;i++){
            data.add(classStruct.virtualAttr.get(i).attrName);
            data.add(classStruct.virtualAttr.get(i).attrRename);
        }
        String code = encode(data);
        /*coding over*/
        //save code
        classStructEditor.putString(className,code);
        classStructEditor.commit();
    }

    public ClassStruct getClassStruct(String className){
        if(existClass(className)==false)return null;
        int n,index = 0;

        String code = classStructView.getString(className,"");
        ArrayList<String> data = decode(code);

        ClassStruct classStruct = new ClassStruct();
        classStruct.className = data.get(index++);
        classStruct.selectClassName = data.get(index++);
        classStruct.tupleNum = Integer.parseInt(data.get(index++));

        n = Integer.parseInt(data.get(index++));
        if(n==0)classStruct.children=new ArrayList<>();
        else{
            classStruct.children = new ArrayList<>();
            for(int i=0;i<n;i++){
                classStruct.children.add(data.get(index++));
            }
        }
        n = Integer.parseInt(data.get(index++));
        classStruct.attrList=new ArrayList<>();
        for(int i=0;i<n;i+=1){
            Attribute attr = new Attribute(data.get(index++),
                    Integer.parseInt(data.get(index++)),
                    Integer.parseInt(data.get(index++)),
                    data.get(index++));
            classStruct.attrList.add(attr);
        }
        classStruct.condition = data.get(index++);
        classStruct.virtualAttr = new ArrayList<>();
        n = Integer.parseInt(data.get(index++));
        for(int i=0;i<n;i++){
            classStruct.virtualAttr.add(new AttrNameTuple(data.get(index++),data.get(index++)));
        }
        return classStruct;
    }

    public void setClassStruct(String className,ClassStruct classStruct){
        saveClassStruct(className,classStruct);
    }

    public boolean createClass(String className,ClassStruct classStruct){
        if(regClass(className)==false)return false;
        classStruct.tupleNum = 0;
        classStruct.className = className;
        saveClassStruct(className,classStruct);
        writeString(className+"_0","");
        return true;
    }

    public boolean dropClass(String className){
        if(className.equals(m_className) && is_dirty==1 && buffer!=null){
            flushToDisk();
            buffer = null;
        }
        if(delClass(className)==false)return false;
        classStructEditor.remove(className);
        classStructEditor.commit();
        int blockNum = 0;
        String prefix = "/data/data/"+mainActivity.getPackageName()+"/files/";
        File file = new File(prefix+className+"_"+Integer.toString(blockNum++));
        while(file.exists()){
            file.delete();
            file = new File(prefix+className+"_"+Integer.toString(blockNum++));
        }
        //INDEX and something else
        //Code Here
        return true;
    }

    /*--------------------Tuple-----------------------*/
    public void initial(String className){
        flushToDisk();
        m_className = className;
        m_blockNum = m_blockOffset = 0;
        buffer = null;
        mode_none_null = 1;
        is_dirty = 0;
    }
    public void initial(String className, int _bn,int _bo){
        flushToDisk();
        m_className = className;
        m_blockNum = _bn;
        m_blockOffset = _bo;
        buffer = null;
        mode_none_null = 1;
        is_dirty = 0;
    }
    public void flushToDisk(){
        if(is_dirty==1&&buffer!=null)
            writeString(m_className+"_"+Integer.toString(m_blockNum),encode(buffer));
        is_dirty = 0;
    }
    public void setNoneNull(int x){
        mode_none_null = x;
    }
    public ArrayList<String> Next(){
        if(buffer==null){
            String filename = m_className+"_"+Integer.toString(m_blockNum);
            buffer = decode(readString(filename));
            is_dirty = 0;
        }
        if(m_blockOffset==PAGESIZE){
            flushToDisk();
            m_blockOffset = 0;
            m_blockNum++;
            buffer = decode(readString(m_className+"_"+Integer.toString(m_blockNum)));
        }
        int len = buffer.size();
        if(m_blockOffset>=len)return null;
        String tmp = buffer.get(m_blockOffset++);
        if(mode_none_null>0 && tmp.equals(""))return Next();
        ArrayList<String> rs = decode(tmp);
        return rs;
    }
    public int getOffset(){
        return m_blockNum*PAGESIZE+m_blockOffset-1;
    }
    public void insert(String className,ArrayList<String> tuple){
        if(buffer!=null && className.equals(m_className)){
            int len = buffer.size();
            for(int i=0;i<PAGESIZE;i++){
                if(i<len && buffer.get(i).equals("")){
                    buffer.set(i,encode(tuple));
                    is_dirty = 1;
                    m_blockOffset = i+1;
                    return ;
                }else if(i>=len){
                    buffer.add(encode(tuple));
                    is_dirty = 1;
                    m_blockOffset = i+1;
                    return ;
                }
            }
        }
        flushToDisk();
        initial(className);
        setNoneNull(0);
        ArrayList<String> l_tuple = Next();
        while(l_tuple != null && l_tuple.size()>0){
            l_tuple = Next();
        }
        if(l_tuple==null) {
//            String tmp = readString(className+"_"+Integer.toString(m_blockNum));
//            writeString(className + "_" + Integer.toString(m_blockNum), tmp);
//            buffer = decode(tmp);
            buffer.add(encode(tuple));
            m_blockOffset++;
            is_dirty = 1;
            return;
        }
        buffer.set(m_blockOffset-1,encode(tuple));
        is_dirty = 1;
        return;
    }
    public void update(ArrayList<String> tuple){
        buffer.set(m_blockOffset-1,encode(tuple));
        is_dirty = 1;
    }
    public void delete(){
        buffer.set(m_blockOffset-1,"");
        is_dirty = 1;
    }
    /*--------------------Index-----------------------*/
    //Code Here

}