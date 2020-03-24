package com.bnpinnovation.reaver.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.ZoneId

interface LicenseDto {
    data class Details(
            val id:Long,
            val totalUserCount: Int,
            val concurrentTalkCount: Int,
            val recordable: Boolean,
            val scarabLink: String,
            @JsonIgnore val created: LocalDateTime
    ) {
        val createdDate
            @JsonProperty("created") get() = created.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    data class Request (
        val totalUserCount: Int,
        val concurrentTalkCount: Int,
        val recordable: Boolean
    )
}
