package ch.ncavallini.polywear.di

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import ch.ncavallini.polywear.BuildConfig
import ch.ncavallini.polywear.auth.AuthInterceptor
import ch.ncavallini.polywear.auth.TokenStore
import ch.ncavallini.polywear.complication.NextClassComplicationService
import ch.ncavallini.polywear.data.ScheduleApi
import ch.ncavallini.polywear.data.ScheduleCache
import ch.ncavallini.polywear.data.ScheduleRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Manual dependency container, built once in [ch.ncavallini.polywear.PolyApp]. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    val tokenStore = TokenStore(appContext)

    private val cache = ScheduleCache(appContext)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.HEADERS
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api: ScheduleApi = retrofit.create(ScheduleApi::class.java)

    private val complicationUpdater = ComplicationDataSourceUpdateRequester.create(
        context = appContext,
        complicationDataSourceComponent = ComponentName(appContext, NextClassComplicationService::class.java),
    )

    val scheduleRepository = ScheduleRepository(
        api = api,
        cache = cache,
        json = json,
        onDataUpdated = { complicationUpdater.requestUpdateAll() },
    )

    private companion object {
        const val BASE_URL = "https://eduapp.ethz.ch/"
    }
}
