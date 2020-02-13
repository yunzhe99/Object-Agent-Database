package com.example.rot.uitotem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class tools {
    //参数：源类属性名称（标题），源类属性列表，源类值元组 返回值：返回匹配到的值
    static String getVal(String name, ArrayList<String> title, ArrayList<String> tuple){
        if(name.charAt(0)>='0' && name.charAt(0)<='9')return name;
        for(int i=0;i<title.size();i++){
            if(name.equals(title.get(i))){
                return tuple.get(i);
            }
        }
        return name;
    }
    //参数：源类属性列表，源类值元组，待计算的表达式 返回值：计算后的value，若为字符串则直接返回。
    static String tinyCompute(ArrayList<String> title,ArrayList<String> tuple,String expr){
        expr += '=';
        ArrayList<String> num = new ArrayList<>();
        ArrayList<Character> op = new ArrayList<>();
        op.add('=');
        String buffer = new String("");
        char ch = expr.charAt(0);
        int len = expr.length();
        int start = 0;
        Map<Character,Integer> pro = new HashMap<>();
        pro.put('=',0);
        pro.put('(',1);
        pro.put('+',3);
        pro.put('-',3);
        pro.put('*',5);
        pro.put('/',5);
        while(start<len){
            ch = expr.charAt(start++);
            if(ch=='('){
                if(!buffer.equals("")){
                    num.add(buffer);
                    buffer = new String("");
                }
                op.add(ch);
            }else if(ch==')'){
                if(!buffer.equals("")){
                    num.add(buffer);
                    buffer = new String("");
                }
                while(op.size()>0 && op.get(op.size()-1)!='('){
                    num.add(Character.toString(op.get(op.size()-1)));
                    op.remove(op.size()-1);
                }
                op.remove(op.size()-1);//delete (
            }else if(ch=='+' || ch=='-' || ch=='*' || ch=='/'){
                if(!buffer.equals("")){
                    num.add(buffer);
                    buffer = new String("");
                }
                while(op.size()>0 && pro.get(op.get(op.size()-1)) >= pro.get(ch) ){
                    num.add(Character.toString(op.get(op.size()-1)));
                    op.remove(op.size()-1);
                }
                op.add(ch);
            }else if(ch=='='){
                if(!buffer.equals("")){
                    num.add(buffer);
                    buffer = new String("");
                }
                for(int i=op.size()-1;i>=0;i--){
                    num.add(Character.toString(op.get(i)));
                }
                break;
            }else{
                buffer += ch;
            }
        }
        op = new ArrayList<>();
        int n = num.size(),index = -1;
        int ans = 0;
        for(int i=0;i<n;i++){
            if(num.get(i).equals("+")){
                ans = Integer.parseInt(getVal(num.get(index),title,tuple))
                        +Integer.parseInt(getVal(num.get(index-1),title,tuple));
                num.set(index-1,Integer.toString(ans));
                index--;
            }else if(num.get(i).equals("-")){
                ans = Integer.parseInt(getVal(num.get(index-1),title,tuple))
                        -Integer.parseInt(getVal(num.get(index),title,tuple));
                num.set(index-1,Integer.toString(ans));
                index--;
            }else if(num.get(i).equals("*")){
                ans = Integer.parseInt(getVal(num.get(index),title,tuple))
                        *Integer.parseInt(getVal(num.get(index-1),title,tuple));
                num.set(index-1,Integer.toString(ans));
                index--;
            }else if(num.get(i).equals("/")){
                ans = Integer.parseInt(getVal(num.get(index-1),title,tuple))
                        /Integer.parseInt(getVal(num.get(index),title,tuple));
                num.set(index-1,Integer.toString(ans));
                index--;
            }else{
                index++;
                num.set(index,num.get(i));
            }
        }
        return getVal(num.get(0),title,tuple);
    }
}
