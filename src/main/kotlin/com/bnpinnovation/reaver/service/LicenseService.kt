package com.bnpinnovation.reaver.service

import com.bnpinnovation.reaver.domain.Company
import com.bnpinnovation.reaver.domain.License
import com.bnpinnovation.reaver.dto.Certification
import com.bnpinnovation.reaver.dto.CompanyDto
import com.bnpinnovation.reaver.dto.LicenseDto
import com.bnpinnovation.reaver.repository.CompanyRepository
import com.bnpinnovation.reaver.repository.LicenseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.RuntimeException

interface LicenseService {
    fun companyList(): List<CompanyDto.Details>
    fun newCompany(request: CompanyDto.Request): CompanyDto.Details
    fun licenseList(companyId: Long): List<LicenseDto.Details>
    fun newLicense(companyId: Long, request: LicenseDto.Request): LicenseDto.Details

    @Service
    @Transactional
    class Default(val companyRepository: CompanyRepository, val licenseRepository: LicenseRepository): LicenseService {
        override fun newLicense(companyId: Long, request: LicenseDto.Request): LicenseDto.Details {
            val company = getCompany(companyId)
            val savedLicense = licenseRepository.save(License(request.totalUserCount, request.concurrentTalkCount, request.recordable))
            company.addLicense(savedLicense)
            return constructLicenseDetailsDto(savedLicense)
        }

        override fun licenseList(companyId: Long): List<LicenseDto.Details>
                = getCompany(companyId).liceses.map { constructLicenseDetailsDto(it) }.toList()

        override fun newCompany(request: CompanyDto.Request): CompanyDto.Details {
            if(!companyRepository.existsByName(request.name)) {
                val company = Company(request.name)
                val certification: Certification = createCertification()
                company.privateKey = certification.privateKey
                company.publicKey = certification.publicKey
                return constructDetailsDto(companyRepository.save(company))
            } else {
                // todo
                throw RuntimeException("해당하는 회사가 존재합니다. name : " + request.name)
            }
        }

        override fun companyList(): List<CompanyDto.Details> = companyRepository.findAll()
                .map { constructDetailsDto(it) }
                .toList()

        private fun getCompany(companyId: Long): Company
                = companyRepository.findById(companyId).orElseThrow { RuntimeException("회사(id:" + companyId + ")가 존재하지 않음") }

        private fun constructLicenseDetailsDto(license: License): LicenseDto.Details {
            val licenseId: Long = license.id?: throw RuntimeException("license id is null")
            return LicenseDto.Details(licenseId, license.totalUserCount, license.concurrentTalkCount, license.recordable, makeLink(license.scarab), license.created)
        }

        private fun makeLink(path: String?): String = "https://"+path

        private fun constructDetailsDto(c: Company): CompanyDto.Details {
            // todo
            val companyId: Long = c.id?: throw RuntimeException("company id is null")
            return CompanyDto.Details(companyId, c.name, makeLink(c.privateKey), makeLink(c.publicKey), c.created, c.updated)
        }

        private fun createCertification(): Certification = Certification("private key path", "public key path")
    }
}
