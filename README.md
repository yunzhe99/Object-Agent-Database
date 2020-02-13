# Object-Agent-Database概述
对象关系数据库的核心仍然是关系数据库技术，通过上层结构或者构造一个包装层， 对象关系数据库将OODBMS的特点转化成RDBMS所能管理的形式，以模拟ORDBMS的功能的组件。它将用户提交的对象关系型查询映像成关系型查询，然后在传统的关系型的引擎上执行。所以ORDBMS的功能很大程度上受制于底层的RDBMS，而且接口或包装层也会影响执行效率，在性能上可能会更差。而且这类数据库对于复杂对象管理功能还比较弱，不能表现数据丰富的语义， 连接查询开销与RDB一样仍然很大。国外的一些专家们也指出：靠增加一些模块到一个已经很复杂的关系数据库上去的途径，并不能从根本上解决问题。数据库技术的一个基本目标就是要找到一个恰当的数据模型来表达它所管理的对象。为了实现复杂信息管理，有必要找到一个新的数据模型，它既具有关系数据模型的柔软性，又具有面向对象数据模型的复杂信息表现能力。

分析关系数据模型，我们发现关系数据模型之所以非常柔软是因为它将数据表示成简单的关系表，关系表可以通过关系代数进行分割和重组，变换其表现形式以满足不同数据库应用的需要。 面向对象数据模型之所以缺乏柔软性，是因为对象封装了数据和操作，很难进行分割和组合。

在现实世界中，人很难进行分割和组合，但代理人可以帮助人进行分身和重组。如果在数据库中把人定义成对象，那代理人就需要用代理对象定义，因此有必要把代理对象引入数据库中。 代理对象除了继承对象的属性和方法，还具有自己独自的属性和方法， 一个对象可以有多个代理对象，多个对象可以共有一个代理对象。也就是说对象虽然难以直接地分割和重组，但通过其代理对象可以间接地进行。 基于这一思想， 我们提出了对象代理模型。对象代理模型将现实世界中客观实体表示成对象， 通过定义代理对象来表现对象的多方面本质和动态变化特性。它既具有关系数据模型的柔软性，又具有面向对象数据模型表现复杂信息的能力，因此能满足复杂数据管理的建模需求。 基于对象代理模型，采用先进的数据库实现技术，武汉大学计算机学院珞珈图腾实验室开发了对象代理数据库管理系统 TOTEM。 该系统统一地实现了对象视图、角色多样性及对象移动， 能够有效地支持个性化信息服务、复杂对象的多表现及对象动态分类。 它具有跨类查询新机制， 针对某个类的对象，通过对象与代理对象间的双向指针，可以找到其他类所定义的信息，能够用来实现目前正在兴起的跨媒体应用。

我们将TOTEM移植到Android端，在Android端实现了一个简单的对象代理数据库。此工作为武汉大学弘毅学堂计算机方向的数据库实验课程。

# 系统构想

系统构想为：用户输入一条SQL语句，点击"DO IT!"按钮，就执行对应的SQL语句。

由上述应用可知，系统需要完成的工作包括编译、执行、存储。编译需要获取SQL语句中提供的信息，执行计算出结果，而存储则将执行的结果保存下来。

下文将逐一进行介绍。

# 编译

由于我们的工作是制作一个手机APP，因此需要生成的是Java代码，我们使用了JavaCC工具。

**JavaCC**（**Java** **C**ompiler **C**ompiler）是一个开源的语法分析器生成器和词法分析器生成器。JavaCC根据输入的文法生成由Java语言编写的分析器。

和YACC类似，JavaCC根据由EBNF格式撰写的形式文法生成语法分析器。不同的是，JavaCC生成的是自顶向下语法分析器，由于可以向前搜寻k个字符，所以可以用来分析LL(k)文法。同时，JavaCC生成词法分析器的方式和Lex也很像。

## 文件头

首先是JavaCC的选项设置，这个按需要设置。

我们是这样设置的：

```java
options{
LOOKAHEAD = 2;
STATIC = false ;
DEBUG_PARSER = true;
 }
```

接着是定义主类。这是整个解析程序的入口，里面主要有一些引用的包和类以及一个main方法（其他的方法由JavaCC生成）。由于上面把STATIC设为false了，所这这里需要创建一个parser对象。

我们的定义如下：

```java
PARSER_BEGIN(SQLParser)

import java.util.ArrayList;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SQLParser
{

  public static ParseResult sqlParse(String sqlInput)
  {
    try
    {
      InputStream s=new ByteArrayInputStream(sqlInput.getBytes());
      SQLParser p = new SQLParser(s);
      return p.start();
    }
    catch(Exception e)
    {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
    return null;
  }
}

PARSER_END(SQLParser)
```

## 编译数据结构

在内存中维护过多的数据结构，是一件很占用空间的事情。为了减小内存占用，我们将数据库维护的表主要放在了存储部分，而在编译和执行部分都使用统一的数据结构。

编译部分的数据结构需要能够及时存储编译获取到的信息，并将这些信息及时准确的交给执行部分的数据结构。因此我们设计了如下所示的数据结构。

```java
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
```

其中，ParseResult是编译时存放数据的地方。详细结构如下：

- Type存放语句的类型
- className存放类名
- 对于代理类，selectClassName存放其源类名
- attrList存放实属性，其中存放了实属性的名称、类型、大小和数值。
- attrNameList存放虚属性，其中存放了虚属性的名称和代理规则，代理规则在编译阶段存放的是字符串类型的表达式，后续会继续进行计算。
- where存放where后面的语句，并提供了判断是否满足条件的函数。where结构中存在一个递归调用，即where的左边右边还分别是一个where结构，中间节点则是where的符号，这样的结构方便了后续的where内容的处理。

## 一些通用函数

在编译部分存在一些用的比较多的函数，我们将其分开，便于代码的维护。

### 属性类型定义

