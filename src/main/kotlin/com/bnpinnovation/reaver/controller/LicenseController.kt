package com.bnpinnovation.reaver.controller

import com.bnpinnovation.reaver.dto.CompanyDto
import com.bnpinnovation.reaver.dto.LicenseDto
import com.bnpinnovation.reaver.service.LicenseService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/company")
class LicenseController(private val licenseService: LicenseService) {

    @GetMapping(value = [""], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun companyList(): List<CompanyDto.Details> = licenseService.companyList()

    @PostMapping(value = [""], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun newCompany(@RequestBody request: CompanyDto.Request): CompanyDto.Details
            = licenseService.newCompany( request )

    @GetMapping(value = ["/{companyId}/license"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun licenseList(@PathVariable companyId:Long): List<LicenseDto.Details>
            = licenseService.licenseList( companyId )

    @PostMapping(value = ["/{companyId}/license"], produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun newLicense(@PathVariable companyId:Long, @RequestBody request:LicenseDto.Request): LicenseDto.Details
            = licenseService.newLicense( companyId, request )

    @GetMapping(value = ["/{companyId}/private"])
    fun downloadPrivateKey(@PathVariable companyId: Long): ResponseEntity<Resource> {
        val resource = licenseService.privateKeyResource(companyId)
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.filename + "\"")
                .body(resource)
    }

    @GetMapping(value = ["/{companyId}/public"])
    fun downloadPublicKey(@PathVariable companyId: Long): ResponseEntity<Resource> {
        val resource = licenseService.publicKeyResource(companyId)
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +resource.filename + "\"")
                .body(resource)
    }

    @GetMapping(value = ["/{companyId}/signed"])
    fun downloadSignedKey(@PathVariable companyId: Long): ResponseEntity<Resource> {
        val resource = licenseService.signedKeyResource(companyId)
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +resource.filename + "\"")
                .body(resource)
    }

    @GetMapping(value = ["/{companyId}/license/{licenseId}"])
    fun scarab(@PathVariable companyId: Long, @PathVariable licenseId: Long): ResponseEntity<Resource> {
        val resource = licenseService.scarabResource(licenseId)
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +resource.filename + "\"")
                .body(resource)
    }

    @GetMapping(value = ["/{companyId}/license/latest"])
    fun scarabLatest(@PathVariable companyId: Long): ResponseEntity<Resource> {
        val resource = licenseService.scarabResourceLatest(companyId)
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +resource.filename + "\"")
                .body(resource)
    }
}