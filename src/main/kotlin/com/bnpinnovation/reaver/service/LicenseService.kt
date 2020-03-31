package com.bnpinnovation.reaver.service

import com.bnpinnovation.reaver.domain.Company
import com.bnpinnovation.reaver.domain.License
import com.bnpinnovation.reaver.dto.CompanyDto
import com.bnpinnovation.reaver.dto.LicenseDto
import com.bnpinnovation.reaver.repository.CompanyRepository
import com.bnpinnovation.reaver.repository.LicenseRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tomcat.util.http.fileupload.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Paths
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import javax.crypto.Cipher
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

        override fun newLicense(companyId: Long, request: LicenseDto.Request): LicenseDto.Details {
            val company = getCompany(companyId)
            val license = licenseRepository.save(License(request.totalUserCount, request.concurrentTalkCount, request.recordable))
            company.addLicense(license)

            val keyPairStore = KeyPairStore.load(keyBasePath, company.publicKeyPath, company.privateKeyPath)
            val plainDataWithBytes = mapper.writeValueAsBytes(request)
            val encryptedData = encrypt(keyPairStore.privateKey, plainDataWithBytes)

            val scarabPath = ScarabStore.scarabPath(company.name)
            license.scarab = scarabPath

            ScarabStore.save(scarabBasePath, scarabPath, encryptedData)
//            val plainData = decrypt(keyPairStore.publicKey, encryptedData)
//            val plainObject = mapper.readValue<LicenseDto.Request>(plainData)

//            log.info("===== compare {}", request==plainObject)

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
                = FileStore.loadFile(Paths.get(scarabBasePath, getLicense(licenseId).scarab))

        override fun scarabResourceLatest(companyId: Long): Resource {
            val company = getCompany(companyId)
            val license = company.latestLicense?: throw RuntimeException("The company("+company.name+") doesn't have a license")
            return FileStore.loadFile(Paths.get(scarabBasePath, license.scarab))
        }

        private fun getLicense(licenseId: Long): License // todo
                = licenseRepository.findById(licenseId).orElseThrow{ throw RuntimeException("license($licenseId)가 존재하지 않음") }

        override fun deleteAllKey() = FileUtils.cleanDirectory(Paths.get(keyBasePath).toFile())
        override fun deleteAllLicense() = FileUtils.cleanDirectory(Paths.get(scarabBasePath).toFile())

        override fun licenseList(companyId: Long): List<LicenseDto.Details>
                = getCompany(companyId).liceses.map { constructLicenseDetailsDto(it) }.toList()

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

        private fun signKeyAndSave(name:String, plainData: ByteArray): String {
            val signedKey = signKey(plainData)
            val filename = KeyPairStore.signedKeyPath(name)
            FileStore.writeToFile(Paths.get(keyBasePath, filename).toString(), signedKey)
            return filename
        }

        private fun signKey(plainData: ByteArray): ByteArray {
            val company = rootCompany()
            val keyPairStore = KeyPairStore.load(keyBasePath, company.publicKeyPath, company.privateKeyPath)
            return sign(keyPairStore.privateKey, plainData)
        }

        private fun rootCompany(): Company {
            return companyRepository.findByName(ROOT)?: throw RuntimeException("root company가 존재하지 않음")
        }

        override fun companyList(): List<CompanyDto.Details> = companyRepository.findAll()
                .map { constructDetailsDto(it) }
                .toList()

        private fun sign(privateKey: PrivateKey, plainData: ByteArray): ByteArray {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey);
            signature.update(plainData);
            return signature.sign()
        }

        private fun verify(publicKey: PublicKey, signatureData: ByteArray, plainData: ByteArray): Boolean {
            val signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey)
            signature.update(plainData)
            return signature.verify(signatureData)
        }

        fun encrypt(privateKey: PrivateKey, plainData: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(plainData)
        }

        fun decrypt(publicKey: PublicKey, encryptData: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return cipher.doFinal(encryptData)
        }

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
