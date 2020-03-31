package com.bnpinnovation.reaver

import com.bnpinnovation.reaver.dto.CompanyDto
import com.bnpinnovation.reaver.service.LicenseService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class ReaverApplication {
    private val log = LoggerFactory.getLogger(ReaverApplication::class.java)

    @Bean
    fun init(service: LicenseService) = CommandLineRunner {
        if (!service.existCompany("bnp")) {
            service.deleteAllKey()
            service.deleteAllLicense()
            val company = service.newCompany(CompanyDto.Request("bnp"))
            log.info("{}", company)
        }
    }

}

fun main(args: Array<String>) {
    runApplication<ReaverApplication>(*args)
}

