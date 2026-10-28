package com.example.whatsappautomation

import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object PhoneContactManager {
    fun saveContactToPhone(context: Context, name: String, phoneNumber: String): Boolean {
        return try {
            val saveAccount = PreferenceManager.getSaveAccount(context)
            var accountName: String? = null
            var accountType: String? = null

            // If user selected Google, find the primary Google account
            if (saveAccount == "GOOGLE") {
                val accounts = AccountManager.get(context).getAccountsByType("com.google")
                if (accounts.isNotEmpty()) {
                    accountName = accounts[0].name
                    accountType = accounts[0].type
                }
            }

            val ops = ArrayList<ContentProviderOperation>()
            
            // 1. Create the raw contact
            val rawContactInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            if (accountName != null && accountType != null) {
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
            } else {
                // Default to local phone memory
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            }
            ops.add(rawContactInsert.build())

            // 2. Add the name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // 3. Add the phone number
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            Log.e("PhoneContactManager", "Failed to save contact: ${e.message}")
            false
        }
    }
}
