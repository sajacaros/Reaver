package com.bnpinnovation.reaver.controller

import com.bnpinnovation.reaver.dto.CompanyDto
import com.bnpinnovation.reaver.dto.LicenseDto
import com.bnpinnovation.reaver.service.LicenseService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/company")
class LicenseController(private val service: LicenseService) {

    @GetMapping(value = [""], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun companyList(): List<CompanyDto.Details> = service.companyList()

    @PostMapping(value = [""], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun newCompany(@RequestBody request: CompanyDto.Request): CompanyDto.Details = service.newCompany( request )

    @GetMapping(value = ["/{companyId}/license"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun licenseList(@PathVariable companyId:Long): List<LicenseDto.Details> = service.licenseList( companyId )

    @PostMapping(value = ["/{companyId}/license"], produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun newLicense(@PathVariable companyId:Long, @RequestBody request:LicenseDto.Request): LicenseDto.Details = service.newLicense( companyId, request )
}