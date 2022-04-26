package com.example.myprovidertest;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private String newId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //添加数据
        Button addBtn = this.findViewById(R.id.add_data);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("content://com.example.databasetest.provider/book");
                System.out.println("uri --> "+uri);
                ContentValues values = new ContentValues();
                values.put("name", "It rains");
                values.put("author", "yxqyxq");
                values.put("pages", 100);
                values.put("price", 9.15);
                Uri newUri = getContentResolver().insert(uri, values);
                System.out.println("newUri --> "+newUri);
                newId = newUri.getPathSegments().get(1);
            }
        });

        //查询数据
        Button queryBtn = this.findViewById(R.id.query_data);
        queryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("content://com.example.databasetest.provider/book");
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndex("name"));
                        String author = cursor.getString(cursor.getColumnIndex("author"));
                        int pages = cursor.getInt(cursor.getColumnIndex("pages"));
                        double price = cursor.getDouble(cursor.getColumnIndex("price"));
                        Log.d(TAG, "book : name --> " + name + ", author --> " + author + ", pages --> " + pages + ", --> " + price);
                    }
                }
                cursor.close();
            }
        });

        //更新数据
        Button updateBtn = this.findViewById(R.id.update_data);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //为了不影响Book表中的其他数据，仅更新刚才添加的那条数据，这就是个例子，别想太多，下面的例子一样
                Uri uri = Uri.parse("content://com.example.databasetest.provider/book/" + newId);
                ContentValues values = new ContentValues();
                values.put("pages", 1000);
                getContentResolver().update(uri, values, null, null);
            }
        });

        //删除数据
        Button deleteBtn = this.findViewById(R.id.delete_data);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("content://com.example.databasetest.provider/book/" + newId);
                getContentResolver().delete(uri, null, null);
            }
        });
    }
}