在这一部分，我们首先匹配属性类型，匹配到就调用上面在定义数据结构的时候一起定义的方法对属性初始化，并返回初始化后的属性，方便执行部分调用。

```java
ArrayList<Attribute> attributeDef():
{
  Token attrName;
  ArrayList<Attribute> result = new ArrayList<Attribute>();
  Attribute attr;
}
{
    attrName = <ID>
    (
    <INT>{attr = new Attribute(attrName.image, 0,4, null);result.add(attr);}
    |
    <CHAR>{attr = new Attribute(attrName.image, 1,10, null);result.add(attr);}
    )
    (
    <COMMA>
    attrName = <ID>
    (
    <INT>{attr = new Attribute(attrName.image, 0,4, null);result.add(attr);}
    |
    <CHAR>{attr = new Attribute(attrName.image, 1,10, null);result.add(attr);}
    ))*
  {
    return result;
  }
}
```

### 代理规则

代理规则往往是select开始，但是后面的往往跟的是一个较为复杂的式子。我们决定在执行部分再去对式子进行解析和计算，在编译部分就只是以字符串的形式存放这些式子。虚属性的名字存放在attrRename中，而代理规则存放在attrName中(因为是从实属性来的)。

```java
ArrayList<AttrNameTuple> attributeSelect():
{
  Token attrName;
  Token attrRename;
  String name = "";
  ArrayList<AttrNameTuple> result = new ArrayList<AttrNameTuple>();
  AttrNameTuple n;
}
{
  ((attrName = <ID> | attrName = <INTNUMBER> | attrName = <PLUS>
  | attrName = <MINUS> | attrName = <MULT> | attrName = <DIV>
  | attrName = <LEFT_BRACKET> | attrName = <RIGHT_BRACKET>)
  {
    name = name + attrName.image;
  })+
  {
    n = new AttrNameTuple(name, null);
    name = "";
  }
  (<AS> attrRename = <ID>
  {
    n.attrRename = attrRename.image;
  })?
  {
    result.add(n);
  }
  (
  <COMMA> ((attrName = <ID> | attrName = <INTNUMBER> | attrName = <PLUS>
  | attrName = <MINUS> | attrName = <MULT> | attrName = <DIV>
  | attrName = <LEFT_BRACKET> | attrName = <RIGHT_BRACKET>)
  {
    name = name + attrName.image;
  })+
  {
    n = new AttrNameTuple(name, null);
    name = "";
  }
  (<AS> attrRename = <ID>
  {
    n.attrRename = attrRename.image;
  })?
  {
    result.add(n);
  }
  )*
  {
    return result;
  }
}
```

### where语句的存放

where语句和上面的代理规则一样，也存在计算的问题。由于where内各个符号的优先级的不同，我们设计了一个语法树来存储where的表达式。树的每个节点都有左孩子、右孩子和符号(可以为空)。通过where节点的深度来区分优先级。优先级越高的深度越深。这样在执行部分只属于根据中间符号匹配左右两边是否满足要求即可。

```java
WhereClause where_expr():
{
  WhereClause left;
  WhereClause right = null;
  WhereClause result;
}
{
  left = and_expr()
  ( <OR> right = where_expr())?
  {
    if(right == null ){
      return left;
    }else{
      result = new WhereClause();
      result.operationType = 8;
      result.left = left;
      result.right = right;
      return result;
    }
  }
}

WhereClause and_expr():
{
  WhereClause left;
  WhereClause right = null;
  WhereClause result;
}
{
  left = not_expr()
  ( <AND> right = and_expr())?
  {
    if(right == null ){
      return left;
    }else{
      result = new WhereClause();
      result.operationType = 7;
      result.left = left;
      result.right = right;
      return result;
    }
  }
}

WhereClause not_expr():
{
  WhereClause left;
  WhereClause right = null;
  WhereClause result;
}
{
  (
  (<NOT> left = not_expr()
  {
    result = new WhereClause();
    result.operationType = 6;
    result.left = left;
  })
  | (result = cmp_expr())
  | ( <LEFT_BRACKET> result = where_expr() <RIGHT_BRACKET>)
  )
  {
    return result;
  }
}

WhereClause cmp_expr():
{
  Token numOrID;
  WhereClause left;
  WhereClause right = null;
  WhereClause result = null;
}
{
  (
  (numOrID = <INTNUMBER>
  { left = new WhereClause();
    left.operationType = 9; left.valueInt = (new Integer(numOrID.image)).intValue(); })
  |
  (numOrID = <ID>
  { left = new WhereClause();
    left.operationType = 10; left.valueString = numOrID.image;
  })
  )
  [
  ((<BIGGER>
  { result = new WhereClause();
    result.operationType = 0;})
  |(<LESS>
  { result = new WhereClause();
    result.operationType = 1;})
  |(<EQUAL>
  { result = new WhereClause();
    result.operationType = 2;})
  |(<NOTEQUAL>
  { result = new WhereClause();
    result.operationType = 3;})
  |(<BIGGEREQUAL>
  { result = new WhereClause();
    result.operationType = 4;})
  |(<LESSEQUAL>
  { result = new WhereClause();
    result.operationType = 5;})
  )
  (
  (numOrID = <INTNUMBER>
  { right = new WhereClause();
    right.operationType = 9;
    right.valueInt = (new Integer(numOrID.image)).intValue();
  })
  |
  (numOrID = <STRING>
  {
    right = new WhereClause();
    right.operationType = 10;
    right.valueString = numOrID.image.substring(1, numOrID.image.length()-1);
  }
  )
  )
  ]
  {
    if (result == null){
      return left;
    }else{
      result.left = left;
      result.right = right;
      return result;
    }
  }
```

## 语义规则

定义语义规则，是编译部分最主要的工作。

### 空格等

首先，遇到空格、换行等，需要跳过，继续扫描下面的输入。

```java
SKIP :
{
  " "
| "/t"
| "/n"
| "/r"
}
```

