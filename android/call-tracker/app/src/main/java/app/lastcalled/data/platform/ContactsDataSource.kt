package app.lastcalled.data.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import app.lastcalled.domain.ImportedContact

class ContactsDataSource(private val context: Context) {
    fun readWithPhoneNumbers(): List<ImportedContact> {
        checkPermission()

        val contacts = linkedMapOf<String, MutableContact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.Contacts.PHOTO_URI,
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            )
            val nameColumn = cursor.getColumnIndexOrThrow(
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            )
            val numberColumn = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            val photoColumn = cursor.getColumnIndexOrThrow(
                ContactsContract.Contacts.PHOTO_URI,
            )

            while (cursor.moveToNext()) {
                val providerId = cursor.getLong(idColumn).toString()
                val number = cursor.getString(numberColumn)?.trim().orEmpty()
                if (number.isBlank()) continue

                val contact = contacts.getOrPut(providerId) {
                    MutableContact(
                        id = "contact:$providerId",
                        displayName = cursor.getString(nameColumn)?.takeIf(String::isNotBlank)
                            ?: "Unnamed contact",
                        photoUri = cursor.getString(photoColumn),
                    )
                }
                contact.phoneNumbers += number
            }
        }

        return contacts.values.map { contact ->
            ImportedContact(
                id = contact.id,
                displayName = contact.displayName,
                photoUri = contact.photoUri,
                phoneNumbers = contact.phoneNumbers.distinct(),
            )
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Contacts permission is not granted.")
        }
    }

    private data class MutableContact(
        val id: String,
        val displayName: String,
        val photoUri: String?,
        val phoneNumbers: MutableList<String> = mutableListOf(),
    )
}
