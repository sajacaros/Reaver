package com.bnpinnovation.reaver.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.ZoneId

interface CompanyDto{
    data class Details(
            val id:Long,
            val name: String,
            @JsonIgnore val created: LocalDateTime,
            @JsonIgnore val updated: LocalDateTime
    ) {
        val createdDate
            @JsonProperty("created") get() = created.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val updatedDate
            @JsonProperty("updated") get() = updated.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    data class Request (
        val name: String
    )
}