### 定义token

定义token，方便后续查找关键词。

```java
TOKEN:
{
<SEMICOLON:";">
| <UPDATE:"UPDATE">
|<CREATE:"CREATE">
|<DROP:"DROP">
|<CLASS:"CLASS">
|<INSERT:"INSERT">
|<INTO:"INTO">
|<VALUES:"VALUES">
|<LEFT_BRACKET:"(">
|<COMMA:",">
|<RIGHT_BRACKET:")">
|<DELETE:"DELETE">
|<FROM:"FROM">
|<WHERE:"WHERE">
|<SELECT:"SELECT">
|<SELECTDEPUTY:"SELECTDEPUTY">
|<ID: ["a"-"z"](["a"-"z","A"-"Z","0"-"9"])* >
|<EXPRESSION: ["{","[","("](["a"-"z","A"-"Z","0"-"9","{","}","[","]","(",")","+","-","*","/"])* >
|<EQUAL:"=">
|<INT: "0"|(["1"-"9"](["0"-"9"])*) >
|<STRING: "\""(["a"-"z","A"-"Z","1"-"9"])*"\"" >
|<CROSS:"->">
|<DOT:".">
|<AS:"AS">
| <BOOLEXP:(["a"-"z","A"-"Z","0"-"9","+","(",")","-","*","/",">","<","=","!",".","\""])* >
|<PLUS:"+">
|<SET:"SET">
}
```

### CREATE语句

CREATE语句有两种，分别是创建源类和代理类。

#### 创建源类

创建源类的语法如下：

```sql
CREATE CLASS <class_name> ([ATTRIBUTE] ({<column><type><attr_constrain>}),[<class_constrain>]) [METHOD{<method_definition>}]；
```

如果在CREATE后面匹配到CLASS，则说明是创建源类。此时，将匹配到的CLASS后面的字符赋值给className，将括号里面的一个或多个属性名和属性类型都赋值到attrName列表和attrList列表中。

实现代码如下：

```java
ParseResult createClass():
{
  ParseResult result = new ParseResult();
  ParseResult selectResult;
  Token className;
}
{
  <CREATE> <CLASS>
  {
    result.Type = 0;
  }
  className = <ID>
  {
    result.className = className.image;
  }
  <LEFT_BRACKET>
    result.attrList = attributeDef()
  {

  }
  <RIGHT_BRACKET>
  ( selectResult = select()
  {
    result.selectClassName = selectResult.selectClassName;
  })?
  {
    return result;
  }
}
```

#### 创建代理类

创建代理类的语法如下：

```sql
CREATE SELECTDEPUTYCLASS <class_name> [[ATTRIBUTE] ({<column><type> [attr_constrain]}, [<class_constrain>])] AS <deputy_rule>;
[METHOD {<method_definition>}]
[WRITE ({soruce.attr = expression})]
```

如果在CREATE后面匹配到SELECTDEPUTYCLASS，则说明是创建代理类。

创建代理类的语法比创建源类的语法多了代理类的源类的名字、代理规则。我们将源类的名字存在selectClassName中，并调用select()函数获取获取代理规则，将虚属性存在attrNameList中，并将选择条件存在where结构里面。

具体实现如下：

```java
ParseResult createSelectDeputy():
{
  ParseResult result = new ParseResult();
  ParseResult selectResult;
  Token className;
}
{
  <CREATE> <SELECTDEPUTY>
  {
    result.Type = 1;
  }
  className = <ID>
  {
    result.className = className.image;
  }
  (<LEFT_BRACKET>
   result.attrList = attributeDef()
   <RIGHT_BRACKET>
  )*
  selectResult = select()
  {
    result.selectClassName = selectResult.selectClassName;
    result.attrNameList = selectResult.attrNameList;
    result.where = selectResult.where;
  }
  {
    return result;
  }
}
```

### DROP语句

在编译部分，drop语句比较简单。

```sql
DROP CLASS product;
```

只需要将DROP的类名存起来即可。

```java
ParseResult drop():
{
  ParseResult result;
  Token className;
}
{
  <DROP> <CLASS> className = <ID>
  {
    result = new ParseResult();
    result.Type = 2;
    result.className = className.image;
    return result;
  }
}
```

### INSERT语句

INSERT语句如下所示：

```sql
INSERT INTO product ( id , name , price ) VALUES ( 1 , 'mac' , 14000 );
```

将类名存进className，将后面的一个或多个值依次存进valueList即可。

```java
ParseResult insert():
{
  ParseResult result = new ParseResult();
  ArrayList<String> valList = new ArrayList<String>();
  Token className;
  Token val;
}
{
  <INSERT> <INTO> className = <ID>
  <LEFT_BRACKET>
  <ID>
  (
  <COMMA>
  <ID>
  )*
  <RIGHT_BRACKET>
  <VALUES>
  {
    result.Type = 3;
    result.className = className.image;
  }
  <LEFT_BRACKET>
  ( ((val = <INTNUMBER>) { valList.add(val.image); }) |
  (( val = <STRING> )
  {
    valList.add(val.image.substring(1, val.image.length() - 1));
  }))
  (
  <COMMA> ( ((val = <INTNUMBER>) { valList.add(val.image); }) |
  (( val = <STRING> )
  {
    valList.add(val.image.substring(1, val.image.length() - 1));
  }))
  )*
  <RIGHT_BRACKET>
  {
    result.valueList = valList;
    return result;
  }
}
```

### UPDATE语句

UPDATE语句较为复杂。

```sql
UPDATE product SET price=4900 WHERE name='mac';
UPDATE usproduct SET sales=3000 WHERE name="ipad";
```

UPDATE分为两种，一种是更新源类，一种是更新代理类。

但是在编译部分，这两种并没有太大的区别。我们先将类名存起来，然后在执行部分再判断这个类是源类还是代理类。

