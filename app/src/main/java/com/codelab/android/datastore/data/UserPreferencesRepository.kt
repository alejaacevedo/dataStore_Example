/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codelab.android.datastore.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.DataStore
import androidx.datastore.preferences.*
import kotlinx.coroutines.flow.*
import java.io.IOException

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val SORT_ORDER_KEY = "sort_order"

enum class SortOrder {
    NONE,
    BY_DEADLINE,
    BY_PRIORITY,
    BY_DEADLINE_AND_PRIORITY
}

data class UserPreferences(
    val showCompleted: Boolean,
    val sortOrder: SortOrder
)

/**
 * Class that handles saving and retrieving user preferences
 */
class UserPreferencesRepository(context: Context) {
    private val dataStore: DataStore<Preferences> = context.createDataStore(
        name = "user",
        migrations = listOf(SharedPreferencesMigration(context, USER_PREFERENCES_NAME))
    )

    private object PreferencesKeys {
        val SHOW_COMPLETED = preferencesKey<Boolean>("show_completed")
        val SORT_ORDER = preferencesKey<String>("sort_order")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
        .map { preferences ->
            val sortOrder = SortOrder.valueOf(preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.NONE.name)
            val showCompleted = preferences[PreferencesKeys.SHOW_COMPLETED] ?: false
            UserPreferences(showCompleted,sortOrder)
        }

    suspend fun updateShowCompleted(showCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_COMPLETED] = showCompleted
        }
    }

    private val sharedPreferences =
        context.applicationContext.getSharedPreferences(USER_PREFERENCES_NAME, Context.MODE_PRIVATE)


    suspend fun enableSortByDeadline(enable: Boolean) {
        // edit handles data transactionally, ensuring that if the sort is updated at the same
        // time from another thread, we won't have conflicts
        dataStore.edit { preferences ->
            // Get the current SortOrder as an enum
            val currentOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.NONE.name
            )

            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_PRIORITY) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_DEADLINE
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_PRIORITY
                    } else {
                        SortOrder.NONE
                    }
                }
            preferences[PreferencesKeys.SORT_ORDER] = newSortOrder.name
        }
    }

    suspend fun enableSortByPriority(enable: Boolean) {
        // edit handles data transactionally, ensuring that if the sort is updated at the same
        // time from another thread, we won't have conflicts
        dataStore.edit { preferences ->
            // Get the current SortOrder as an enum
            val currentOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.NONE.name
            )

            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_DEADLINE) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_PRIORITY
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_DEADLINE
                    } else {
                        SortOrder.NONE
                    }
                }
            preferences[PreferencesKeys.SORT_ORDER] = newSortOrder.name
        }
    }

}
