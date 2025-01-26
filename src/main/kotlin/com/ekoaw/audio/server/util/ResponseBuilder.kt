package com.ekoaw.audio.server.util

import com.ekoaw.audio.server.model.response.ResponseModel

object ResponseBuilder {
    fun success(message: String = "success"): ResponseModel {
        return ResponseModel(status = "success", message = message)
    }

    fun error(message: String = "error"): ResponseModel {
        return ResponseModel(status = "error", message = message)
    }
}