这里的属性名，由于无法区分是实属性还是虚属性，统一赋值到attrNameRename中，交由执行部分处理。

对于where语句的处理与上面是一样的。

```java
ParseResult update():
{
  Token className;
  Token attrName;
  Token val;
  ArrayList<AttrNameTuple> attrNameList = new ArrayList<AttrNameTuple>();
  AttrNameTuple attrNameRename;
  ArrayList<String> valList = new ArrayList<String>();
  WhereClause where = null;
}
{
  <UPDATE> className = <ID> <SET>
  attrName = <ID> <EQUAL>
  ((val = <INTNUMBER> {valList.add(val.image);})
   |(val = <STRING>   {valList.add(val.image.substring(1, val.image.length()-1));}))
   {
     attrNameRename = new AttrNameTuple(attrName.image, null);
     attrNameList.add(attrNameRename);
   }
  (<COMMA>
  attrName = <ID> <EQUAL>
  ((val = <INTNUMBER>{valList.add(val.image);})
   |(val = <STRING>{valList.add(val.image.substring(1, val.image.length()-1));}))
   {
     attrNameRename = new AttrNameTuple(attrName.image, null);
     attrNameList.add(attrNameRename);
   }
  )*
  (<WHERE>
  where = where_expr())?
  {
    ParseResult result = new ParseResult();
    result.Type = 7;
    result.className = className.image;
    result.valueList = valList;
    result.attrNameList = attrNameList;
    result.where = where;
    return result;
  }
}
```

### DELETE语句

DELETE语句与DROP语句比较像，但是由于是删除某一些记录，多了一些输入。

```sql
DELETE FROM product WHERE name='mi' ;
```

按照之前的做法依次存放即可。

```java
ParseResult delete():
{
  ParseResult result = new ParseResult();
  Token className;
  WhereClause where = new WhereClause();
}
{
  <DELETE> <FROM> className = <ID> <WHERE>
  where = where_expr()
  {
    result.Type = 4;
    result.className = className.image;
    result.where = where;
    return result;
  }
}
```

### SELECT语句

SELECT语句分为两类：

- 类间查询。在同一个类中查询，直接查询就好。
- 跨类查询。如果要查询的属性，在当前类中不存在，但是在它的源类或者源类的其他代理类中存在，也可以查询。这是通过代理类和源类之间的指针实现的。

由于两种查询的语法有所区别，我们将分别对其匹配。

```sql
SELECT id , name , price FROM product;
SELECT sales , name , usprice FROM usproduct;
SELECT  jpproduct->product->usproduct.sales ,  jpproduct->product->usproduct.name FROM jpproduct WHERE id=2;
```

对于类间查询，直接将要查询的属性和类存储起来即可。与上面类似，我们目前还无法判断一个类是源类还是代理类，因此暂时不加区分。

对于跨类查询，由于跨类查询中用到的各个类之间都有指针，我们只存储了查询起点和查询终点。其他处理和判断，在执行部分完成。

对于where语句的处理与上文相同。

```java
ParseResult select():
{
  ParseResult result = new ParseResult();
  ArrayList<AttrNameTuple> selectAttr = new ArrayList<AttrNameTuple>();
  Token className;
  Token selectClassName;
  Token attr;
  AttrNameTuple attrtuple1 = new AttrNameTuple();
  WhereClause where = null;
}
{
  <SELECT>
  (
      (selectAttr = attributeSelect() <FROM> selectClassName = <ID> (<WHERE>
      where = where_expr())?
      {
        result.Type = 5;
        result.attrNameList = selectAttr;
        result.selectClassName = selectClassName.image;
        result.where = where;
        return result;
      }
      )
  |
      (
      <ID>
      <CROSS>
      <ID>
      <CROSS>
      className = <ID>
      <DOT>
      attr = <ID>
      {
          attrtuple1.attrName = attr.image;
          selectAttr.add(attrtuple1);
      }
      (
      <COMMA>
      <ID>
      <CROSS>
      <ID>
      <CROSS>
      <ID>
      <DOT>
      attr = <ID>
      {
          AttrNameTuple attrtuple2 = new AttrNameTuple();
          attrtuple2.attrName = attr.image;
          selectAttr.add(attrtuple2);
      }
      )*
      <FROM> selectClassName = <ID> (<WHERE>
      where = where_expr())?
      {
        result.Type = 6;
        result.selectClassName = selectClassName.image;
        result.attrNameList = selectAttr;
        result.className = className.image;
        result.where = where;
        return result;
      }
      )
  )
}
```

# 执行

执行部分是整个系统最复杂的一个部分，稍不注意就会出错。并且这一部分的错误很难定位。

执行部分首先从编译部分得到数据，存入自己的数据结构中，然后对不同的语句，执行对应的操作，并将操作的结果及时交给存储部分，由存储部分存到硬盘。

整个系统的各个部分都是在执行部分调用的。可以说，执行部分是整个系统最核心的部分。

## 执行数据结构

编译部分的数据结构核心是方便编译时对数据的存储。执行部分，我们另外设计了一个数据结构，用来存放每个类的信息，以方便执行部分调用。虽然在存储部分我们设计了很多张表，但是我们并不会在内存里面同时维护那么多的表，而是将一个类我们需要的所有信息都读到执行部分的数据结构中。这种方式可以明显减少程序的内存占用。

```java
class ClassStruct{
    public int tupleNum;//元组数量,未使用
    public String className;//类名
    public String selectClassName; //代理的源类，仅代理类有用
    public ArrayList<String> children;//源类的孩子在哪里，仅源类有用，因为仅支持一级代理
    public ArrayList<Attribute> attrList;//源类的实属性列表，代理类的实属性列表包括默认值
    public String condition;//代理条件，仅代理类有用，实质存储的是创建该类的时候的sql语句
    public ArrayList<AttrNameTuple> virtualAttr;//虚属性，仅仅代理类使用，attrName存储表达式（有的话），attrReName，存储重命名
}
```

