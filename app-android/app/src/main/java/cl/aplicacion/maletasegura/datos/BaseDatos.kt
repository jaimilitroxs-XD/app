package cl.aplicacion.maletasegura.datos

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Base SQLite: tablas usuarios, maletas y eventos
object BaseDatos {
    lateinit var db: SQLiteDatabase
        private set

    fun iniciar(context: Context) {
        db = Ayudante(context).writableDatabase
    }

    private class Ayudante(context: Context) : SQLiteOpenHelper(context, "maleta_segura.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE usuarios (
                    correo TEXT PRIMARY KEY,
                    nombre TEXT NOT NULL,
                    sal    BLOB NOT NULL,
                    hash   BLOB NOT NULL)"""
            )
            db.execSQL(
                """CREATE TABLE maletas (
                    correo    TEXT NOT NULL,
                    direccion TEXT NOT NULL,
                    nombre    TEXT NOT NULL,
                    PRIMARY KEY (correo, direccion))"""
            )
            db.execSQL(
                """CREATE TABLE eventos (
                    id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    correo  TEXT NOT NULL,
                    momento INTEGER NOT NULL,
                    tipo    TEXT NOT NULL,
                    titulo  TEXT NOT NULL,
                    detalle TEXT NOT NULL,
                    maleta  TEXT NOT NULL)"""
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, versionAntigua: Int, versionNueva: Int) {
        }
    }
}
