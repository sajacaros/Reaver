package com.bnpinnovation.reaver.domain

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
data class Company(
        val name: String
) {

        @Id @GeneratedValue(strategy = GenerationType.AUTO)
        var id: Long? = null
        var privateKeyPath: String = "filepath"
        var publicKeyPath: String = "filepath"
        var signedKeyPath: String? = null

        @OneToMany(fetch = FetchType.LAZY, mappedBy = "company")
        val _licenses = mutableListOf<License>()

        val liceses get() = Collections.unmodifiableList(_licenses.toList())
        fun addLicense(license: License) {
                _licenses.add(license)
                license.company = this
        }

        @CreationTimestamp
        val created: LocalDateTime = LocalDateTime.now()

        @UpdateTimestamp
        var updated: LocalDateTime = LocalDateTime.now()

        val latestLicense: License?
                get() = if(_licenses.isEmpty()) null else _licenses.last()
}
