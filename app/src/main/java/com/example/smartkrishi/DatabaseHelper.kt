package com.example.smartkrishi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class RecommendationRecord(
    val id: Int,
    val cropName: String,
    val soilPh: String,
    val soilType: String,
    val result: String,
    val date: String
)

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "SmartKrishi.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                mobile TEXT UNIQUE NOT NULL,
                village TEXT,
                password TEXT NOT NULL
            )""")
        db.execSQL("""
            CREATE TABLE recommendations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_mobile TEXT,
                crop_name TEXT,
                soil_ph TEXT,
                soil_type TEXT,
                result TEXT,
                date TEXT
            )""")
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS recommendations")
        onCreate(db)
    }

    // Fix #13: Removed all db.close() calls — SQLiteOpenHelper manages connection lifecycle

    fun registerUser(name: String, mobile: String, village: String, password: String): Boolean {
        return try {
            val v = ContentValues().apply {
                put("name", name); put("mobile", mobile)
                put("village", village); put("password", password)
            }
            val r = writableDatabase.insert("users", null, v)
            r != -1L
        } catch (e: Exception) { false }
    }

    fun loginUser(mobile: String, password: String): Boolean {
        val c = readableDatabase.rawQuery(
            "SELECT * FROM users WHERE mobile=? AND password=?", arrayOf(mobile, password))
        val ok = c.count > 0
        c.close()
        return ok
    }

    fun getUserName(mobile: String): String {
        val c = readableDatabase.rawQuery("SELECT name FROM users WHERE mobile=?", arrayOf(mobile))
        var n = "Farmer"
        if (c.moveToFirst()) n = c.getString(0)
        c.close()
        return n
    }

    fun getUserVillage(mobile: String): String {
        val c = readableDatabase.rawQuery("SELECT village FROM users WHERE mobile=?", arrayOf(mobile))
        var v = ""
        if (c.moveToFirst()) v = c.getString(0) ?: ""
        c.close()
        return v
    }

    fun saveRecommendation(mobile: String, crop: String, ph: String, soilType: String, result: String, date: String) {
        val v = ContentValues().apply {
            put("user_mobile", mobile); put("crop_name", crop)
            put("soil_ph", ph); put("soil_type", soilType)
            put("result", result); put("date", date)
        }
        writableDatabase.insert("recommendations", null, v)
    }

    fun getRecommendations(mobile: String): List<RecommendationRecord> {
        val list = mutableListOf<RecommendationRecord>()
        val c = readableDatabase.rawQuery(
            "SELECT * FROM recommendations WHERE user_mobile=? ORDER BY id DESC", arrayOf(mobile))
        if (c.moveToFirst()) do {
            list.add(RecommendationRecord(
                id       = c.getInt(c.getColumnIndexOrThrow("id")),
                cropName = c.getString(c.getColumnIndexOrThrow("crop_name")),
                soilPh   = c.getString(c.getColumnIndexOrThrow("soil_ph")),
                soilType = c.getString(c.getColumnIndexOrThrow("soil_type")),
                result   = c.getString(c.getColumnIndexOrThrow("result")),
                date     = c.getString(c.getColumnIndexOrThrow("date"))
            ))
        } while (c.moveToNext())
        c.close()
        return list
    }

    fun deleteRecommendation(id: Int) {
        writableDatabase.delete("recommendations", "id=?", arrayOf(id.toString()))
    }

    fun getRecommendationCount(mobile: String): Int {
        val c = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM recommendations WHERE user_mobile=?", arrayOf(mobile))
        var count = 0
        if (c.moveToFirst()) count = c.getInt(0)
        c.close()
        return count
    }
}