## 执行数据流

在执行的过程中，用户输入一条SQL语句，点击执行按钮，系统才会执行。因此我们重载了onClick函数，在这个函数里面进行处理。

### 调用分析器

用户输入的SQL语句，对于系统而言是一个string串。讲这个string串放到编译部分提供的分析器里面去分析，即可得到编译的结果。在后续的过程中，我们将多次调用这个编译的结果result。

```java
sql = editText.getText().toString();
result = SQLParser.sqlParse(sql);//在这边调用语法解析器
```

### 执行处理过程

接下来，我们调用了一个分析函数Analyze，在这里完成了执行部分的处理。函数Analyze维护两个全局变量：

- tip：不同语句情况下给用户的提示信息
- tf：标志位，判断是否执行成功

执行成功则提示用户成功，否则提示用户失败。如果是select语句，还要显示select的结果，因此需要跳转到select的界面，并将select的结果显示出来。

```java
Analyze();//这边是主要的执行部分
                if(tf){// return true
                    if(result.Type != 5 && result.Type != 6){//5,6为查找，则提示查找成功
                        Toast.makeText(MainActivity.this, tip, Toast.LENGTH_SHORT).show();
                        if(tf)editText.getText().clear();
                    }else{// select
                        Intent intent = new Intent(MainActivity.this, activity_table.class);//这就下一个界面了
                        intent.putExtra("sql", sql);
                        Bundle bundle=new Bundle();
                        bundle.putParcelableArrayList("tuples", (ArrayList)tuples);//这边继续显示
                        intent.putExtras(bundle);
                        startActivity(intent);
                        if(tf)editText.getText().clear();
                    }
                }else{// return false
                    Toast.makeText(MainActivity.this, tip, Toast.LENGTH_SHORT).show();
                }
```

传给显示页面的参数是select执行之后获得的二维数组tuples，将其依次输入到表格中即可。

```java
        if(tuples.size() != 0){
            int column = tuples.get(0).size();
            int row = tuples.size();
            int gridMinWidth = getScreenWidth()/column - column+1;
            gridLayout.setColumnCount(column);
            gridLayout.setRowCount(row);
            for(int i = 0; i < row; i++){
                for(int j = 0; j < column;j++){
                    GridLayout.Spec rowSpec=GridLayout.spec(i, 1, GridLayout.UNDEFINED);
                    GridLayout.Spec columnSpec=GridLayout.spec(j, 1, GridLayout.UNDEFINED);
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
                    params.setGravity(Gravity.CENTER);
                    TextView values = new TextView(this);
                    values.setText(tuples.get(i).get(j));
                    values.setTextSize(15);
                    values.setWidth(gridMinWidth);
//                    values.setHeight(30);
                    values.setGravity(Gravity.CENTER);
                    values.setBackground(getResources().getDrawable(R.drawable.grid_frame));
                    gridLayout.addView(values, params);
                    gridLayout.setPadding(0,0,0,0);
                }
            }
        }
```

### 编译出错处理

用户的输入可能不符合语法，编译不通过，此时编译部分将无法匹配，也即无法得到结果。因此，返回值为空，此时直接退出，提示语法错误即可。

```java
                //Toast
                tip = "Please check your syntax!";
                Toast.makeText(MainActivity.this, tip, Toast.LENGTH_SHORT).show();
                editText.setText(editText.getText());
```

## 执行细节

在Analyze函数中，我们分别调用了下面的函数，去完成不同的执行工作。

### 创建源类

源类是最基础的类，只需要将数据按照数据结构设计的内容分别存储即可。

```java
//创建源类
    static boolean Create(){
        ClassStruct classStruct = new ClassStruct();
        classStruct.className = result.className;//className存放的是类名
        classStruct.selectClassName = null;
        classStruct.attrList = result.attrList;//实属性列表
        classStruct.virtualAttr = null;
        classStruct.condition = sql;//condition先不管他，先存起来，后面再分析
        classStruct.tupleNum = 0;
        classStruct.children = new ArrayList<>();
        return storeAPI.createClass(result.className,classStruct);
    }
```

值得一提的是，在存储表的时候，在执行部分我们只是调用了存储部分给的函数，并没有去维护这些表。创建表的工作由存储来完成。

### 创建代理类

源类在创建之初一定是空的，而代理类并不是这样。根据代理规则，代理类在创建之初，源类中符合代理规则的记录就将自动进入代理类中。因此，在创建代理类的时候，就需要找到源类，并根据代理规则进行第一次的更新。我们的更新操作是直接调用isSatisfy函数遍历所有源类中的记录，将符合条件的记录添加到代理类中。代理类中有这些记录，但是其虚属性是不保存的，而是由代理规则在查询的过程中实时计算出来。此时，我们不需要查询，因此代理规则并没有用到。由于代理规则的处理较为复杂，并且并不是全局需要的，我们在创建代理类的时候，并不直接存储代理规则。我们在代理规则部分依旧存储原语句，而在后续需要使用代理规则的时候，我们再重新调用编译部分的函数将其解析一次，以得到真正的代理规则。

