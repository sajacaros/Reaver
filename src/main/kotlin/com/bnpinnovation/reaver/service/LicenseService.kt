package com.bnpinnovation.reaver.service

import com.bnpinnovation.reaver.domain.Company
import com.bnpinnovation.reaver.domain.License
import com.bnpinnovation.reaver.dto.CompanyDto
import com.bnpinnovation.reaver.dto.LicenseDto
import com.bnpinnovation.reaver.repository.CompanyRepository
import com.bnpinnovation.reaver.repository.LicenseRepository
import com.bnpinnovation.reaver.service.KeyPairStore.Companion.signedKeyPath
import com.bnpinnovation.reaver.util.EncryptionUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tomcat.util.http.fileupload.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Paths
import javax.persistence.EntityManager

interface LicenseService {
    fun companyList(): List<CompanyDto.Details>
    fun newCompany(request: CompanyDto.Request): CompanyDto.Details
    fun licenseList(companyId: Long): List<LicenseDto.Details>
    fun newLicense(companyId: Long, request: LicenseDto.Request): LicenseDto.Details
    fun existCompany(s: String): Boolean
    fun privateKeyResource(companyId: Long): Resource
    fun publicKeyResource(companyId: Long): Resource
    fun signedKeyResource(companyId: Long): Resource
    fun deleteAllKey()
    fun deleteAllLicense()
    fun scarabResource(licenseId: Long): Resource
    fun scarabResourceLatest(companyId: Long): Resource

    @Service
    @Transactional
    class Default(val companyRepository: CompanyRepository, val licenseRepository: LicenseRepository): LicenseService {
        private val log = LoggerFactory.getLogger(LicenseService::class.java)
        private val ROOT = "bnp"

        @Autowired
        lateinit var entityManager: EntityManager

        @Value("\${gamble.path}")
        lateinit var keyBasePath: String

        @Value("\${scarab.path}")
        lateinit var scarabBasePath: String

        @Autowired
        lateinit var mapper: ObjectMapper

        override fun newCompany(request: CompanyDto.Request): CompanyDto.Details {
            if(!companyRepository.existsByName(request.name)) {
                val company = companyRepository.save(Company(request.name))
                val keyPairStore = KeyPairStore.generate(request.name, keyBasePath)

                company.publicKeyPath = keyPairStore.publicKeyPath
                company.privateKeyPath = keyPairStore.privateKeyPath

                entityManager.flush()

                company.signedKeyPath = signKeyAndSave(request.name, keyPairStore.publicKey.encoded)

                return constructDetailsDto(company)
            } else {
                // todo
                throw RuntimeException("해당하는 회사가 존재합니다. name : " + request.name)
            }
        }

        override fun newLicense(companyId: Long, request: LicenseDto.Request): LicenseDto.Details {
            val company = getCompany(companyId)
            val license = licenseRepository.save(License(request.totalUserCount, request.concurrentTalkCount, request.recordable))
            company.addLicense(license)

            val keyPairStore = company.keyPairStore(keyBasePath)
            val plainDataWithBytes = mapper.writeValueAsBytes(request)
            val encryptedData = EncryptionUtils.encrypt(keyPairStore.privateKey, plainDataWithBytes)

            val scarabPath = ScarabStore.scarabPath(company.name)
            license.scarab = scarabPath.toString()

            ScarabStore.save(scarabBasePath, scarabPath.toString(), encryptedData)

            return constructLicenseDetailsDto(license)
        }

        override fun existCompany(s: String): Boolean = companyRepository.existsByName(s)

        override fun publicKeyResource(companyId: Long): Resource
                = FileStore.loadFile(Paths.get(keyBasePath, getCompany(companyId).publicKeyPath))

        override fun privateKeyResource(companyId: Long): Resource
                = FileStore.loadFile(Paths.get(keyBasePath, getCompany(companyId).privateKeyPath))

        override fun signedKeyResource(companyId: Long): Resource
                = FileStore.loadFile(Paths.get(keyBasePath, getCompany(companyId).signedKeyPath))

        override fun scarabResource(licenseId: Long): Resource
                = scarabResource(getLicense(licenseId))

        override fun scarabResourceLatest(companyId: Long): Resource {
            val license = getCompany(companyId).latestLicense?: throw RuntimeException("The company($companyId) doesn't have a license")
            return scarabResource(license)
        }

        override fun deleteAllKey() = FileUtils.cleanDirectory(Paths.get(keyBasePath).toFile())

        override fun deleteAllLicense() = FileUtils.cleanDirectory(Paths.get(scarabBasePath).toFile())
        override fun licenseList(companyId: Long): List<LicenseDto.Details>
                = getCompany(companyId).licenses.map { constructLicenseDetailsDto(it) }.toList()

        private fun scarabResource(license: License): Resource
                = FileStore.loadFile(Paths.get(scarabBasePath, license.scarab))

        private fun getLicense(licenseId: Long): License // todo
                = licenseRepository.findById(licenseId).orElseThrow{ throw RuntimeException("license($licenseId)가 존재하지 않음") }

        private fun signKeyAndSave(companyName:String, plainData: ByteArray): String {
            val filePath = signedKeyPath(companyName).toString()
            FileStore.writeToFile(Paths.get(keyBasePath, filePath).toString(), signing(plainData))
            return filePath
        }

        private fun signing(plainData: ByteArray): ByteArray {
            val keyPairStore = rootCompany().keyPairStore(keyBasePath)
            return EncryptionUtils.sign(keyPairStore.privateKey, plainData)
        }

        private fun rootCompany(): Company {
            return companyRepository.findByName(ROOT)?: throw RuntimeException("root company가 존재하지 않음")
        }

        override fun companyList(): List<CompanyDto.Details> = companyRepository.findAll()
                .map { constructDetailsDto(it) }
                .toList()

        private fun getCompany(companyId: Long): Company
                = companyRepository.findById(companyId).orElseThrow { RuntimeException("회사(id:" + companyId + ")가 존재하지 않음") }

        private fun constructLicenseDetailsDto(license: License): LicenseDto.Details {
            val licenseId: Long = license.id?: throw RuntimeException("license id is null")
            return LicenseDto.Details(licenseId, license.totalUserCount, license.concurrentTalkCount, license.recordable, license.created)
        }

        private fun constructDetailsDto(c: Company): CompanyDto.Details {
            // todo
            val companyId: Long = c.id?: throw RuntimeException("company id is null")
            return CompanyDto.Details(companyId, c.name, c.created, c.updated)
        }
    }
}
