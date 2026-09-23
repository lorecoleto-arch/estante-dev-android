package com.estante.app.ui.common

import androidx.annotation.StringRes
import com.estante.app.R
import com.estante.app.domain.ReadingStatus
import retrofit2.HttpException
import java.io.IOException

/** Tipos de erro que a interface sabe explicar ao usuário. */
enum class ErrorKind(@StringRes val messageRes: Int) {
    NO_CONNECTION(R.string.error_no_connection),
    SERVER(R.string.error_server),
    UNKNOWN(R.string.error_unknown),
}

fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is IOException -> ErrorKind.NO_CONNECTION // sem internet, DNS, timeout...
    is HttpException -> ErrorKind.SERVER
    else -> ErrorKind.UNKNOWN
}

@get:StringRes
val ReadingStatus.labelRes: Int
    get() = when (this) {
        ReadingStatus.WANT_TO_READ -> R.string.status_want_to_read
        ReadingStatus.READING -> R.string.status_reading
        ReadingStatus.FINISHED -> R.string.status_finished
    }