```java
//创建代理类
    static boolean CreateSelectDeputy(){
        ClassStruct classStruct = new ClassStruct();
        classStruct.className = result.className;//类名
        classStruct.selectClassName = result.selectClassName;//源类名
        classStruct.children = new ArrayList<>();
        classStruct.condition = sql;//将sql放到condition中间，留作后面继续解析获得where属性
        classStruct.virtualAttr = result.attrNameList;//虚属性
        classStruct.attrList = result.attrList;//实属性
        if(storeAPI.existClass(classStruct.selectClassName)==false)return false;//如果不存在源类，就报错
        if(!storeAPI.createClass(result.className,classStruct))return false;//创建失败，则返回错误

        ClassStruct fa_classStruct = storeAPI.getClassStruct(result.selectClassName);//找到他们的父节点
        fa_classStruct.children.add(classStruct.className);//父节点的孩子里面加上类名
        Log.d("Hello", "CreateSelectDeputy: "+fa_classStruct.children.size());//记录日志，用于debug
        storeAPI.setClassStruct(fa_classStruct.className,fa_classStruct);

        //更新迁移
        storeAPI.initial(fa_classStruct.className);
        MEM.initial(classStruct.className);
        ArrayList<String > tuple = null;
        ArrayList<String> son_tuple = new ArrayList<>();
        son_tuple.add("-1");//pointers
        if(classStruct.attrList!=null)
            for(int i=0;i<classStruct.attrList.size();i++){
                son_tuple.add(classStruct.attrList.get(i).defaultVal);
            }//实属性如果有的话，直接就加上
        while((tuple=storeAPI.Next())!=null){
            int son_offset = -1;
            if(isSatisfy(result.where,fa_classStruct,tuple)){
                int offset = storeAPI.getOffset();
                son_tuple.set(0,Integer.toString(offset));
                MEM.insert(classStruct.className,son_tuple);
                son_offset = MEM.getOffset();//将满足条件的虚属性加上
            }
            ArrayList<String> tmp = storeAPI.decode(tuple.get(0));
            tmp.add(Integer.toString(son_offset));
            tuple.set(0,storeAPI.encode(tmp));
            storeAPI.update(tuple);
        }

        MEM.flushToDisk();
        return true;
    }
```

### 删除记录

删除操作是将记录删除的操作。但是在对象代理数据库中，如果一个类有自己的代理类，那么很可能这条记录被它的代理类代理了。删除这条记录的同时，也应当将代理类中的记录删除。

在执行部分，我们只考虑将块删除，对于系统表一致性的维护，由存储部分完成。

```java
//从源类中删除一个元组，并把孩子的该元组都删除 Return true Anyways!
    static boolean Delete(){
        ClassStruct classStruct = storeAPI.getClassStruct(result.className);
        //获取该类的所有结构，用于后面找孩子
        ArrayList<String> tuple = null;
        storeAPI.initial(result.className);
        while((tuple=storeAPI.Next())!=null){
            if(isSatisfy(result.where,storeAPI.getClassStruct(result.className),tuple)){
                //找孩子
                ArrayList<String> nexts = storeAPI.decode(tuple.get(0));
                for(int i=0;i<nexts.size();i++){
                    if(!nexts.get(i).equals("-1")) {
                        int offset = Integer.parseInt(nexts.get(i));
                        //获取对应的块
                        MEM.initial(classStruct.children.get(i),
                                offset/StoreAPI.PAGESIZE,offset%StoreAPI.PAGESIZE);
                        ArrayList<String> _t = MEM.Next();
                        MEM.delete();//将孩子也都删除
                    }
                }
                storeAPI.delete();//将记录删除
            }
        }
        MEM.flushToDisk();//将删完的刷到磁盘上去，也即将这块删除
        return true;
    }
```

### 删除类

删除类分为两种，一是删除源类，一是删除代理类。我们首先判断要删除的类是源类还是代理类，接着分别进行处理。

#### 删除源类

删除一个源类，就也要将其代理类都删除，其实就是删除了整棵树。

我们递归删除了整个由源类指向代理类的指针生成的树，实现了对一个类的删除。

```java
//删除源类,实质上会把整棵树删除
    static boolean Drop(String className){
        if(storeAPI.existClass(className)==false){return false;}//不存在，则返回错误

        ClassStruct classStruct = storeAPI.getClassStruct(className);
        storeAPI.dropClass(className);

        if(classStruct!=null)
            for(int i=0;i<classStruct.children.size();i++){
                Drop(classStruct.children.get(i));//依次递归删除各个节点
            }
        return true;
    }
```

#### 删除代理类

删除代理类和删除源类的区别在于，删除代理类除了要将它自己的孩子都删除外，还要将它的父节点指向它的指针也都删除。我们的做法是便利它父节点的所有孩子，找到属于他的信息，然后将之删除。接着就把它当做源类，调用上面的删除源类的函数，完成接下来的删除处理。

```java
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
                    fa_classStruct.children.remove(index);//找到对应的index，就将他删除
                    storeAPI.setClassStruct(fa_classStruct.className,fa_classStruct);
                    storeAPI.initial(fa_classStruct.className);
                    ArrayList<String> tuple = null;
                    while((tuple = storeAPI.Next())!=null){
                        ArrayList<String> pointer = storeAPI.decode(tuple.get(0));
                        pointer.remove(index);
                        tuple.set(0,storeAPI.encode(pointer));
                        storeAPI.update(tuple);//将删完的刷到磁盘上去
                    }
                    break;
                }
            }

        Drop(className);
        return true;
    }
```

### 插入

插入操作是一个较为复杂的操作，因为它存在这些问题：

- 编译部分获得的信息只有插入的类名，需要我们自己去获得类的属性表
- 插入的类型可能是非法的，我们需要进行类型检测
- 插入后，如果该类有代理类，而插入的语句符合代理规则，那么就需要将该记录同样插入到代理类中

因此，我们首先获得了要插入的类现有的类结构，同时将编译获取到的参数列表转换正确的形式。比如，对于所有的整型数据，我们在编译部分都是按照字符串存储的，现在需要将其转换成整型。此过程也检验插入数据是否合法，因为非法的数据是无法转换成功的。接着将合法的类型插入。

如果存在子类，我们则调用isSatisfy函数，判断其是否符合代理规则。如果符合代理规则，就将记录也插入子类中。在插入子类的时候，还要加上子类的实属性，由于此时没有给子类的实属性赋值，我们便插入了默认值。最后将执行结果刷进磁盘。

```java
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
```

### 更新

更新同样分为源类的更新和代理类的更新。

