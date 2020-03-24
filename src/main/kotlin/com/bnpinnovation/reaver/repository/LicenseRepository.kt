package com.bnpinnovation.reaver.repository

import com.bnpinnovation.reaver.domain.License
import org.springframework.data.jpa.repository.JpaRepository

interface LicenseRepository: JpaRepository<License, Long> {

}
