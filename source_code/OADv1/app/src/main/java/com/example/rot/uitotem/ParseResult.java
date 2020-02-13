package com.example.rot.uitotem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ParseResult {

    public int Type;//0:create class; 1:create select deputy class
    //2:Drop;     3:Insert;  4:Delete
    //5:select;   6: cross select
    public String className;
    public String selectClassName;
    public ArrayList<Attribute> attrList;
    public ArrayList<AttrNameTuple> attrNameList;
    public ArrayList<String> valueList;
    public WhereClause where;
}

class WhereClause{
    int operationType;   //>: 0   <:1    =: 2   <>: 3   >=: 4   <=:5
    //NOT : 6      AND:7      OR:8
    //int: 9    String: 10
    int valueInt;
    String valueString;
    WhereClause left;
    WhereClause right;

    ArrayList<String> tuple;
    ArrayList<Attribute> attrs;
    int is_int;
    private String getValue(String name){
        int len = tuple.size();
        for(int i=0;i<len;i++){
            if(name.equals(attrs.get(i).attrName)){
                is_int = attrs.get(i).attrType==0?1:0;
                return tuple.get(i);
            }
        }
        return "";
    }
    private boolean node(WhereClause cond){
        WhereClause _left = cond.left,_right=cond.right;
        int l;
        String sl;
        switch (cond.operationType){
            case 0:
                sl = getValue(_left.valueString);
                if(_right.operationType==9){
                    return Integer.parseInt(sl)>_right.valueInt;
                }else{
                    return sl.compareTo(_right.valueString)>0?true:false;
                }
            case 1:
                sl = getValue(_left.valueString);
                if(_right.operationType==9){
                    return Integer.parseInt(sl)<_right.valueInt;
                }else{
                    return sl.compareTo(_right.valueString)<0?true:false;
                }
            case 2:
                sl = getValue(_left.valueString);
                if(is_int==1)return Integer.parseInt(sl)==_right.valueInt;
                return sl.equals(_right.valueString);
            case 3:
                sl = getValue(_left.valueString);
                if(is_int==1)return Integer.parseInt(sl) != _right.valueInt;
                return !sl.equals(_right.valueString);
            case 4:
                sl = getValue(_left.valueString);
                if(_right.operationType==9){
                    return Integer.parseInt(sl)>=_right.valueInt;
                }else{
                    return sl.compareTo(_right.valueString)>=0?true:false;
                }
            case 5:
                sl = getValue(_left.valueString);
                if(_right.operationType==9){
                    return Integer.parseInt(sl)<=_right.valueInt;
                }else{
                    return sl.compareTo(_right.valueString)<=0?true:false;
                }
            case 6:
                return !node(_left);
            case 7:
                return node(_left)&&node(_right);
            case 8:
                return node(_left)||node(_right);
            case 9:
                if(cond.valueInt==0)return false;
                return true;
            case 10:
                if(cond.valueString.equals(""))return false;
                return true;
            default:
                return true;
        }
    }
    public boolean judge(ArrayList<String> _tuple,ArrayList<Attribute> _attrs){
        tuple = _tuple;
        attrs = _attrs;
        return node(this);
    }
}

class Attribute {
    public String attrName;
    public int attrType;
    public int attrSize;
    public String defaultVal;

    public Attribute(String attrName, int attrType, int attrSize,String defaultVal) {
        this.attrName = attrName;
        this.attrType = attrType;
        this.attrSize = attrSize;
        this.defaultVal = defaultVal==null?"":defaultVal;
    }
}


class AttrNameTuple {
    public String attrName;
    public String attrRename;
    public AttrNameTuple(String _attrName,String _attrRename){
        this.attrName = _attrName;
        this.attrRename = _attrRename;
    }
    public AttrNameTuple(){}
}

class ClassStruct{
    public int tupleNum;//元组数量,未使用
    public String className;//类名
    public String selectClassName; //代理的源类，仅代理类有用
    public ArrayList<String> children;//源类的孩子在哪里，仅源类有用，因为仅支持一级代理
    public ArrayList<Attribute> attrList;//源类的实属性列表，代理类的实属性列表包括默认值
    public String condition;//代理条件，仅代理类有用，实质存储的是创建该类的时候的sql语句
    public ArrayList<AttrNameTuple> virtualAttr;//虚属性，仅仅代理类使用，attrName存储表达式（有的话），attrReName，存储重命名
}