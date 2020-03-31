package com.bnpinnovation.reaver.service

import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class KeyPairStore {
    lateinit var publicKeyPath: String
    lateinit var privateKeyPath: String
    private val _publicKey: PublicKey
    private val _privateKey: PrivateKey

    private constructor() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024)
        val pair = keyGen.generateKeyPair()
        _privateKey = pair.private
        _publicKey = pair.public
    }

    constructor(publicKey: PublicKey, privateKey: PrivateKey) {
        _publicKey = publicKey
        _privateKey = privateKey
    }

    val publicKey: PublicKey
        get() = _publicKey

    val privateKey: PrivateKey
        get() = _privateKey

    fun writeKeyToFile(basePath: String="key", publicDstPath: String="public.key", privateDstPath: String="private.key") {
        FileStore.writeToFile(Paths.get(basePath, publicDstPath).toString(), publicKey.encoded)
        FileStore.writeToFile(Paths.get(basePath, privateDstPath).toString(), privateKey.encoded)
    }

    companion object {
        private val factory = KeyFactory.getInstance("RSA")

        fun generate(name: String, basePath: String): KeyPairStore {
            val keyPairStore = KeyPairStore()
            keyPairStore.publicKeyPath = publicPath(name)
            keyPairStore.privateKeyPath = privatePath(name)
            keyPairStore.writeKeyToFile(basePath, keyPairStore.publicKeyPath, keyPairStore.privateKeyPath)

            return keyPairStore
        }

        fun load(basePath: String, publicKeyPath: String, privateKeyPath: String): KeyPairStore {
            val keyPairStore = readFromFile(basePath, publicKeyPath, privateKeyPath)
            keyPairStore.publicKeyPath = publicKeyPath
            keyPairStore.privateKeyPath = privateKeyPath
            return keyPairStore
        }

        private fun readFromFile(basePath: String, publicKeyPath:String, privateKeyPath: String): KeyPairStore {
            val publicKeyFullPath = Paths.get(basePath, publicKeyPath).toString()
            val privateKeyFullPath = Paths.get(basePath, privateKeyPath).toString()
            return KeyPairStore(invokePublicKey(publicKeyFullPath), invokePrivateKey(privateKeyFullPath))
        }

        private val formatter = DateTimeFormatter.ofPattern("yyMMdd")

        // {companyName}/yyMMdd_public_{rnd}.key
        private fun publicPath(name: String)
                = Paths.get( name, generationFilename("public")).toString()

        // {companyName}/yyMMdd_private_{rnd}.key
        private fun privatePath(name: String)
                = Paths.get(name, generationFilename("private")).toString()

        // {companyName}/yyMMdd_signed_{rnd}.key
        fun signedKeyPath(name: String)
                = Paths.get(name, generationFilename("signed")).toString()

        // yyMMdd_{prefix}_{rnd}.key
        private fun generationFilename(prefix: String)
                = String.format("%s_%s_%d.key", LocalDateTime.now().format(formatter), prefix, (0..1000).random())

        private fun invokePublicKey(publicKeyPath: String)
                = factory.generatePublic(X509EncodedKeySpec(Files.readAllBytes(Paths.get(publicKeyPath))))

        private fun invokePrivateKey(privateKeyPath: String)
                = factory.generatePrivate(PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(privateKeyPath))))

    }
}

class ScarabStore {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyMMdd")

        fun scarabPath(name: String)
                = String.format("%s_%s_%d.scarab", name, LocalDateTime.now().format(formatter), (0..1000).random())

        fun save(basePath: String, scarabPath: String, scarab: ByteArray) {
            FileStore.writeToFile(Paths.get(basePath, scarabPath).toString(), scarab)
        }

    }
}

class FileStore {
    companion object {
        fun writeToFile(dstFullPath: String, data: ByteArray) {
            val path = Paths.get(dstFullPath)
            Files.createDirectories(path.parent)
            Files.write(path, data)
        }

        fun loadFile(filePath: Path): Resource {
            val resource = UrlResource(filePath.toUri())

            if(resource.exists() && resource.isReadable) {
                return resource
            }else{
                throw RuntimeException("failed to read file("+filePath.fileName+")!")
            }
        }
    }
}