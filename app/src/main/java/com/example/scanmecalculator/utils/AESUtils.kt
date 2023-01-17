package com.example.scanmecalculator.utils

import com.example.scanmecalculator.model.ResultItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Type
import java.security.*
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

object AESUtils {
    private val SECRET = "ASDFGHJKLASDFGHJ".toByteArray()

    fun saveArrayToEncryptedFile(resultItems: ArrayList<ResultItem>, file: File?) {
        val gson = Gson()
        val jsonString = gson.toJson(resultItems)
        val encryptedJsonString = encrypt(rawKey, jsonString.toByteArray())
        val fos = FileOutputStream(file)
        fos.write(encryptedJsonString)
        fos.close()
    }

    fun readArrayFromEncryptedFile(file: File): ByteArray {
        if (!file.exists()) return "".toByteArray()
        val fis = FileInputStream(file)
        val encryptedJsonString = ByteArray(file.length().toInt())
        fis.read(encryptedJsonString)
        fis.close()

        return decrypt(encryptedJsonString)
    }

    @get:Throws(Exception::class)
    private val rawKey: ByteArray
        get() {
            val key: SecretKey = SecretKeySpec(SECRET, "AES")
            return key.encoded
        }

    @Throws(Exception::class)
    private fun encrypt(raw: ByteArray, clear: ByteArray): ByteArray {
        val skeySpec: SecretKey = SecretKeySpec(raw, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
        return cipher.doFinal(clear)
    }

    @Throws(Exception::class)
    private fun decrypt(encrypted: ByteArray): ByteArray {
        val skeySpec: SecretKey = SecretKeySpec(SECRET, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec)
        return cipher.doFinal(encrypted)
    }

    fun toByte(hexString: String): ByteArray {
        val len = hexString.length / 2
        val result = ByteArray(len)
        for (i in 0 until len) result[i] = Integer.valueOf(
            hexString.substring(2 * i, 2 * i + 2),
            16
        ).toByte()
        return result
    }

    fun toHex(buf: ByteArray?): String {
        if (buf == null) return ""
        val result = StringBuffer(2 * buf.size)
        for (i in buf.indices) {
            appendHex(result, buf[i])
        }
        return result.toString()
    }

    private const val HEX = "0123456789ABCDEF"
    private fun appendHex(sb: StringBuffer, b: Byte) {
        sb.append(HEX[b.toInt() shr 4 and 0x0f]).append(HEX[b.toInt() and 0x0f])
    }
}

//object  EncryptionUtils {
//
//    val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
//    val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
//
//    val fileToRead = "my_sensitive_data.txt"
//    val encryptedFile = EncryptedFile.Builder(
//        File(DIRECTORY, fileToRead),
//        applicationContext,
//        mainKeyAlias,
//        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
//    ).build()
//
//    val inputStream = encryptedFile.openFileInput()
//    val byteArrayOutputStream = ByteArrayOutputStream()
//    var nextByte: Int = inputStream.read()
//    while (nextByte != -1) {
//        byteArrayOutputStream.write(nextByte)
//        nextByte = inputStream.read()
//    }
//
//    val plaintext: ByteArray = byteArrayOutputStream.toByteArray()

//    private  const val KEY_ALIAS = "myKey"
//    private  const val FILE_NAME = "resultItems.enc"
//    private  const val ANDROID_KEY_STORE = "AndroidKeyStore"
//    private val password = "sd"
//    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
//    fun generateKey(): SecretKey? {
//        return SecretKeySpec(password.toByteArray(), "AES")
//    }
//
//    @Throws(
//        NoSuchAlgorithmException::class,
//        NoSuchPaddingException::class,
//        InvalidKeyException::class,
//        InvalidParameterSpecException::class,
//        IllegalBlockSizeException::class,
//        BadPaddingException::class,
//        UnsupportedEncodingException::class
//    )
//    fun encryptMsg(message: String, secret: SecretKey?): ByteArray? {
//        /* Encrypt the message. */
//        var cipher: Cipher? = null
//        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
//        cipher.init(Cipher.ENCRYPT_MODE, secret)
//        return cipher.doFinal(message.toByteArray(charset("UTF-8")))
//    }
//
//    @Throws(
//        NoSuchPaddingException::class,
//        NoSuchAlgorithmException::class,
//        InvalidParameterSpecException::class,
//        InvalidAlgorithmParameterException::class,
//        InvalidKeyException::class,
//        BadPaddingException::class,
//        IllegalBlockSizeException::class,
//        UnsupportedEncodingException::class
//    )
//    fun decryptMsg(cipherText: ByteArray?, secret: SecretKey?): String? {
//        /* Decrypt the message, given derived encContentValues and initialization vector. */
//        var cipher: Cipher? = null
//        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
//        cipher.init(Cipher.DECRYPT_MODE, secret)
//        return String(cipher.doFinal(cipherText), Charset.forName("UTF-8"))
//    }


//    fun saveArrayToEncryptedFile(resultItems: ArrayList<ResultItem>, file: File?) {
//        try {
//            // Convert the array of ResultItem objects into a JSON string
//            val gson = Gson()
//            val jsonString = gson.toJson(resultItems)
//
//
//            // Generate a key pair in the Android Keystore
//            val keyPairGenerator = KeyPairGenerator.getInstance("RSA", ANDROID_KEY_STORE)
//            keyPairGenerator.initialize(
//                KeyGenParameterSpec.Builder(
//                    KEY_ALIAS,
//                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
//                )
//                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
//                    .build()
//            )
//            val keyPair = keyPairGenerator.generateKeyPair()
//
//            // Encrypt the JSON string using the public key
//            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
//            cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)
////            val encryptedJsonString = cipher.doFinal(jsonString.toByteArray())
//            val encryptedJsonString = cipher.doFinal(jsonString.toByteArray(charset("UTF-8")))
//            Log.d(TAG, encryptedJsonString.toString())
//            // Save the encrypted JSON string to a file
//            val fos = FileOutputStream(file)
//            fos.write(encryptedJsonString)
//            fos.close()
//        } catch (e: Exception) {
//            Log.e("EncryptionUtils", "Error saving array to encrypted file", e)
//        }
//    }
////
////    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
////    fun generateKey(): SecretKey? {
////        return SecretKeySpec(password.getBytes(), "AES").also { secret = it }
////    }
////
////    @Throws(
////        NoSuchAlgorithmException::class,
////        NoSuchPaddingException::class,
////        InvalidKeyException::class,
////        InvalidParameterSpecException::class,
////        IllegalBlockSizeException::class,
////        BadPaddingException::class,
////        UnsupportedEncodingException::class
////    )
////    fun encryptMsg(message: String, secret: SecretKey?): ByteArray? {
////        /* Encrypt the message. */
////        var cipher: Cipher? = null
////        cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
////        cipher.init(Cipher.ENCRYPT_MODE, secret)
////        return cipher.doFinal(message.toByteArray(charset("UTF-8")))
////    }
////
////    @Throws(
////        NoSuchPaddingException::class,
////        NoSuchAlgorithmException::class,
////        InvalidParameterSpecException::class,
////        InvalidAlgorithmParameterException::class,
////        InvalidKeyException::class,
////        BadPaddingException::class,
////        IllegalBlockSizeException::class,
////        UnsupportedEncodingException::class
////    )
////    fun decryptMsg(cipherText: ByteArray?, secret: SecretKey?): String? {
////        /* Decrypt the message, given derived encContentValues and initialization vector. */
////        var cipher: Cipher? = null
////        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
////        cipher.init(Cipher.DECRYPT_MODE, secret)
////        return String(cipher.doFinal(cipherText), Charset.forName("UTF-8"))
////    }
//
//    fun readArrayFromEncryptedFile(file: File): Array<ResultItem>? {
//        return try {
//            // Read the encrypted JSON string from the file
//            val fis = FileInputStream(file)
//            val encryptedJsonString = ByteArray(file.length().toInt())
//            fis.read(encryptedJsonString)
//            fis.close()
//
//            // Decrypt the JSON string using the private key
//            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
//            keyStore.load(null)
//            val privateKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
//            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
//            cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
//            val jsonString = cipher.doFinal(encryptedJsonString)
//
//            // Parse the JSON string back into an array of ResultItem objects
//            val gson = Gson()
//            gson.fromJson(String(jsonString), Array<ResultItem>::class.java)
//        } catch (e: Exception) {
//            Log.e("EncryptionUtils", "Error reading array from encrypted file", e)
//            null
//        }
//    }
