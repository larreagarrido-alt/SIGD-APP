package pe.net.ws.kpiweb.features.kpi.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.kpi.model.KpiRouteItem;

/**
 * Gestor de persistencia SQLite local para las actividades de la Hoja de Ruta.
 * Permite inserción, consulta filtrada, actualización y eliminación de visitas comerciales.
 */
public class KpiRouteDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "kpi_route_sales.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_ROUTE = "route_activities";
    public static final String COL_ID = "id";
    public static final String COL_ACTIVITY_TYPE = "activity_type";
    public static final String COL_BUSINESS_NAME = "business_name";
    public static final String COL_CONTACT_PERSON = "contact_person";
    public static final String COL_PHONE = "phone";
    public static final String COL_MANUAL_ADDRESS = "manual_address";
    public static final String COL_DATE = "date_str";
    public static final String COL_DAY_NAME = "day_name";
    public static final String COL_START_TIME = "start_time";
    public static final String COL_END_TIME = "end_time";
    public static final String COL_COMMERCIAL_RESULT = "commercial_result";
    public static final String COL_OBSERVATIONS = "observations";
    public static final String COL_TIMESTAMP = "timestamp_created";

    private static KpiRouteDbHelper instance;

    public static synchronized KpiRouteDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new KpiRouteDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private KpiRouteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_ROUTE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ACTIVITY_TYPE + " TEXT, "
                + COL_BUSINESS_NAME + " TEXT, "
                + COL_CONTACT_PERSON + " TEXT, "
                + COL_PHONE + " TEXT, "
                + COL_MANUAL_ADDRESS + " TEXT, "
                + COL_DATE + " TEXT, "
                + COL_DAY_NAME + " TEXT, "
                + COL_START_TIME + " TEXT, "
                + COL_END_TIME + " TEXT, "
                + COL_COMMERCIAL_RESULT + " TEXT, "
                + COL_OBSERVATIONS + " TEXT, "
                + COL_TIMESTAMP + " INTEGER"
                + ");";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTE);
        onCreate(db);
    }

    public long insertItem(KpiRouteItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ACTIVITY_TYPE, item.getActivityType());
        cv.put(COL_BUSINESS_NAME, item.getBusinessName());
        cv.put(COL_CONTACT_PERSON, item.getContactPerson());
        cv.put(COL_PHONE, item.getPhone());
        cv.put(COL_MANUAL_ADDRESS, item.getManualAddress());
        cv.put(COL_DATE, item.getDate());
        cv.put(COL_DAY_NAME, item.getDayName());
        cv.put(COL_START_TIME, item.getStartTime());
        cv.put(COL_END_TIME, item.getEndTime());
        cv.put(COL_COMMERCIAL_RESULT, item.getCommercialResult());
        cv.put(COL_OBSERVATIONS, item.getObservations());
        cv.put(COL_TIMESTAMP, item.getTimestamp() > 0 ? item.getTimestamp() : System.currentTimeMillis());

        long id = db.insert(TABLE_ROUTE, null, cv);
        item.setId(id);
        return id;
    }

    public int updateItem(KpiRouteItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ACTIVITY_TYPE, item.getActivityType());
        cv.put(COL_BUSINESS_NAME, item.getBusinessName());
        cv.put(COL_CONTACT_PERSON, item.getContactPerson());
        cv.put(COL_PHONE, item.getPhone());
        cv.put(COL_MANUAL_ADDRESS, item.getManualAddress());
        cv.put(COL_DATE, item.getDate());
        cv.put(COL_DAY_NAME, item.getDayName());
        cv.put(COL_START_TIME, item.getStartTime());
        cv.put(COL_END_TIME, item.getEndTime());
        cv.put(COL_COMMERCIAL_RESULT, item.getCommercialResult());
        cv.put(COL_OBSERVATIONS, item.getObservations());

        return db.update(TABLE_ROUTE, cv, COL_ID + " = ?", new String[]{String.valueOf(item.getId())});
    }

    public int deleteItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_ROUTE, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<KpiRouteItem> getAllItems() {
        return queryItems(null, null);
    }

    public List<KpiRouteItem> getItemsByDay(String dayName) {
        if (dayName == null || dayName.isEmpty() || dayName.equalsIgnoreCase("Todos")) {
            return getAllItems();
        }
        return queryItems(COL_DAY_NAME + " LIKE ?", new String[]{"%" + dayName + "%"});
    }

    private List<KpiRouteItem> queryItems(String selection, String[] selectionArgs) {
        List<KpiRouteItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_ROUTE, null, selection, selectionArgs, null, null, COL_TIMESTAMP + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    KpiRouteItem item = new KpiRouteItem();
                    item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
                    item.setActivityType(cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTIVITY_TYPE)));
                    item.setBusinessName(cursor.getString(cursor.getColumnIndexOrThrow(COL_BUSINESS_NAME)));
                    item.setContactPerson(cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTACT_PERSON)));
                    item.setPhone(cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)));
                    item.setManualAddress(cursor.getString(cursor.getColumnIndexOrThrow(COL_MANUAL_ADDRESS)));
                    item.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)));
                    item.setDayName(cursor.getString(cursor.getColumnIndexOrThrow(COL_DAY_NAME)));
                    item.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_START_TIME)));
                    item.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_END_TIME)));
                    item.setCommercialResult(cursor.getString(cursor.getColumnIndexOrThrow(COL_COMMERCIAL_RESULT)));
                    item.setObservations(cursor.getString(cursor.getColumnIndexOrThrow(COL_OBSERVATIONS)));
                    item.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)));
                    list.add(item);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public int getCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ROUTE, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ROUTE, null, null);
    }
}
