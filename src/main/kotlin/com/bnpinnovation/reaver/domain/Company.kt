package com.bnpinnovation.reaver.domain

import com.bnpinnovation.reaver.service.KeyPairStore
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

        @CreationTimestamp
        val created: LocalDateTime = LocalDateTime.now()

        @UpdateTimestamp
        var updated: LocalDateTime = LocalDateTime.now()

        fun addLicense(license: License) {
                _licenses.add(license)
                license.company = this
        }

        val licenses: List<License>
                get() = Collections.unmodifiableList(_licenses.toList())

        val latestLicense: License?
                get() = if(_licenses.isNotEmpty()) _licenses.last() else null

        fun keyPairStore(keyBasePath:String): KeyPairStore
                = KeyPairStore.load(keyBasePath, publicKeyPath, privateKeyPath)
}
