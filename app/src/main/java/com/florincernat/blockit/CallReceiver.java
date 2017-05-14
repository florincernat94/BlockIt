package com.florincernat.blockit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Created by florin on 1/5/2017.
 */
public class CallReceiver extends BroadcastReceiver{
    static boolean BLOCK_CALLS=true; //in black list
    static boolean BLOCK_UNKNOWN=true;
    static boolean BLOCK_ANONYMOUS=true;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            SQLiteDatabase logs = context.openOrCreateDatabase("Logs",Context.MODE_PRIVATE,null);
            logs.execSQL("CREATE TABLE IF NOT EXISTS logs(number varchar,reason varchar)");

            // get the phone number
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if((BLOCK_CALLS)&&(isBlocked(context,incomingNumber))){
                try {
                    Toast.makeText(context, "Apel Blocat deoarece este in black list " + incomingNumber, Toast.LENGTH_LONG).show();
                    disconnectPhoneItelephony(context);
                    logs.execSQL("INSERT INTO logs values(\""+incomingNumber+"\",\"Apel blocat deoarece este adaugat in Blacklist\")");
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | RemoteException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            //daca este activat block unknown, iar numarul nu este in lista de contacte atunci il blocam
            else if ((BLOCK_UNKNOWN)&&(!isKnown(context,incomingNumber))){
                try {
                    Toast.makeText(context, "Apel Blocat deoarece nu este in contacte " + incomingNumber, Toast.LENGTH_LONG).show();
                    disconnectPhoneItelephony(context);
                    logs.execSQL("INSERT INTO logs values(\""+incomingNumber+"\",\"Apel Blocat deoarece nu este in contacte\")");
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | RemoteException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            else if((BLOCK_ANONYMOUS)&&(incomingNumber.equals("Unknown"))){
                try {
                    Toast.makeText(context, "Apel Blocat deoarece nu este anonim " + incomingNumber, Toast.LENGTH_LONG).show();
                    disconnectPhoneItelephony(context);
                    logs.execSQL("INSERT INTO logs values(\""+incomingNumber+"\",\"Apel Blocat deoarece este anonim\")");
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | RemoteException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void disconnectPhoneItelephony(Context context) throws ClassNotFoundException, NoSuchMethodException, RemoteException, InvocationTargetException, IllegalAccessException {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        Class clazz = Class.forName(telephonyManager.getClass().getName());
        Method method = clazz.getDeclaredMethod("getITelephony");
        method.setAccessible(true);
        ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
        telephonyService.endCall();
    }
    // caut in lista de contacte sa vad daca este
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
}
