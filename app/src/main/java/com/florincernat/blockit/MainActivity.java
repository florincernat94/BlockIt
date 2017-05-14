package com.florincernat.blockit;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    SQLiteDatabase blackList;
    SQLiteDatabase forbiddenMessages;
    SQLiteDatabase logs;
    //SQLiteDatabase receivers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        blackList = this.openOrCreateDatabase("Black List",MODE_PRIVATE,null);
        logs=this.openOrCreateDatabase("Logs",MODE_PRIVATE,null);
        forbiddenMessages = this.openOrCreateDatabase("Forbidden Messages",MODE_PRIVATE,null);
        blackList.execSQL("CREATE TABLE IF NOT EXISTS blackList( forbiddenNumber varchar primary key )");
        forbiddenMessages.execSQL("CREATE TABLE IF NOT EXISTS forbiddenMessages( message varchar )");
        logs.execSQL("CREATE TABLE IF NOT EXISTS logs (number varchar,reason varchar)");

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
            case R.id.main_id:
                setContentView(R.layout.activity_main);
                return true;
            case R.id.manage_black_list:
                setContentView(R.layout.black_list_manager);
                return true;
            case R.id.exit:
                finish();
                return true;
        }
        return true;
    }
    public void backButton(View v){
        setContentView(R.layout.black_list_manager);
    }

    public void addBlockedNumber(View v){
        EditText numberToBlock = (EditText) findViewById(R.id.add_number_to_block);
        String number = numberToBlock.getText().toString();
        if(!(number.equals(""))) {
            blackList.execSQL("INSERT INTO blackList values(\"" + number + "\")");

        }

    }


    public void removeBlockedNumber(View v){
        EditText numberToUnblock = (EditText) findViewById(R.id.remove_blocked_number);
        String number = numberToUnblock.getText().toString();
        blackList.execSQL("DELETE from blackList where forbiddenNumber=\""+number+"\"");

    }

    public void printBlockedNumbers(View v){
        setContentView(R.layout.black_list);
        final List<String> blockedNumbersList = new ArrayList<String>();
        Cursor cursor = blackList.rawQuery("SELECT forbiddenNumber FROM blackList",null);
        //indexul numerelor din blackList
        int numberIndex = cursor.getColumnIndex("forbiddenNumber");
        //merg la prima linie din db
        cursor.moveToFirst();
        //daca avem rezultate
        if(cursor.getCount()>0){
            do{ //cat timp sunt numere in db, le pun in arraylist si le afisez cu listview
                String number=cursor.getString(numberIndex);
                blockedNumbersList.add(number);

            }while(cursor.moveToNext());

        }
        cursor.close();
        final ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 ,blockedNumbersList);
        ListView listView = (ListView) findViewById(R.id.blocked_numbers_listView_button);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                blackList.execSQL("DELETE from blackList where forbiddenNumber=\""+blockedNumbersList.get(i)+"\"");
                blockedNumbersList.remove(i);
                adapter.notifyDataSetChanged();

                return false;
            }
        });

    }
    public void addForbiddenMessage(View v){
        EditText numberToBlock = (EditText) findViewById(R.id.forbidden_message);
        String message = numberToBlock.getText().toString();
        if(!(message.equals(""))){
            forbiddenMessages.execSQL("INSERT INTO forbiddenMessages values(\""+message+"\")");
        }



    }

    public void printForbiddenMessages(View v){
        setContentView(R.layout.forbidden_messages);
        final List<String> forbiddenMessagesList = new ArrayList<String>();
        //forbiddenMessages.execSQL("CREATE TABLE IF NOT EXISTS forbiddenMessages"+"( message varchar )");
        Cursor cursor = forbiddenMessages.rawQuery("SELECT message FROM forbiddenMessages",null);
        //indexul mesajelor din forbiddenMessages
        int messageIndex = cursor.getColumnIndex("message");
        //merg la prima linie din db
        cursor.moveToFirst();
        //daca avem rezultate
        if(cursor.getCount()>0){
            do{ //cat timp sunt numere in db, le pun in arraylist si le afisez cu listview
                String message=cursor.getString(messageIndex);
                forbiddenMessagesList.add(message);

            }while(cursor.moveToNext());

        }
        cursor.close();

        final ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, forbiddenMessagesList);

        ListView listView = (ListView) findViewById(R.id.forbidden_messages_list_view);
        listView.setAdapter(adapter);
        //ascult long clickuri pentru stergere
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                forbiddenMessages.execSQL("DELETE FROM forbiddenMessages WHERE message=\""+forbiddenMessagesList.get(i)+"\"");
                forbiddenMessagesList.remove(i);
                adapter.notifyDataSetChanged();

                return false;
            }
        });
    }
    //activez/dezactivez sms receiverul
    public void setSmsReceiver(View v){
        Switch smsSwitch=(Switch) findViewById(R.id.sms_switch);
        smsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SmsReceiver.BLOCK_SMS=isChecked;
                if(isChecked)
                    Toast.makeText(MainActivity.this, "Am activat  SMS Block", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "Am dezactivat  SMS Block", Toast.LENGTH_SHORT).show();
            }
        });

    }
    public void setUnknownSmsReceiver(View v){
        Switch unknownSmsSwitch=(Switch) findViewById(R.id.unknown_sms_switch);
        unknownSmsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SmsReceiver.BlOCK_UNKNOWN_SMS=isChecked;
                if(isChecked)
                    Toast.makeText(MainActivity.this, "Am activat Unknown SMS Block", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "Am dezactivat Unknown SMS Block", Toast.LENGTH_SHORT).show();
            }
        });

    }
    public void setAnonymousSmsReceiver(View v){
        Switch anonymousSmsSwitch=(Switch) findViewById(R.id.anonymous_sms_switch);
        anonymousSmsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SmsReceiver.BLOCK_ANONYMOUS_SMS=isChecked;
                if(isChecked)
                    Toast.makeText(MainActivity.this, "Am activat Anonymous SMS Block", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "Am dezactivat Anonymous SMS Block", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void setUnknownCallReceiver(View v){
        Switch UnknownCallSwitch=(Switch) findViewById(R.id.unknown_calls_switch);
        UnknownCallSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CallReceiver.BLOCK_UNKNOWN=isChecked;
                if(isChecked)
                    Toast.makeText(MainActivity.this, "Am activat Unknown Calls Block", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "Am dezactivat Unknown Calls Block", Toast.LENGTH_SHORT).show();
            }
        });

    }
    public void setCallReceiver(View v){
        Switch CallSwitch=(Switch) findViewById(R.id.calls_switch);
        CallSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CallReceiver.BLOCK_CALLS=isChecked;
                if(isChecked)
                    Toast.makeText(MainActivity.this, "Am activat Calls Block", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "Am dezactivat Calls Block", Toast.LENGTH_SHORT).show();
            }
        });

    }
    public void setAnonymousCallReceiver(View v){
        Switch AnonymousCallSwitch=(Switch) findViewById(R.id.anonymous_calls_switch);
        AnonymousCallSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CallReceiver.BLOCK_ANONYMOUS=isChecked;
                if(isChecked)
                    Toast.makeText(MainActivity.this, "Am activat Anonymous Calls Block", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "Am dezactivat Anonymous Calls Block", Toast.LENGTH_SHORT).show();
            }
        });

    }
    public void printLogs(View v){
        setContentView(R.layout.logs);
        final List<String[]> logsList = new LinkedList<String[]>();
        Cursor cursor = logs.rawQuery("SELECT number,reason FROM logs",null);

        cursor.moveToFirst();
        //daca avem rezultate
        if(cursor.getCount()>0){
            do{
                logsList.add(new String[]{cursor.getString(0),cursor.getString(1)});

            }while(cursor.moveToNext());

        }
        cursor.close();
        ListAdapter listAdapter = new ArrayAdapter<String[]>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                logsList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {


                View view = super.getView(position, convertView, parent);

                //text1 prima linive, text2 a doua linie
                String[] entry = logsList.get(position);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setText(entry[0]);
                text2.setText(entry[1]);
                return view;
            }
        };
       /* ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 ,logsNumberList);
*/
        ListView listView = (ListView) findViewById(R.id.logs_list_view);
        listView.setAdapter(listAdapter);


    }




}

