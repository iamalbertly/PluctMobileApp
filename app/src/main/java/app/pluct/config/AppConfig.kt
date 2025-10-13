package app.pluct.config

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.IOException

object AppConfig {
    private var _engineBase: String? = null
    private var _userId: String? = null
    
    val engineBase: String
        get() = _engineBase ?: throw IllegalStateException("AppConfig not initialized")
    
    val userId: String
        get() = _userId ?: throw IllegalStateException("AppConfig not initialized")
    
    fun initialize(context: Context) {
        try {
            val json = context.assets.open("pluct.json").bufferedReader().use { it.readText() }
            val config = JSONObject(json)
            
            _engineBase = config.getString("engineBase")
            _userId = config.getString("userId")
            
            Log.i("AppConfig", "Initialized with engineBase=$_engineBase, userId=$_userId")
        } catch (e: IOException) {
            Log.e("AppConfig", "Failed to load pluct.json", e)
            throw RuntimeException("Failed to load app configuration", e)
        }
    }
}
