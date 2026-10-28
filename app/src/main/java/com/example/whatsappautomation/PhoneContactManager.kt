package com.example.whatsappautomation

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object PhoneContactManager {
    data class ContactAccount(val name: String?, val type: String?)

    // Reliable way to get accounts available for saving contacts
    fun getAccounts(context: Context): List<ContactAccount> {
        val list = mutableListOf<ContactAccount>()
        list.add(ContactAccount(null, null)) // Phone Only option

        try {
            // Query the Settings table which lists all accounts that can hold contacts
            val cursor = context.contentResolver.query(
                ContactsContract.Settings.CONTENT_URI,
                arrayOf(
                    ContactsContract.Settings.ACCOUNT_NAME,
                    ContactsContract.Settings.ACCOUNT_TYPE
                ),
                null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(0)
                    val type = it.getString(1)
                    // Only include Google accounts to avoid junk, and avoid duplicates
                    if (name != null && type != null && type == "com.google") {
                        if (list.none { acc -> acc.name == name }) {
                            list.add(ContactAccount(name, type))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneContactManager", "Error fetching accounts", e)
        }
        return list
    }

    fun saveContactToPhone(context: Context, name: String, phoneNumber: String, selectedAccount: ContactAccount): Boolean {
        return try {
            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = ops.size

            // 1. Create the raw contact with the correct account
            val rawContactInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            
            if (selectedAccount.name != null && selectedAccount.type != null) {
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, selectedAccount.type)
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, selectedAccount.name)
                Log.d("PhoneContactManager", "Saving to Google Account: ${selectedAccount.name}")
            } else {
                // Fallback to local
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                Log.d("PhoneContactManager", "Saving to Local Phone Memory")
            }
            ops.add(rawContactInsert.build())

            // 2. Add the name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // 3. Add the phone number
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Log.d("PhoneContactManager", "Successfully saved contact: $name")
            true
        } catch (e: Exception) {
            Log.e("PhoneContactManager", "Failed to save contact: ${e.message}", e)
            false
        }
    }
}
