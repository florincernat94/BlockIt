package com.florincernat.blockit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {
    public static boolean BLOCK_SMS=true; //din black list
    public static boolean BlOCK_UNKNOWN_SMS=true;
    public static boolean BLOCK_ANONYMOUS_SMS=true;
    @Override
    public void onReceive(Context context, Intent intent) {
        String smsIntent=intent.getAction();
        //get sms informations
        Bundle bundle = intent.getExtras();
        Object[] pdus = (Object[]) bundle.get("pdus");
        final SmsMessage[] messages = new SmsMessage[pdus.length];
        for (int i = 0; i < pdus.length; i++) {
            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
        }
        //numarul de la care s-a primit mesajul
        String number = messages[0].getOriginatingAddress();
        String message = messages[0].getMessageBody();
        if((intent != null)&&(smsIntent.equals("android.provider.Telephony.SMS_RECEIVED"))){
            SQLiteDatabase logs = context.openOrCreateDatabase("Logs",Context.MODE_PRIVATE,null);
            logs.execSQL("CREATE TABLE IF NOT EXISTS logs(number varchar,reason varchar)");
            //SQLiteDatabase blackList = context.openOrCreateDatabase("Black List", Context.MODE_PRIVATE, null);

            if((BLOCK_SMS)&&(isBlocked(context,number))) {
                //numarul este in baza de date la black list
                //il blocam
                Toast.makeText(context, "Mesaj Blocat", Toast.LENGTH_LONG).show();
                this.abortBroadcast();
                logs.execSQL("INSERT INTO logs values(\""+number+"\",\"Mesaj blocat deoarece este adaugat in Blacklist\")");


            }else if((BlOCK_UNKNOWN_SMS)){
                if(!isKnown(context,number)){
                    Toast.makeText(context, "Mesaj Blocat deoarece nu este in contacte", Toast.LENGTH_LONG).show();
                    abortBroadcast();
                    logs.execSQL("INSERT INTO logs values(\""+number+"\",\"Mesaj Blocat deoarece nu este in contacte\")");
                }

            }
            else if((BLOCK_ANONYMOUS_SMS)){
                if(number.equals("Unknown")){
                    Toast.makeText(context, "Mesaj Blocat deoarece este anonim", Toast.LENGTH_LONG).show();
                    abortBroadcast();
                    logs.execSQL("INSERT INTO logs values(\""+number+"\",\"Mesaj Blocat deoarece este anonim\")");
                }
            }else if(containsForbiddenWords(context,message)){
                Toast.makeText(context, "Mesaj Blocat datorita textului", Toast.LENGTH_LONG).show();
                this.abortBroadcast();
                logs.execSQL("INSERT INTO logs values(\""+number+"\",\"Mesaj Blocat datorita textului\")");
            }
        }
    }
    public boolean isKnown(Context context,String incomingNumber){
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(incomingNumber));
        String[] mPhoneNumberProjection = { ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER, ContactsContract.PhoneLookup.DISPLAY_NAME };
        Cursor cursor = context.getContentResolver().query(lookupUri,mPhoneNumberProjection, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return true;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return false;
    }
    public boolean isBlocked(Context context,String incomingNumber){
        SQLiteDatabase blackList = context.openOrCreateDatabase("Black List",Context.MODE_PRIVATE,null);
        Cursor cursor = blackList.rawQuery("SELECT forbiddenNumber from blackList WHERE forbiddenNumber=\""+incomingNumber+"\"",null);
        try {
            if (cursor.moveToFirst()) {
                return true;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return false;
    }
    public boolean containsForbiddenWords(Context context,String message){
        SQLiteDatabase forbiddenMessage = context.openOrCreateDatabase("Forbidden Messages", Context.MODE_PRIVATE, null);
        Cursor messagesCursor = forbiddenMessage.rawQuery("SELECT message from forbiddenMessages ", null);
        //indexul mesajelor din forbiddenMessages
        int messageIndex = messagesCursor.getColumnIndex("message");
        //merg la prima linie din db
        messagesCursor.moveToFirst();

        if (messagesCursor.getCount() > 0) {
            do {//daca mesajul contine mesaj interzis din baza de date il opreste
                String badMessage = messagesCursor.getString(messageIndex);
                if (message.contains(badMessage)) {
                    return true;
                }

            } while (messagesCursor.moveToNext());
        }
        messagesCursor.close();
        return false;
    }
}