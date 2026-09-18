package ch.ncavallini.polywear.data

import ch.ncavallini.polywear.data.dto.LessonDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * eduapp schedule gateway. Base URL: https://eduapp.ethz.ch/
 *
 * Auth (session cookie) is added by
 * [ch.ncavallini.polywear.auth.AuthInterceptor]. The endpoint returns a
 * top-level JSON array of lessons.
 */
interface ScheduleApi {
    @GET("web/gw/schedule")
    suspend fun getSchedule(@Query("semkez") semkez: String): List<LessonDto>
}
