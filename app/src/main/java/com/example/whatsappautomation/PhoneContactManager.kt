package com.example.whatsappautomation

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object PhoneContactManager {
    // Data class to represent an account choice
    data class ContactAccount(val name: String?, val type: String?)

    fun getAccounts(context: Context): List<ContactAccount> {
        val list = mutableListOf(ContactAccount(null, null)) // Phone only
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
                val n = it.getString(0)
                val t = it.getString(1)
                if (n != null && t != null) {
                    list.add(ContactAccount(n, t))
                }
            }
        }
        return list
    }

    fun saveContactToPhone(context: Context, name: String, phoneNumber: String, selectedAccount: ContactAccount): Boolean {
        return try {
            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = ops.size
            
            // 1. Create the raw contact
            val rawContactInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, selectedAccount.type)
            rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, selectedAccount.name)
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
            true
        } catch (e: Exception) {
            Log.e("PhoneContactManager", "Failed to save contact: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
