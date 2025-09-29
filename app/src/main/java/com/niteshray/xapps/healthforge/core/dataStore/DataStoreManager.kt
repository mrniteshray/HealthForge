package com.niteshray.xapps.healthforge.core.dataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized place to manage all preference keys
 * This helps avoid typos and makes key management easier
 */
object PreferenceKey {
    // Auth related
    const val AUTH_TOKEN = "auth_token"
    const val USER_ID = "user_id"
    const val USER_EMAIL = "user_email"
    const val USER_NAME = "user_name"
    const val IS_LOGGED_IN = "is_logged_in"
    const val TOKEN_EXPIRY = "token_expiry"

    // App settings
    const val IS_FIRST_LAUNCH = "is_first_launch"
    const val THEME_MODE = "theme_mode"
    const val LANGUAGE = "language"
    const val NOTIFICATIONS_ENABLED = "notifications_enabled"

    // User preferences
    const val LAST_SYNC_TIME = "last_sync_time"
    const val USER_PREFERENCES = "user_preferences"
    const val SELECTED_CATEGORIES = "selected_categories"
}


// Extension property for Context to create DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class DataStore @Inject constructor(
    private val context: Context
)
{

    /**
     * Save String value
     */
    suspend fun saveString(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    /**
     * Save Int value
     */
    suspend fun saveInt(key: String, value: Int) {
        val prefKey = intPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    /**
     * Save Long value
     */
    suspend fun saveLong(key: String, value: Long) {
        val prefKey = longPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    /**
     * Save Float value
     */
    suspend fun saveFloat(key: String, value: Float) {
        val prefKey = floatPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    /**
     * Save Double value
     */
    suspend fun saveDouble(key: String, value: Double) {
        val prefKey = doublePreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    /**
     * Save Boolean value
     */
    suspend fun saveBoolean(key: String, value: Boolean) {
        val prefKey = booleanPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    /**
     * Save Set<String> value
     */
    suspend fun saveStringSet(key: String, value: Set<String>) {
        val prefKey = stringSetPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    // ==================== GET FUNCTIONS (Flow) ====================

    /**
     * Get String value as Flow
     */
    fun getString(key: String, defaultValue: String = ""): Flow<String> {
        val prefKey = stringPreferencesKey(key)
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[prefKey] ?: defaultValue
            }
    }

    /**
     * Get Int value as Flow
     */
    fun getInt(key: String, defaultValue: Int = 0): Flow<Int> {
        val prefKey = intPreferencesKey(key)
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[prefKey] ?: defaultValue
            }
    }

    /**
     * Get Long value as Flow
     */
    fun getLong(key: String, defaultValue: Long = 0L): Flow<Long> {
        val prefKey = longPreferencesKey(key)
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[prefKey] ?: defaultValue
            }
    }

    /**
     * Get Float value as Flow
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Flow<Float> {
        val prefKey = floatPreferencesKey(key)
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[prefKey] ?: defaultValue
            }
    }

    /**
     * Get Double value as Flow
     */
    fun getDouble(key: String, defaultValue: Double = 0.0): Flow<Double> {
        val prefKey = doublePreferencesKey(key)
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[prefKey] ?: defaultValue
            }
    }

    /**
     * Get Boolean value as Flow
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean> {
        val prefKey = booleanPreferencesKey(key)
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[prefKey] ?: defaultValue
            }
    }

    /**
     * Get Set<String> value as Flow
     */
    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Flow<Set<String>> {
        val prefKey = stringSetPreferencesKey(key)
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[prefKey] ?: defaultValue
            }
    }

    // ==================== GET FUNCTIONS (Suspend - Single Value) ====================

    /**
     * Get String value once (suspend function)
     */
    suspend fun getStringOnce(key: String, defaultValue: String = ""): String {
        return getString(key, defaultValue).first()
    }

    /**
     * Get Int value once (suspend function)
     */
    suspend fun getIntOnce(key: String, defaultValue: Int = 0): Int {
        return getInt(key, defaultValue).first()
    }

    /**
     * Get Long value once (suspend function)
     */
    suspend fun getLongOnce(key: String, defaultValue: Long = 0L): Long {
        return getLong(key, defaultValue).first()
    }

    /**
     * Get Float value once (suspend function)
     */
    suspend fun getFloatOnce(key: String, defaultValue: Float = 0f): Float {
        return getFloat(key, defaultValue).first()
    }

    /**
     * Get Double value once (suspend function)
     */
    suspend fun getDoubleOnce(key: String, defaultValue: Double = 0.0): Double {
        return getDouble(key, defaultValue).first()
    }

    /**
     * Get Boolean value once (suspend function)
     */
    suspend fun getBooleanOnce(key: String, defaultValue: Boolean = false): Boolean {
        return getBoolean(key, defaultValue).first()
    }

    /**
     * Get Set<String> value once (suspend function)
     */
    suspend fun getStringSetOnce(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return getStringSet(key, defaultValue).first()
    }

    // ==================== REMOVE FUNCTIONS ====================

    /**
     * Remove a specific key
     */
    suspend fun remove(key: String) {
        // We need to try all possible key types since we don't know the type
        context.dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(key))
            preferences.remove(intPreferencesKey(key))
            preferences.remove(longPreferencesKey(key))
            preferences.remove(floatPreferencesKey(key))
            preferences.remove(doublePreferencesKey(key))
            preferences.remove(booleanPreferencesKey(key))
            preferences.remove(stringSetPreferencesKey(key))
        }
    }

    /**
     * Remove multiple keys
     */
    suspend fun removeKeys(keys: List<String>) {
        context.dataStore.edit { preferences ->
            keys.forEach { key ->
                preferences.remove(stringPreferencesKey(key))
                preferences.remove(intPreferencesKey(key))
                preferences.remove(longPreferencesKey(key))
                preferences.remove(floatPreferencesKey(key))
                preferences.remove(doublePreferencesKey(key))
                preferences.remove(booleanPreferencesKey(key))
                preferences.remove(stringSetPreferencesKey(key))
            }
        }
    }

    /**
     * Clear all preferences
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // ==================== UTILITY FUNCTIONS ====================

    /**
     * Check if a key exists
     */
    suspend fun containsKey(key: String): Boolean {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences.contains(stringPreferencesKey(key)) ||
                        preferences.contains(intPreferencesKey(key)) ||
                        preferences.contains(longPreferencesKey(key)) ||
                        preferences.contains(floatPreferencesKey(key)) ||
                        preferences.contains(doublePreferencesKey(key)) ||
                        preferences.contains(booleanPreferencesKey(key)) ||
                        preferences.contains(stringSetPreferencesKey(key))
            }.first()
    }

    /**
     * Get all preferences as Map
     */
    fun getAllPreferences(): Flow<Map<String, Any?>> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences.asMap().mapKeys { it.key.name }
            }
    }

    /**
     * Get count of stored preferences
     */
    suspend fun getPreferencesCount(): Int {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences.asMap().size
            }.first()
    }

    // ==================== BACKUP & RESTORE ====================

    /**
     * Export all preferences as Map (for backup)
     */
    suspend fun exportPreferences(): Map<String, Any?> {
        return getAllPreferences().first()
    }

    /**
     * Import preferences from Map (for restore)
     */
    suspend fun importPreferences(preferencesMap: Map<String, Any?>) {
        context.dataStore.edit { preferences ->
            preferences.clear() // Clear existing preferences
            preferencesMap.forEach { (key, value) ->
                when (value) {
                    is String -> preferences[stringPreferencesKey(key)] = value
                    is Int -> preferences[intPreferencesKey(key)] = value
                    is Long -> preferences[longPreferencesKey(key)] = value
                    is Float -> preferences[floatPreferencesKey(key)] = value
                    is Double -> preferences[doublePreferencesKey(key)] = value
                    is Boolean -> preferences[booleanPreferencesKey(key)] = value
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        preferences[stringSetPreferencesKey(key)] = value as Set<String>
                    }
                }
            }
        }
    }
}
