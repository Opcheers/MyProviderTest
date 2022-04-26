# 内容提供器

内容提供器（content provider）主要用于在不同的程序之间实现数据共享，内容提供器可以选择只对哪一部分数据进行共享。

内容提供器一般有两种用法：

1.使用现有的内容提供器来读取和操作相应程序中的数据

2.创建自己的内容提供器给我们程序的数据提供外部访问接口

### 运行时权限

RuntimePermission

#### **1.普通权限**

不会直接威胁到用户的安全和隐私的权限，这部分权限系统会自动帮我们进行授权

使用普通权限时，在AndroidManifest.xml中添加权限声明就可以

#### **2.危险权限**

可能会触及到用户隐私，或者对设备安全性造成影响的权限，对于这部分用户要手动点击授权才可以，否则程序会无法使用相应功能

一共是9组24个，除此之外都是普通权限，使用危险权限时需要进行权限处理

用户一旦同意授权某个权限，，那么该权限所对应的权限组中的所有权限都会被授权

### 在程序运行时申请权限

#### 1.判断用户是否把权限给我们了

```java 
if(ContextCompat.checkSelfPermission(活动, 权限名)!=PackageManager.PERMISSION_GRANTED)
//相等就是给了，不相等就是没给
```

#### 2.没给就请求用户权限

```
ActivityCompat.requestPermissions（活动实例，权限名数组，请求码）
```

#### 3.重写onRequestPermissionsResult()

```Java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button makeCall = this.findViewById(R.id.make_call);

        makeCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //首先，判断用户是不是把权限给我们了
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED){
                    //不相等就说明没有授权,调用下面的方法请求用户权限

                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                    //第一个参数是Activity实例， 第二个参数是String数组，把要申请的权限名放在数组中即可，第三个是请求码，只要是唯一值就行
                } else {
                    //相等就打电话
                    call();
                }

            }
        });
    }

    private void call() {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:10086"));
            startActivity(intent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * 最后重写onRequestPermissionsResult
     * 授权的结果就封装在grantResults中
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    call();
                } else {
                    Toast.makeText(this, "您已拒绝授权", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
}
```

### 访问其他程序中的数据

#### ContentResolver基本用法

**1.拿到内容URI字符串**

标准URI格式 content://包名.provider/表名

```
content://com.example.app.provider/table1	//表示查询table1中所有数据
content://com.example.app.provider/table2/1    //表示查询table2中ID为1的记录
content://com.example.app.provider/table2/1/word    //查table2中ID为1的记录的word字段

使用通配符的Uri，还要借助UriMatcher这个类实现匹配内容Uri的功能
content://com.example.app.provider/*		//匹配任意表
content://com.example.app.provider/table1/# 	//匹配table1中任意一行数据的内容
```

**2.解析成Uri对象**

```java 
Uri uri = Uri.parse("content://com.example.app.provider/table1")
```

**3.查询表中数据**

```java
Cursor cursor = getContentResolver().query(
                uri, //from table_name
                projection, //select column1, column2
                selection, //while column = value
                selectionArgs, 
                sortOrder);//order by column1, column2
```

**4.通过移动游标的位置来遍历Cursor**

```Java
if (cursor != null){
	while (cursor.moveToNext()) {
		String column1 = cursor.getString(cursor.getColumnIndex("column1"));
		int column2 = cursor.getInt(cursor.getColumnIndex("column2"));
	}
	cursor.close();//得关上，别忘了
}
```

**5.其他操作**

```Java
//添加数据
ContentValues values = new ContentValues();
values.put("column1", "text");
values.put("column2", 1);
getContentResolver.insert(uri, values);

//更新数据
ContentValues values = new ContentValues();
values.put("column1", "");
getContentResolver.update(uri, values, "column = ? and column2 = ?", new String[]{"text", "1"});

//删除数据
getContentResolver.delete(uri, "column2 = ?", new String[]{"1"});
```

#### 创建内容提供器的步骤

**1.定义自己的内容提供器类继承ContentProvider**