由于代理类的虚属性是实时计算出来的，其值依赖于源类的值。因此对代理类的虚属性更新是没有意义的。对于代理类，仅考虑对其实属性的更新。遍历代理类的属性列表和和要更新的属性的属性列表，如果属性名相同，就将更新的属性的计算结果赋值给代理类的属性列表，完成更新。

对于源类而言，除了要根据规则更新属性的值，还要考虑是否更新之后，代理类中的元组是否还满足代理规则，对于不满足代理规则的，应该删去，而满足代理规则的，则应该加入。我们的做法就是将子类中的所有记录都遍历一遍，分别进行判断。

```java
//return true Anyways
    static boolean Update()
    {
        //获取要插入的源类的类结构
        ClassStruct classStructOfSourceClass = storeAPI.getClassStruct(result.className);
        //start 代理类更新 （支持源类和代理类的更新）
        if(classStructOfSourceClass.selectClassName!=null&&!classStructOfSourceClass.selectClassName.equals("")){
            //deputy class update 只考虑对代理类实属性的更新
            storeAPI.initial(classStructOfSourceClass.className);
            ArrayList<String> tuple = null;
            ArrayList<String> son_title = getTitle(classStructOfSourceClass);
            while((tuple=storeAPI.Next())!=null){
                if(isSatisfy(result.where,classStructOfSourceClass,tuple)){
                    ArrayList<String> l_tuple = compute(classStructOfSourceClass,tuple);//获取这个类的完整的结构
                    if(classStructOfSourceClass.attrList!=null)
                        for(int i=0;i<classStructOfSourceClass.attrList.size();i++){
                            for(int j=0;j<result.attrNameList.size();j++){
                                if(result.attrNameList.get(j).attrName.equals(classStructOfSourceClass.attrList.get(i).attrName)){
                                    tuple.set(i+1,tinyCompute(son_title,l_tuple,result.valueList.get(j)));//将计算的结果存进去
                                }
                            }
                        }
                    storeAPI.update(tuple);
                    //代理类更新就不在乎会不会有源类跟着变化了，tinyCompute里面有处理
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
                    for(int i = 0; i < sourcePointer.size(); i++)//在这里循环
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
```

### 查询

查询分为两种：

- 类间查询
- 跨类查询

我们分别对这两种情况分别进行了处理。

#### 类间查询

对类间的查询，我们首先需要获得要查询的属性的列表，然后遍历存储的各个记录，如果符合要求，就将其加入结果返回。

在这里需要区分一些源类和代理类的情况。对于源类而言，我们直接获取其所有记录即可。而对于代理类而言，我们需要先进行一次计算，以恢复其完整结构。因为虚属性的值是不直接存储的。

```java
    static ArrayList<ArrayList<String>> select(){
        ArrayList<ArrayList<String>> res = new ArrayList<>();
        if(!storeAPI.existClass(result.selectClassName))return res;
        ClassStruct classStruct = storeAPI.getClassStruct(result.selectClassName);
        ArrayList<String> title = new ArrayList<>();
        if(result.attrNameList!=null)
            for(int i=0;i<result.attrNameList.size();i++){
                if(result.attrNameList.get(i).attrRename!=null&&
                        !result.attrNameList.get(i).attrRename.equals(""))
                    title.add(result.attrNameList.get(i).attrRename);//代理类
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
```

#### 跨类查询

跨类查询通过起点的条件判断，来查询终点的记录。跨类查询较类间查询要复杂得多，因为我们需要通过指针去找结果，而不能直接通过计算来得到。

在我们的编译部分，我们只存储了起点和终点。我们也没有对起点和终点的类型进行区分。因此，我们首将跨类查询分为了三类：

- 源类->代理类
- 代理类->源类
- 代理类->代理类

##### 源类->代理类

对于源类->代理类的情况，我们首先找到查询的代理类是源类的第几个孩子，通过孩子索引到查找的内容在终点中的位置，然后将结果从磁盘中读出返回。

```java
int childIndex = 0;
            for(int i=0;i<start_point.children.size();i++){
                if(start_point.children.get(i).equals(end_point.className)){
                    childIndex = i;//找到代理类的索引号
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
```

##### 代理类->源类

从代理类向源类的跨类查询比从源类向代理类的查询简单一些，因为一个代理类只有一个源类，而一个源类可以有很多的代理类。所以判断完条件之后直接从块的开始处开始读取即可，其他操作与源类查询代理类一致。

```java
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
```

##### 代理类->代理类

从代理类到代理类的跨类查询是前两个查询的综合。我们首先根据他们之间的联系——他们共同的父节点找到他们的索引关系，然后再依次索引得到查询结果。

```java
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
```

# 存储

我们在存储部分的实现较为复杂，并且与数据库实现关系并不是很密切。为了防止偏离主题，在这里我将介绍一种较为简单的实现，仅供参考。

## 存储数据结构

在存储部分，我们定义了如下的数据结构：

```java
public class StoreAPI {
    private MainActivity mainActivity;					// 来自安卓的mainactivity，主要用来打开文件Input、Output流
    private SharedPreferences classIDView;				// 记录所有的已经存在的类
    private SharedPreferences.Editor classIDEditor;		// 修改已经有的类，结果作用在classIDView上
    private SharedPreferences classStructView;			// 记录所有的已经存在类的内容
    private SharedPreferences.Editor classStructEditor;	// 修改已经有的类的内容，结果作用在classStructEditor
    private int m_blockNum,m_blockOffset;				// 我们每次初始化StoreAPI的时候，都有是通过一个类进行初始化，这里面存储的是该类当前访问到的地址偏移
    private String m_className;							// 记录初始化该StoreAPI的类名
    private ArrayList<String> buffer;					// buffer用来存储该类相关的内容
    private int mode_none_null,is_dirty;				// 指向buffer，是否被写了，用于存储时的策略

    //the number of tuples in a PAGE;
    public static final int PAGESIZE = 32;				// 一页最多的tuple数量
    }
```

