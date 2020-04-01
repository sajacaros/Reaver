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
            keyPairStore.publicKeyPath = publicPath(name).toString()
            keyPairStore.privateKeyPath = privatePath(name).toString()
            keyPairStore.writeKeyToFile(basePath, keyPairStore.publicKeyPath, keyPairStore.privateKeyPath)

            return keyPairStore
        }

        fun load(basePath: String, publicKeyPath: String, privateKeyPath: String): KeyPairStore {
            val keyPairStore = readFromFile(basePath, publicKeyPath, privateKeyPath)
            keyPairStore.publicKeyPath = publicKeyPath
            keyPairStore.privateKeyPath = privateKeyPath
            return keyPairStore
        }

        // {name}/{name}_{yyMMdd}_{rnd}.public
        fun publicPath(name: String): Path
                = Paths.get( name, FileStore.generationFilename(name, "public"))

        // {name}/{name}_{yyMMdd}_{rnd}.private
        fun privatePath(name: String): Path
                = Paths.get(name, FileStore.generationFilename(name, "private"))

        // {name}/{name}_{yyMMdd}_{rnd}.signed
        fun signedKeyPath(name: String): Path
                = Paths.get(name, FileStore.generationFilename(name, "signed"))

        private fun readFromFile(basePath: String, publicKeyPath:String, privateKeyPath: String): KeyPairStore {
            val publicKeyFullPath = Paths.get(basePath, publicKeyPath).toString()
            val privateKeyFullPath = Paths.get(basePath, privateKeyPath).toString()
            return KeyPairStore(invokePublicKey(publicKeyFullPath), invokePrivateKey(privateKeyFullPath))
        }

        private fun invokePublicKey(publicKeyPath: String)
                = factory.generatePublic(X509EncodedKeySpec(Files.readAllBytes(Paths.get(publicKeyPath))))

        private fun invokePrivateKey(privateKeyPath: String)
                = factory.generatePrivate(PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(privateKeyPath))))

    }
}

class ScarabStore {
    companion object {
        fun scarabPath(name: String): Path
                = Paths.get(name, FileStore.generationFilename(name, "scarab"))

        fun save(basePath: String, scarabPath: String, scarab: ByteArray)
                = FileStore.writeToFile(Paths.get(basePath, scarabPath).toString(), scarab)
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

        private val formatter = DateTimeFormatter.ofPattern("yyMMdd")
        // {name}_{yyMMdd}_{rnd}.{postfix}
        fun generationFilename(name: String, postfix: String)
                = String.format("%s_%s_%d.%s", name, LocalDateTime.now().format(formatter), (0..1000).random(), postfix)
    }
}

