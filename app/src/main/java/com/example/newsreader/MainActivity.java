package com.example.newsreader;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> content = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        articleDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, title VARCHAR , content VARCHAR)");
       ////////////////////////////

        DownloadTask downloadTask = new DownloadTask();
        try {
            downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch (Exception e){
            //e.printStackTrace();
        }


        ListView listView = findViewById(R.id.listView);
        arrayAdapter  = new ArrayAdapter(this , android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);
            }
        });
        UpdatListView ();
    }

    public void UpdatListView (){
        Cursor c = articleDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex = c.getColumnIndex("content");
        int titleIndex     = c.getColumnIndex("title");
        if(c.moveToFirst()){
            content.clear();
            titles.clear();
            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            }
            while (c.moveToNext());
                arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask < String , Void ,  String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int  data = inputStreamReader.read();
                while(data!=-1) {
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }
                JSONArray jsonArray = new JSONArray(result);
                int noOfItem = 20;
                if(jsonArray.length()<20){
                    noOfItem = jsonArray.length();
                }
                articleDB.execSQL("DELETE FROM articles");   ///why??????(DOUBTS
                for(int i=0;i<noOfItem;i++){
                    ////
                    String articleID = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleID+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                     inputStream = urlConnection.getInputStream();
                     inputStreamReader = new InputStreamReader(inputStream);
                      data = inputStreamReader.read();
                      String Articleinfo = "";
                    while(data!=-1) {
                        char current = (char) data;
                        Articleinfo += current;
                        data = inputStreamReader.read();
                    }
                    JSONObject jsonObject = new JSONObject(Articleinfo);
                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        //////
                        String Title = jsonObject.getString("title");
                        String Articleurl = jsonObject.getString("url");

                        url =new URL(Articleurl);
                        urlConnection = (HttpURLConnection)url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        String ArticleContent = "" ;
                        while(data != -1){
                            char current = (char)data;
                            ArticleContent += current;
                            data = inputStreamReader.read();
                        }
//////////////////
                      // Log.i("AticleContent" , ArticleContent);

                        String sql = "INSERT INTO articles (articleid,title,content) VALUES (? , ? , ?)";
                        SQLiteStatement statement = articleDB.compileStatement(sql);
                        statement.bindString(1,articleID);
                        statement.bindString(2,Title);
                        statement.bindString(3,ArticleContent);
                        statement.execute();
                       //Log.i("TitleAndUrl", Title + Articleurl);
                    }
                }

               // Log.i("urls", result );
                return result;
            }
            catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            UpdatListView ();
        }
    }
}