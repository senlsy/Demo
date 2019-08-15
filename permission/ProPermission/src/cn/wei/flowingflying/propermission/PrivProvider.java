package cn.wei.flowingflying.propermission;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;


public class PrivProvider extends ContentProvider
{
    
    public static final String AUTHORITY="cn.wei.flowingflying.propermission.PrivProvider";
    public static final Uri CONTENT_URI=Uri.parse("content://" + AUTHORITY + "/counter");
    
    public static final String CONTENT_TYPE="vnd.android.cursor.dir/vnd.cn.flowingflying.privprovider";
    public static final String CONTENT_ITEM_TYPE="vnd.android.cursor.item/vnd.cn.flowingflying.privprovider";
    
    public static String[] colsName={"Number"};
    private static String[] entryOne={"1"};
    private static int counter=0;
    private MatrixCursor cursor=null;
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
    
    @Override
    public String getType(Uri uri) {
        return null;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }
    
    @Override
    public boolean onCreate() {
        return false;
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if(cursor == null){
            cursor=new MatrixCursor(colsName);
        }
        counter=100;
        entryOne[0]="" + counter;
        cursor.addRow(entryOne);
        return cursor;
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection,String[] selectionArgs) {
        return 0;
    }
}