```Java
public class DatabaseProvider extends ContentProvider {

    public final static int BOOK_DIR = 0;//访问book的所有数据
    public final static int BOOK_ITEM = 1;//访问book的单条数据
    public final static int CATEGORY_DIR = 2;//访问category的所有数据
    public final static int CATEGORY_ITEM = 3;//访问category的单条数据

    public final static String AUTHORITY = "com.example.databasetest.provide";
    private static UriMatcher uriMatcher;
    private MyDatabaseHelper dbHelper;

    /**
     * uriMatcher.addURI三个参数：authority, path, 自定义代码
     * 当调用uriMatcher.match(uri)方法时，返回某个能匹配这个Uri对象所对应的自定义代码
     * 利用这个代码可以判断出调用方期望访问的是哪张表的数据了
     */
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "book", BOOK_DIR);
        uriMatcher.addURI(AUTHORITY, "bool/#", BOOK_ITEM);
        uriMatcher.addURI(AUTHORITY, "category", CATEGORY_DIR);
        uriMatcher.addURI(AUTHORITY, "category/#", CATEGORY_ITEM);
    }

    public DatabaseProvider() {
    }

    /**
     * 删除数据
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return 返回被删除的行数
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleteRow = 0;
        switch (uriMatcher.match(uri)){
            case BOOK_DIR:
                deleteRow = db.delete("Book", selection, selectionArgs);
                break;
            case BOOK_ITEM:
                String bookId = uri.getPathSegments().get(1);
                deleteRow = db.delete("Book", "id = ?", new String[]{bookId});
                break;
            case CATEGORY_DIR:
                deleteRow = db.delete("Category", selection, selectionArgs);
                break;
            case CATEGORY_ITEM:
                String categoryId = uri.getPathSegments().get(1);
                deleteRow = db.delete("Category", "id = ?", new String[]{categoryId});
                break;
            default:
                break;

        }
        return deleteRow;
    }

    /**
     * 插入数据
     * @param uri
     * @param values
     * @return 以新增数据id结尾的Uri
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Uri uriReturn = null;
        switch (uriMatcher.match(uri)){
            case BOOK_DIR:
            case BOOK_ITEM:
                long newBookId = db.insert("Book", null, values);
                uriReturn = Uri.parse("content://" + AUTHORITY + "/book/" + newBookId);
                break;
            case CATEGORY_DIR:
            case CATEGORY_ITEM:
                long categoryId = db.insert("Category", null, values);
                uriReturn = Uri.parse("content://" + AUTHORITY + "/category/" + categoryId);
                break;
            default:
                break;
        }
        return uriReturn;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new MyDatabaseHelper(getContext(), "BookStore.db", null, 1);
        return true;
    }

    /**
     * 数据查询
     * 其中，uri.getPathSegments()：
     *  将内容URI权限之后的部分以“/"符号进行分割，并把分割的结果放到一个字符串列表中，
     *  那么这个列表第0个位置就是路径，第一个位置就是id
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = null;
        switch (uriMatcher.match(uri)){
            case BOOK_DIR:
                cursor = db.query("Book", projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case BOOK_ITEM:
                String bookId = uri.getPathSegments().get(1);
                cursor = db.query("Book", projection, "id = ?", new String[]{bookId}, null, null, sortOrder);
                break;
            case CATEGORY_DIR:
                cursor = db.query("Category", projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case CATEGORY_ITEM:
                String categoryId = uri.getPathSegments().get(1);
                cursor = db.query("Category", projection, "id = ?", new String[]{categoryId}, null, null, sortOrder);
                break;
            default:
                break;
        }
        return cursor;
    }

    /**
     * 数据更新
     * @param uri
     * @param values
     * @param selection
     * @param selectionArgs
     * @return 返回受影响的行数
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int updateRows = 0;
        switch (uriMatcher.match(uri)){
            case BOOK_DIR:
                updateRows = db.update("Book", values, selection, selectionArgs);
                break;
            case BOOK_ITEM:
                String bookId = uri.getPathSegments().get(1);
                updateRows = db.update("Book", values, "id = ?", new String[]{bookId});
                break;
            case CATEGORY_DIR:
                updateRows = db.update("Category", values, selection, selectionArgs);
                break;
            case CATEGORY_ITEM:
                String categoryId = uri.getPathSegments().get(1);
                updateRows = db.update("Category", values, "id = ?", new  String[]{categoryId});
                break;
            default:
                break;
        }
        return updateRows;
    }
    
    /**
     * 获取Uri对象所对应的MIME类型
     * @param uri
     * @return
     */
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            case BOOK_DIR:
                return "vnd.android.cursor.dir/vnd.com.example.databasetest.provider.book";
            case BOOK_ITEM:
                return "vnd.android.cursor.item/vnd.com.example.databasetest.provider.book";
            case CATEGORY_DIR:
                return "vnd.android.cursor.dir/vnd.com.example.databasetest.provider.category";
            case CATEGORY_ITEM:
                return "vnd.android.cursor.item/vnd.com.example.databasetest.provider.category";
            default:
                break;
        }
        return null;
    }
}
```

其中getType()是所有内容提供器必须提供的方法，用于获取Uri对象所对应的MIME类型，MIME 字符串主要由三部分组成：

![image-20220426111600950](C:\Users\83771\AppData\Roaming\Typora\typora-user-images\image-20220426111603168.png)

**2.在AndroidManifest.xml中注册这个ContentProvider，注册时需要绑定一个Uri**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.example.databasetest" >
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.DatabaseTest"
        tools:ignore="MissingClass" >
        <provider
            android:name=".DatabaseProvider"
            android:authorities="com.example.databasetest.provider"
            android:enabled="true"
            android:exported="true" > 
        </provider>
        <!-- 其他应用程序可通过该Uri来访问这个自定义content provider所暴露的数据 -->
		<!--enabled和exported为true表示允许DatabaseProvider被其他应用程序进行访问-->
        ...
        
    </application>
</manifest>
```
