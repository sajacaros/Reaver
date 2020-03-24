package com.bnpinnovation.reaver.repository

import com.bnpinnovation.reaver.domain.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository: JpaRepository<Company, Long> {
    fun existsByName(name: String): Boolean;

}