## 硬盘存储接口

我们采用了一种比较简单的存储方式，将每个类都存储在一个文件中，通过文件指针指向具体内容。规定每页最多的tuple数量为32，设置了一个8096比特的缓冲区用于取数据，以减小磁盘I/O。

为了方便执行部分的调用，我们封装了如下的这些函数。

### encode函数

执行部分的数据不能直接存到文件里面，需要进行简单的编码，即进行一些数据格式的转换，以便于文件的读取。

```java
    public String encode(ArrayList<String> data){	// 将即将存入的内容编码，之后将其写入磁盘
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
```

### decode函数

decode函数是encode函数的逆过程，将encode之后的数据再解码成正常的数据形式。

```java
    public ArrayList<String> decode(String code){		// 将从磁盘读取到的原始数据进行解析
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
```

### writeString函数

writeString函数能够向指定文件存储数据，利用Java内置的数据流函数。

```java
    public boolean writeString(String fileName,String data){	// 向指定文件存储数据，一般用在encode之后
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
```

### readString函数

readString函数能够从指定文件读取数据，按8069字节一个块来读取。

```java
    private String readString(String fileName){				// 从指定文件读取数据，一般用在decode之前
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
```

### saveClassStruct函数

saveClassStruct函数将类存储到磁盘中。输入参数className，是为了将数据写到对应的文件中。

```java
    private void saveClassStruct(String className,ClassStruct classStruct){	// classStruct的元数据存储到classStruct中
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
```

### getClassStruct函数

getClassStruct函数可以通过类名获取类信息，在执行部分曾多次调用。

```java
    public ClassStruct getClassStruct(String className){		// classStruct元数据的获取，来自classStruct
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
```

### createClass函数

createClass函数是对save的封装，多了一步操作磁盘，建立属于该类的第一个磁盘小分块。

```java
    public boolean createClass(String className,ClassStruct classStruct){	
        if(regClass(className)==false)return false;
        classStruct.tupleNum = 0;
        classStruct.className = className;
        saveClassStruct(className,classStruct);
        writeString(className+"_0","");
        return true;
    }
```

### dropClass函数

dropClass可以删除类，也就是修改类表，并且删除对应的文件。

```java
    public boolean dropClass(String className){								// 如果初始化storeAPI的类恰恰为要删除的类，防止以后将其同步到磁盘，先同步到磁盘再将其删除
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
```

### initial函数

initial函数能够重新初始化storeAPI，先将原来的类buffer全部同步到磁盘，然后将新初始类的读写偏移改为0或改为指定值。

```java
public void initial(String className){									// 重新初始化storeAPI，先将原来的类buffer全部同步到磁盘，然后将新初始类的读写偏移改为0
        flushToDisk();
        m_className = className;
        m_blockNum = m_blockOffset = 0;
        buffer = null;
        mode_none_null = 1;
        is_dirty = 0;
    }
    public void initial(String className, int _bn,int _bo){					// 指定了读写偏移
        flushToDisk();
        m_className = className;
        m_blockNum = _bn;
        m_blockOffset = _bo;
        buffer = null;
        mode_none_null = 1;
        is_dirty = 0;
    }
```

### Next函数

Next函数能够获取下一个该class的文件块。如果正好处于块的头部或者尾部，还需要计算块的位置。

```java
    public ArrayList<String> Next(){										// 获取下一个该class的文件块
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
```

### insert函数

insert函数能够插入元组，也就是更新该类的数据。直接插就好。

```java
    public void insert(String className,ArrayList<String> tuple){			// 插入元组，也就是更新该类的数据
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
            buffer.add(encode(tuple));
            m_blockOffset++;
            is_dirty = 1;
            return;
        }
        buffer.set(m_blockOffset-1,encode(tuple));
        is_dirty = 1;
        return;
    }
```

### update函数

update函数能够更新元组，直接写就好了。

```java
    public void update(ArrayList<String> tuple){								// 直接写，写之前要确定好offset位置
        buffer.set(m_blockOffset-1,encode(tuple));
        is_dirty = 1;
    }
```

### delete函数

delete函数能够删除元组，就直接将当前位置置为空串就好。

```java
    public void delete(){														// 直接删
        buffer.set(m_blockOffset-1,"");
        is_dirty = 1;
    }
```

# 备注

下面是一些备注事项。

## 未来工作

此系统由武汉大学弘毅学堂计算机方向2017级本科生李蕴哲、李沁遥、郑晖、朱赫、谢宇涛基于武汉大学弘毅学堂2016级本科生赵鹏翔，田雅婷，吉凯，关焕康的系统完善实现。

最初实现的版本，很好的完成了系统的功能，但是也存在一些缺陷。比如支持的SQL语句不够规范、存储方式过于简单等。针对对象代理数据库支持的最新版本的SQL语句，我们重做了编译模块，并去除了一些冗余的规则。在编译部分，我们也做了很大的改动，我们不再按类存放，而是真正维护了几个系统表。在执行部分需要获取信息的时候，从系统表中取数据而非从一个个以类名为名字的文件中取数据，这种做法虽然较为复杂，但是可以有效提高资源利用效率。为了减少过多的文件带来的垃圾问题，我们采用了虚拟磁盘的技术，使整个系统都只维护一个文件。

但是我们的实现也存在一些问题，比如，我们依旧没有实现双向指针表，因此在执行部分也是通过遍历存储块来找匹配的名字的方式来实现更新。这是因为我们对于双向指针表的必要性产生怀疑，如此复杂的一个数据结构真的有必要吗？由于最初设计的问题，对于某些语句的实现我们也过于简单，这也是还需要改进的地方。

本文给出的存储部分仅仅是一个较为简单的实现，因此也没有维护系统表。

## 版权申明

作者：李蕴哲

联系方式: liyunzhe@whu.edu.cn

版权声明: Copyright (c) 2020 Li Yunzhe 
