package com.example.rot.uitotem;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.example.rot.uitotem.tools.tinyCompute;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static StoreAPI storeAPI;
    static StoreAPI MEM;
    static StoreAPI TMP;
    static ParseResult result;
    static String sql;//存储创建类的语句，其实是为了保存代理规则


    private EditText editText;
    private Button buttonDoIt;
    private String tip = "";
    private boolean tf = true;
    ArrayList<ArrayList<String>> tuples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //初始化存储模块
        storeAPI = new StoreAPI(this);
        MEM = new StoreAPI(this);
        TMP = new StoreAPI(this);

        editText = (EditText)findViewById(R.id.edit_text);
        buttonDoIt = (Button)findViewById(R.id.button_doit);
        buttonDoIt.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_doit){
            sql = editText.getText().toString();
            Log.d("Hello", "SQL:"+sql);
            result = SQLParser.sqlParse(sql);
            if(result != null) {
                Analyze();
                if(tf){// return true
                    if(result.Type != 5 && result.Type != 6){
                        Toast.makeText(MainActivity.this, tip, Toast.LENGTH_SHORT).show();
                        if(tf)editText.getText().clear();
                    }else{// select
                        Intent intent = new Intent(MainActivity.this, activity_table.class);
                        intent.putExtra("sql", sql);
                        Bundle bundle=new Bundle();
                        bundle.putParcelableArrayList("tuples", (ArrayList)tuples);
                        intent.putExtras(bundle);
                        startActivity(intent);
                        if(tf)editText.getText().clear();
                    }
                }else{// return false
                    Toast.makeText(MainActivity.this, tip, Toast.LENGTH_SHORT).show();
                }
            }else{
                //Toast
                tip = "Please check your syntax!";
                Toast.makeText(MainActivity.this, tip, Toast.LENGTH_SHORT).show();
                editText.setText(editText.getText());
            }

        }

    }

    void Analyze(){
        tf = true;
        tip = "";
        switch (result.Type) {
            case 0:
                tf = Create();
                tip = "Create " + Boolean.toString(tf);
                break;
            case 1:
                tf = CreateSelectDeputy();
                tip = "CreateSelectDeputy " + Boolean.toString(tf);
                break;
            case 2:
                tf = Drop(result.className);
                tip = "Drop Class " + Boolean.toString(tf);
                break;
            case 3:
                tf = Insert();
                tip = "Insert " + Boolean.toString(tf);
                break;
            case 4:
                tf = Delete();
                tip = "Delete " + Boolean.toString(tf);
                break;
            case 5:
            case 6:
                if (result.Type == 5)
                    tuples = select();
                else
                    tuples = crossSelect();
                tip = "Select finish";
                break;
            case 7:
                tf = Update();
                tip = "Update True";
                break;
            default:
                tip = "Please check your input!";
        }
    }
    //参数：代理类结构 返回值：代理类标题的列表 （获取代理类的标题） (Note:获取源类的属性列表也是OK的)
    static ArrayList<String> getTitle(ClassStruct classStruct){
        ArrayList<String> son_title = new ArrayList<>();
        if(classStruct.virtualAttr!=null)
            for(int i=0;i<classStruct.virtualAttr.size();i++){
                if(classStruct.virtualAttr.get(i).attrRename!=null &&
                        !classStruct.virtualAttr.get(i).attrRename.equals(""))
                    son_title.add(classStruct.virtualAttr.get(i).attrRename);
                else
                    son_title.add(classStruct.virtualAttr.get(i).attrName);
            }
        if(classStruct.attrList!=null)
            for(int i=0;i<classStruct.attrList.size();i++){
                son_title.add(classStruct.attrList.get(i).attrName);
            }
        return son_title;
    }
    //参数：代理类结构 返回值：属性的类型列表，size不可信，defalut不可信
    static ArrayList<Attribute> getAttrL(ClassStruct classStruct){
        ArrayList<Attribute> res = new ArrayList<>();
        ClassStruct fa_classStruct = storeAPI.getClassStruct(classStruct.selectClassName);
        for(int i=0;i<classStruct.virtualAttr.size();i++){
            res.add(new Attribute(classStruct.virtualAttr.get(i).attrRename
                    ,0,10,""));
            if(classStruct.virtualAttr.get(i).attrRename==null||
                    classStruct.virtualAttr.get(i).attrRename.equals("")){
                res.get(i).attrName = classStruct.virtualAttr.get(i).attrName;
            }
            for(int j=0;j<fa_classStruct.attrList.size();j++){
                if(fa_classStruct.attrList.get(j).attrName.equals(classStruct.virtualAttr.get(i).attrName)){
                    res.get(i).attrType = fa_classStruct.attrList.get(j).attrType;
                    break;
                }
            }
        }
        if(classStruct.attrList!=null)
            for(int i=0;i<classStruct.attrList.size();i++){
                res.add(classStruct.attrList.get(i));
            }
        return res;
    }
    //参数：代理类结构，代理类元组 返回值：代理类恢复信息后的元组
    static ArrayList<String> compute(ClassStruct classStruct,ArrayList<String> tuple){
        ArrayList<String> next_tuple = null;
        ArrayList<String> next_title = null;
        next_title = getTitle(TMP.getClassStruct(classStruct.selectClassName));
        int offset = Integer.parseInt(tuple.get(0));
        TMP.initial(classStruct.selectClassName,offset/StoreAPI.PAGESIZE,offset%StoreAPI.PAGESIZE);
        next_tuple = TMP.Next();
        next_tuple.remove(0);
        for(int i=0;i<classStruct.attrList.size();i++){
            next_title.add(classStruct.attrList.get(i).attrName);
            next_tuple.add(tuple.get(i+1));
        }

        ArrayList<String> res = new ArrayList<>();
        if(classStruct.virtualAttr!=null)
            for(int i=0;i<classStruct.virtualAttr.size();i++){
                res.add(tinyCompute(next_title,next_tuple,classStruct.virtualAttr.get(i).attrName));
            }
        if(classStruct.attrList!=null)
            for(int i=0;i<classStruct.attrList.size();i++){
                res.add(tuple.get(i+1));
            }
        return res;
    }

    //参数：where 类结构 元组 返回值：真假值
    static boolean isSatisfy(WhereClause where,ClassStruct classStruct,ArrayList<String> tuple){
        if(where==null)return true;
        ArrayList<Attribute> attrlist = null;
        ArrayList<String> l_tuple = null;
        if(classStruct.selectClassName==null||classStruct.selectClassName.equals("")){
            // Source Class
            attrlist = classStruct.attrList;
            l_tuple = new ArrayList<>();
            for(int i=1;i<tuple.size();i++){
                l_tuple.add(tuple.get(i));
            }
            return where.judge(l_tuple,attrlist);
        }else{
            // Deputy Class
            l_tuple = compute(classStruct,tuple);
            attrlist = getAttrL(classStruct);
            return where.judge(l_tuple,attrlist);
        }
    }

    //从源类中删除一个元组，并把孩子的该元组都删除 Retur true Anyways!
    static boolean Delete(){
        ClassStruct classStruct = storeAPI.getClassStruct(result.className);
        ArrayList<String> tuple = null;
        storeAPI.initial(result.className);
        while((tuple=storeAPI.Next())!=null){
            if(isSatisfy(result.where,storeAPI.getClassStruct(result.className),tuple)){
                ArrayList<String> nexts = storeAPI.decode(tuple.get(0));
                for(int i=0;i<nexts.size();i++){
                    if(!nexts.get(i).equals("-1")) {
                        int offset = Integer.parseInt(nexts.get(i));
                        MEM.initial(classStruct.children.get(i),
                                offset/StoreAPI.PAGESIZE,offset%StoreAPI.PAGESIZE);
                        ArrayList<String> _t = MEM.Next();
                        MEM.delete();
                    }
                }
                storeAPI.delete();
            }
        }
        MEM.flushToDisk();
        return true;
    }

    //创建源类
    static boolean Create(){
        ClassStruct classStruct = new ClassStruct();
        classStruct.className = result.className;
        classStruct.selectClassName = null;
        classStruct.attrList = result.attrList;
        classStruct.virtualAttr = null;
        classStruct.condition = sql;
        classStruct.tupleNum = 0;
        classStruct.children = new ArrayList<>();
        return storeAPI.createClass(result.className,classStruct);
    }

    //创建代理类
    static boolean CreateSelectDeputy(){
        ClassStruct classStruct = new ClassStruct();
        classStruct.className = result.className;
        classStruct.selectClassName = result.selectClassName;
        classStruct.children = new ArrayList<>();
        classStruct.condition = sql;
        classStruct.virtualAttr = result.attrNameList;
        classStruct.attrList = result.attrList;
        if(storeAPI.existClass(classStruct.selectClassName)==false)return false;
        if(!storeAPI.createClass(result.className,classStruct))return false;

        ClassStruct fa_classStruct = storeAPI.getClassStruct(result.selectClassName);
        fa_classStruct.children.add(classStruct.className);
        Log.d("Hello", "CreateSelectDeputy: "+fa_classStruct.children.size());
        storeAPI.setClassStruct(fa_classStruct.className,fa_classStruct);

        storeAPI.initial(fa_classStruct.className);
        MEM.initial(classStruct.className);
        ArrayList<String > tuple = null;
        ArrayList<String> son_tuple = new ArrayList<>();
        son_tuple.add("-1");//pointers
        if(classStruct.attrList!=null)
            for(int i=0;i<classStruct.attrList.size();i++){
                son_tuple.add(classStruct.attrList.get(i).defaultVal);
            }
        while((tuple=storeAPI.Next())!=null){
            int son_offset = -1;
            if(isSatisfy(result.where,fa_classStruct,tuple)){
                int offset = storeAPI.getOffset();
                son_tuple.set(0,Integer.toString(offset));
                MEM.insert(classStruct.className,son_tuple);
                son_offset = MEM.getOffset();
            }
            ArrayList<String> tmp = storeAPI.decode(tuple.get(0));
            tmp.add(Integer.toString(son_offset));
            tuple.set(0,storeAPI.encode(tmp));
            storeAPI.update(tuple);
        }

        MEM.flushToDisk();
        return true;
    }

    //删除源类,实质上会把整棵树删除
    static boolean Drop(String className){
        if(storeAPI.existClass(className)==false){return false;}

        ClassStruct classStruct = storeAPI.getClassStruct(className);
        storeAPI.dropClass(className);

        if(classStruct!=null)
            for(int i=0;i<classStruct.children.size();i++){
                Drop(classStruct.children.get(i));
            }
        return true;
    }

    //删除代理类
    static boolean DropSelectDeputy(String className){
        if(!storeAPI.existClass(className)){
            return false;
        }

        //剔除父亲的关于它的指针
        ClassStruct fa_classStruct = storeAPI.getClassStruct(
                storeAPI.getClassStruct(className).selectClassName);
        int index = 0;
        if(fa_classStruct.children!=null)
            for(;index<fa_classStruct.children.size();index++){
                if(fa_classStruct.children.get(index).equals(className)){
                    fa_classStruct.children.remove(index);
                    storeAPI.setClassStruct(fa_classStruct.className,fa_classStruct);
                    storeAPI.initial(fa_classStruct.className);
                    ArrayList<String> tuple = null;
                    while((tuple = storeAPI.Next())!=null){
                        ArrayList<String> pointer = storeAPI.decode(tuple.get(0));
                        pointer.remove(index);
                        tuple.set(0,storeAPI.encode(pointer));
                        storeAPI.update(tuple);
                    }
                    break;
                }
            }

        Drop(className);
        return true;
    }

    //insert
    static boolean Insert()
    {
        //获取要插入的源类的类结构
        ClassStruct classStructOfSourceClass = storeAPI.getClassStruct(result.className);
        //类型检查
        try
        {
            //获取要插入的源类的属性类型等信息列表
            ArrayList<Attribute> attrListOfSourceClass = classStructOfSourceClass.attrList;
            Attribute attrTemp;
            if(attrListOfSourceClass!=null)
                for(int i = 0; i < attrListOfSourceClass.size(); i++)
                {
                    attrTemp = attrListOfSourceClass.get(i);
                    if(attrTemp.attrType == 0)
                    {
                        Integer.parseInt(result.valueList.get(i));
                    }
                }
        }
        catch(NumberFormatException e)
        {
            System.out.println("Type Error!");
            return false;
        }
        /*建立新tuple的0号位,没有就是空串*/
        ArrayList<String>sourcePointer = new ArrayList<String>();
        result.valueList.add(0,storeAPI.encode(sourcePointer));
        storeAPI.insert(result.className, result.valueList);
        //获取新插入的tuple的offset
        int sourceOffset = storeAPI.getOffset();
        //若存在子类，要判断是否插入子类，并且更新父亲的指针
        if(classStructOfSourceClass.children != null && classStructOfSourceClass.children.size() > 0)
        {
            for(int i = 0; i < classStructOfSourceClass.children.size(); i++)
            {
                //用MEM块来处理子块
                ClassStruct classStructOfChildrenClass = MEM.getClassStruct(classStructOfSourceClass.children.get(i));
                //对创建子类时的SQL语句重新解析，目的是获得Where的结构
                ParseResult conditionResult = SQLParser.sqlParse(classStructOfChildrenClass.condition);
                //判断的是父类的实属性和要插入的tuple值
                if(isSatisfy(conditionResult.where, classStructOfSourceClass, result.valueList))
                {
                    //如果符合条件，则也插入子类，在子类新增一个tuple，该tuple的第0位是父类中新增tuple的偏移量
                    ArrayList<String>newTuple = new ArrayList<String>();
                    newTuple.add(String.valueOf(sourceOffset));
                    //然后增加实属性的值
                    if(classStructOfChildrenClass.attrList != null && classStructOfChildrenClass.attrList.size()>0)
                    {
                        for(int j = 0; j < classStructOfChildrenClass.attrList.size(); j++)
                        {
                            //插入实属性的默认值
                            newTuple.add(classStructOfChildrenClass.attrList.get(j).defaultVal);
                        }

                    }
                    //将newTuple插入到子类中
                    MEM.insert(conditionResult.className,newTuple);
                    //修改源类指向子类的指针
                    int childrenOffset = MEM.getOffset();
                    sourcePointer.add(String.valueOf(childrenOffset));
                }
                else
                {
                    //不符合条件，不插入子类，修改源类指向子类的指针为-1
                    sourcePointer.add("-1");
                }
            }
            //对源类tuple的第0位编码
            String pointer = storeAPI.encode(sourcePointer);
            //更新源类刚插入的tuple值
            result.valueList.set(0, pointer);
            storeAPI.update(result.valueList);
        }
        MEM.flushToDisk();
        return true;
    }

    //return true Anyways
    static boolean Update()
    {
        //获取要插入的源类的类结构
        ClassStruct classStructOfSourceClass = storeAPI.getClassStruct(result.className);
        //start 代理类更新 （支持源类和代理类的更新）
        if(classStructOfSourceClass.selectClassName!=null&&!classStructOfSourceClass.selectClassName.equals("")){
            //deputy class update
            storeAPI.initial(classStructOfSourceClass.className);
            ArrayList<String> tuple = null;
            ArrayList<String> son_title = getTitle(classStructOfSourceClass);
            while((tuple=storeAPI.Next())!=null){
                if(isSatisfy(result.where,classStructOfSourceClass,tuple)){
                    ArrayList<String> l_tuple = compute(classStructOfSourceClass,tuple);
                    if(classStructOfSourceClass.attrList!=null)
                        for(int i=0;i<classStructOfSourceClass.attrList.size();i++){
                            for(int j=0;j<result.attrNameList.size();j++){
                                if(result.attrNameList.get(j).attrName.equals(classStructOfSourceClass.attrList.get(i).attrName)){
                                    tuple.set(i+1,tinyCompute(son_title,l_tuple,result.valueList.get(j)));
                                }
                            }
                        }
                    storeAPI.update(tuple);
                }
            }
            return true;
        }
        //END 代理类更新
        //要拿到该类的所有tuple，然后根据where的限制选择更新
        ArrayList<String> oldTuple = null;
        storeAPI.initial(result.className);
        ArrayList<String> fa_title = getTitle(classStructOfSourceClass);
        while((oldTuple = storeAPI.Next()) != null)
        {
            //如果当前tuple符合where的限制则更新
            if(isSatisfy(result.where, classStructOfSourceClass, oldTuple))
            {
                if(classStructOfSourceClass.attrList != null && classStructOfSourceClass.attrList.size() > 0)
                {
                    //遍历原属性列表和要更改的属性列表，确定要更改的属性
                    for(int i = 0; i < classStructOfSourceClass.attrList.size(); i++)
                    {
                        for(int j = 0; j < result.attrNameList.size(); j++)
                        {
                            //找到要更改的属性,是原属性列表的第i+1个属性值（第0位是指针位），要变成更改属性列表的第j个属性的值
                            if(classStructOfSourceClass.attrList.get(i).attrName.equals(result.attrNameList.get(j).attrName))
                            {
                                oldTuple.set(i+1,tinyCompute(fa_title,oldTuple,result.valueList.get(j)));
                            }
                        }
                    }
                }
                //解析指向子类的指针
                ArrayList<String> sourcePointer = storeAPI.decode(oldTuple.get(0));
                //接下来判断更新之后的元组是否还满足创建代理时的要求，不符合的话，要把对应的指针记录删掉,新满足的将会增加
                if(classStructOfSourceClass.children != null && classStructOfSourceClass.children.size() > 0)
                {
                    for(int i = 0; i < sourcePointer.size(); i++)
                    {

                        int childrenClassOffset = Integer.parseInt(sourcePointer.get(i));
                        //对创建子类时的SQL语句重新解析，目的是获得Where的结构
                        ClassStruct classStructOfChildrenClass = MEM.getClassStruct(classStructOfSourceClass.children.get(i));
                        ParseResult conditionResult = SQLParser.sqlParse(classStructOfChildrenClass.condition);

                        if(!sourcePointer.get(i).equals("-1"))
                        {
                            //子类符合，不用变动；子类不符合，要删除
                            if(!isSatisfy(conditionResult.where, classStructOfSourceClass, oldTuple))
                            {
                                MEM.initial(classStructOfChildrenClass.className,
                                        childrenClassOffset/StoreAPI.PAGESIZE,childrenClassOffset%StoreAPI.PAGESIZE);
                                ArrayList<String> temp = MEM.Next();
                                MEM.delete();
                                sourcePointer.set(i,"-1");
                            }
                        }else{
                            //子类符合应该添加
                            if(isSatisfy(conditionResult.where, classStructOfSourceClass, oldTuple))
                            {
                                ArrayList<String> l_tuple = new ArrayList<>();
                                l_tuple.add(Integer.toString(storeAPI.getOffset()));
                                for(int j=0;j<classStructOfChildrenClass.attrList.size();j++){
                                    l_tuple.add(classStructOfChildrenClass.attrList.get(j).defaultVal);
                                }
                                MEM.insert(classStructOfChildrenClass.className,l_tuple);
                                sourcePointer.set(i,Integer.toString(MEM.getOffset()));
                            }
                        }

                    }
                }

                //更新源类的元组
                oldTuple.set(0,storeAPI.encode(sourcePointer));
                storeAPI.update(oldTuple);
            }
        }
        MEM.flushToDisk();
        return true;
    }

    static ArrayList<ArrayList<String>> select(){
        ArrayList<ArrayList<String>> res = new ArrayList<>();
        if(!storeAPI.existClass(result.selectClassName))return res;
        ClassStruct classStruct = storeAPI.getClassStruct(result.selectClassName);
        ArrayList<String> title = new ArrayList<>();
        if(result.attrNameList!=null)
            for(int i=0;i<result.attrNameList.size();i++){
                if(result.attrNameList.get(i).attrRename!=null&&
                        !result.attrNameList.get(i).attrRename.equals(""))
                    title.add(result.attrNameList.get(i).attrRename);
                else
                    title.add(result.attrNameList.get(i).attrName);
            }
        res.add(title);

        //源类查询
        if(classStruct.selectClassName==null || classStruct.selectClassName.equals("")){
            storeAPI.initial(classStruct.className);
            ArrayList<String> tuple = null;
            ArrayList<String> fa_title = getTitle(classStruct);
            while((tuple=storeAPI.Next())!=null){
                if(isSatisfy(result.where,classStruct,tuple)){
                    tuple.remove(0);
                    ArrayList<String> tar = new ArrayList<>();
                    for(int i=0;i<result.attrNameList.size();i++){
                        String tmp = tinyCompute(fa_title,tuple,result.attrNameList.get(i).attrName);
                        tar.add(tmp);
                    }
                    res.add(tar);
                }
            }
            return res;
        }

        //代理类查询
        storeAPI.initial(classStruct.className);
        ArrayList<String> tuple = null;
        ArrayList<String> son_title = getTitle(classStruct);
        while((tuple=storeAPI.Next())!=null){
            if(isSatisfy(result.where,classStruct,tuple)){
                ArrayList<String> l_t = compute(classStruct,tuple);
                ArrayList<String> tar = new ArrayList<>();
                for(int i=0;i<result.attrNameList.size();i++){
                    String tmp = tinyCompute(son_title,l_t,result.attrNameList.get(i).attrName);
                    tar.add(tmp);
                }
                res.add(tar);
            }
        }
        return res;
    }

    //work for cross select (title+tuple)+result --> final answer
    static ArrayList<String> getCalc(ArrayList<String> tuple,ArrayList<String> title){
        ArrayList<String> res = new ArrayList<>();
        for(int i=0;i<result.attrNameList.size();i++){
            res.add(tinyCompute(title,tuple,result.attrNameList.get(i).attrName));
        }
        return res;
    }

    static ArrayList<ArrayList<String>> crossSelect(){
        ArrayList<ArrayList<String>> res = new ArrayList<>();
        ArrayList<String > res_title = new ArrayList<>();
        //title
        for(int i=0;i<result.attrNameList.size();i++){
            res_title.add(result.attrNameList.get(i).attrName);
            if(result.attrNameList.get(i).attrRename!=null &&
                    !result.attrNameList.get(i).attrRename.equals("")){
                res_title.set(i,result.attrNameList.get(i).attrRename);
            }
        }
        res.add(res_title);

        ClassStruct start_point = storeAPI.getClassStruct(result.selectClassName);
        ClassStruct end_point = storeAPI.getClassStruct(result.className);

        if(start_point.selectClassName==null||start_point.selectClassName.equals("")){
            //source -> deputy_1
            int childIndex = 0;
            for(int i=0;i<start_point.children.size();i++){
                if(start_point.children.get(i).equals(end_point.className)){
                    childIndex = i;
                    break;
                }
            }
            storeAPI.initial(start_point.className);
            ArrayList<String> stuple = null;
            ArrayList<String> title = getTitle(end_point);
            while((stuple=storeAPI.Next())!=null){
                if(isSatisfy(result.where,start_point,stuple)){
                    ArrayList<String> pointer = storeAPI.decode(stuple.get(0));
                    int childOffset = Integer.parseInt(pointer.get(childIndex));
                    if(childOffset<0)continue;
                    MEM.initial(end_point.className,
                            childOffset/StoreAPI.PAGESIZE,childOffset%StoreAPI.PAGESIZE);
                    ArrayList<String> tmp = compute(end_point,MEM.Next());
                    res.add(getCalc(tmp,title));
                }
            }
            return res;
        }else if(end_point.selectClassName==null || end_point.selectClassName.equals("")){
            //deputy_1 -> source
            storeAPI.initial(start_point.className);
            ArrayList<String> tuple = null;
            ArrayList<String> title = getTitle(end_point);
            while((tuple=storeAPI.Next())!=null){
                if(isSatisfy(result.where,start_point,tuple)){
                    int fatherOffset = Integer.parseInt(tuple.get(0));
                    MEM.initial(end_point.className,
                            fatherOffset/StoreAPI.PAGESIZE,fatherOffset%StoreAPI.PAGESIZE);
                    ArrayList<String> tmp = MEM.Next();
                    tmp.remove(0);
                    res.add(getCalc(tmp,title));
                }
            }
            return res;
        }else{
            //deputy_1 -> deputy_2
            storeAPI.initial(start_point.className);
            ArrayList<String> tuple = null;;
            ClassStruct father = storeAPI.getClassStruct(start_point.selectClassName);
            int index = 0;
            for(int i=0;i<father.children.size();i++){
                if(father.children.get(i).equals(end_point.className)){
                    index = i;
                    break;
                }
            }
            ArrayList<String> son_title = getTitle(end_point);
            while((tuple=storeAPI.Next())!=null){
                if(isSatisfy(result.where,start_point,tuple)){
                    int fatherOffset = Integer.parseInt(tuple.get(0));
                    MEM.initial(father.className,
                            fatherOffset/StoreAPI.PAGESIZE,fatherOffset%StoreAPI.PAGESIZE);
                    ArrayList<String > fa_tuple = MEM.Next();
                    int sonOffset = Integer.parseInt(storeAPI.decode(fa_tuple.get(0)).get(index));
                    if(sonOffset<0)continue;
                    MEM.initial(end_point.className,
                            sonOffset/StoreAPI.PAGESIZE,sonOffset%StoreAPI.PAGESIZE);
                    ArrayList<String > son_tuple = MEM.Next();
                    son_tuple = compute(end_point,son_tuple);
                    res.add(getCalc(son_tuple,son_title));
                }
            }
            return res;
        }
    }

}
