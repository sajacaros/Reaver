package com.bnpinnovation.reaver.domain

import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import javax.persistence.*

@Entity
data class License(
        var totalUserCount: Int,
        var concurrentTalkCount: Int,
        var recordable: Boolean
) {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null

    var scarab: String = "http://"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companyId")
    var company: Company? = null

    @CreationTimestamp
    var created: LocalDateTime = LocalDateTime.now()
}