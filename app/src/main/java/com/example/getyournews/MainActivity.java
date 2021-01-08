package com.example.getyournews;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    //declaring all variables initially

    ArrayList<String> titles=new ArrayList<>() ;
    ArrayList<String> content=new ArrayList<>() ;
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesdb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initializing the variables in onCreate method
        ListView listView= (ListView)findViewById(R.id.ListView);

        arrayAdapter=new ArrayAdapter(this , android.R.layout.simple_list_item_1,titles);  //adapter to connect the data with UI component
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent=new Intent(getApplicationContext(), Article.class) ;
                intent.putExtra("content",content.get(i));
                startActivity(intent);
            }
        });

//
        articlesdb = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
//        //creating database table name-article
        articlesdb.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER,title VARCHAR,content VARCHAR)");
        updatelistView();



        DownloadTask d=new DownloadTask(); //object of class DownloadTask

        try{

            d.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        }catch (Exception e){

            Log.i("Error","Cant retrieve3") ;
            e.printStackTrace();

        }



    }
    public void updatelistView(){
        Cursor c= articlesdb.rawQuery("SELECT * FROM articles",null);
        int contentindex=c.getColumnIndex("content");
        int titleIndex =c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            content.clear();
            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentindex));

            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }

    }

    //Setting up the download Task to download content from the web (id numbers of top stories)
    public class DownloadTask extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... strings) {
            String result=""; //variable to store the results which are downloaded
            URL url;
            HttpURLConnection urlConnection=null;
            try {
                url=new URL(strings[0]);  //getting the url
                urlConnection= (HttpURLConnection) url.openConnection(); //establishing the connection with http
                InputStream is=urlConnection.getInputStream(); //is acts as a channel
                InputStreamReader rd=new InputStreamReader(is); //reading the channel
                int data=rd.read();  //loop through the characters
                while(data !=-1)
                {
                    char curr=(char) data;  //convert data to character
                    result +=curr; //append each character in result
                    data=rd.read() ;


                }
               // Log.i("URLContent",result);
                JSONArray jsonArray=new JSONArray(result);
                int numberofitems=20;
                if(jsonArray.length()<20){
                    numberofitems=jsonArray.length();
                }
                //clear data if any
                articlesdb.execSQL("DELETE from articles");
                for(int i=0;i<numberofitems;i++) //extracting indivual sites through apis
                {
                    String article=jsonArray.getString(i);
                    url=new URL("https://hacker-news.firebaseio.com/v0/item/"+article+".json?print=pretty");
                    urlConnection=(HttpURLConnection) url.openConnection();
                    is=urlConnection.getInputStream();
                    rd=new InputStreamReader(is);
                    data=rd.read();
                    String articleinfo="";
                    while(data!=-1){
                        char current=(char) data;
                        articleinfo+=current;
                        data=rd.read();


                    }
                    //Log.i("info",articleinfo);
                    //extracting the title and url of the news from the api
                    JSONObject jsonObject=new JSONObject(articleinfo);
                    //checking if the object has url and title both or not only then print
                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String title_article=jsonObject.getString("title");
                        String articleURL= jsonObject.getString("url");
                        url=new URL(articleURL);  //content of actual article
                        urlConnection=(HttpURLConnection) url.openConnection();
                        is=urlConnection.getInputStream();
                        rd=new InputStreamReader(is);
                        data=rd.read();
                        String articleContent="";
                        while(data!=-1){
                            char current=(char) data;
                            articleContent+=current;
                            data=rd.read();


                        }
                        //Log.i("info",articleContent);
                        String sql="INSERT INTO articles(articleId, title, content) VALUES(?, ?, ?)" ;
                        SQLiteStatement statement =articlesdb.compileStatement(sql);
                        statement.bindString(1,article);
                        statement.bindString(2,title_article);
                        statement.bindString(3,articleContent);

                        statement.execute();


                    }

                }

            } catch (MalformedURLException e) { //catch clause if url is malformed i.e it is not right
                Log.i("Error","Cant retrieve1") ;
                e.printStackTrace();
            } catch (IOException e) {
                Log.i("Error","Cant retrieve2") ;////catch if there is some problem in catching the url
                e.printStackTrace();
            } catch (JSONException e) {
                Log.i("Error","Cant retrieve2") ;
                e.printStackTrace();
            }


            return null;
        }
        @Override
        protected void onPostExecute(String s){
            super.onPostExecute(s);
            updatelistView();

        }
    }
}
