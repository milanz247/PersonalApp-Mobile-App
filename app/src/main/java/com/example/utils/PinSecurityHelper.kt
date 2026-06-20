package com.example.utils

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.experimental.and

object PinSecurityHelper {
    private val random = SecureRandom()

    // Generate a random 8-byte salt as hex string
    private fun generateSalt(): String {
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        return bytes.toHexString()
    }

    // Hash the input string using SHA-256 with the specified hex salt
    fun hashPin(pin: String, salt: String = generateSalt()): String {
        val md = MessageDigest.getInstance("SHA-256")
        val saltedInput = salt + pin
        val hashedBytes = md.digest(saltedInput.toByteArray(Charsets.UTF_8))
        val hashHex = hashedBytes.toHexString()
        return "$salt:$hashHex"
    }

    // Verify if the input pin matches the stored hashed PIN
    fun verifyPin(pin: String, storedHashWithSalt: String?): Boolean {
        if (storedHashWithSalt.isNullOrBlank()) return false
        val parts = storedHashWithSalt.split(":")
        if (parts.size != 2) {
            // Fallback to plain text PIN comparison for older records
            return pin == storedHashWithSalt
        }
        val salt = parts[0]
        val storedHash = parts[1]
        
        // Re-hash and compare hashes in a timing-safe constant-time comparison
        val calculatedHashWithSalt = hashPin(pin, salt)
        val calculatedHash = calculatedHashWithSalt.split(":")[1]
        
        return constantTimeEquals(calculatedHash, storedHash)
    }

    // Timing-attack resistant string comparison
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in 0 until a.length) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    // Helper extension to convert ByteArray to Hex string
    private fun ByteArray.toHexString(): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(size * 2)
        for (byte in this) {
            val octet = byte.toInt()
            val firstIndex = (octet ushr 4) and 0x0F
            val secondIndex = octet and 0x0F
            result.append(hexChars[firstIndex])
            result.append(hexChars[secondIndex])
        }
        return result.toString()
    }
}
