package com.ravibhaiya.quizzen.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** The one preferences file of the app. A DataStore file must have exactly one instance, so everything shares this. */
internal val Context.quizzenDataStore: DataStore<Preferences> by preferencesDataStore(name = "quizzen_settings")
