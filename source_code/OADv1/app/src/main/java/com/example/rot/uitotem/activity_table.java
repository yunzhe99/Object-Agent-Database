package com.example.rot.uitotem;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class activity_table extends AppCompatActivity{
    private TextView textView;
    private Button buttonReturn;
    private int gridMinWidth;
    private GridLayout gridLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);
        Intent intent = getIntent();
        String sql = intent.getStringExtra("sql");
        ArrayList<ArrayList<String>> tuples = (ArrayList<ArrayList<String>>) getIntent().getSerializableExtra("tuples");
        textView = (TextView)findViewById(R.id.text_view);
        gridLayout = (GridLayout)findViewById(R.id.grid_layout);
        gridLayout.removeAllViews();
        gridLayout.removeAllViewsInLayout();
        textView.setText(sql);
        //初始化表格的最小宽度
//        gridMinWidth = getScreenWidth()/15;
//        Log.e("tuples", String.valueOf(tuples));
        if(tuples.size() != 0){
            int column = tuples.get(0).size();
            int row = tuples.size();
            Log.d("row", String.valueOf(row));
            Log.d("column", String.valueOf(column));
            int gridMinWidth = getScreenWidth()/column - column+1;
            Log.d("screenwidth", String.valueOf(getScreenWidth()));
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
    }

    //获取屏幕大小，然后设置表格的宽度
    private int getScreenWidth(){
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }
}
