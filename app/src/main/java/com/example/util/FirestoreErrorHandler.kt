package com.example.util

import android.util.Log

enum class OperationType {
    GET,
    LIST,
    CREATE,
    UPDATE,
    DELETE
}

fun handleFirestoreError(e: Exception, operationType: OperationType, path: String) {
    Log.e(
        "FirestoreError",
        """
        {
          "error": "${e.javaClass.simpleName}",
          "message": "${e.message?.replace("\"", "\\\"")}",
          "operation": "$operationType",
          "path": "$path"
        }
        """.trimIndent(),
        e
    )
}
