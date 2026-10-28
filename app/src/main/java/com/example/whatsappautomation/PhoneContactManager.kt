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

            if (saveAccount == "GOOGLE") {
                val accounts = AccountManager.get(context).getAccountsByType("com.google")
                if (accounts.isNotEmpty()) {
                    accountName = accounts[0].name
                    accountType = accounts[0].type
                } else {
                    return false
                }
            }

            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = ops.size
            
            // 1. Create the raw contact
            val rawContactInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            if (accountName != null && accountType != null) {
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
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

            // 4. Add to "My Contacts" Group (Forces Google Sync)
            if (accountName != null && accountType != null) {
                val groupId = getMyContactsGroupId(context, accountName, accountType)
                if (groupId != null) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
                            .build()
                    )
                    Log.d("PhoneContactManager", "Added to My Contacts group ID: $groupId")
                } else {
                    Log.e("PhoneContactManager", "Could not find 'My Contacts' group ID.")
                }
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            Log.e("PhoneContactManager", "Failed to save contact: ${e.message}")
            false
        }
    }

    // Fetches the Group ID for "My Contacts" specific to the Google Account
    private fun getMyContactsGroupId(context: Context, accountName: String, accountType: String): Long? {
        val groupUri = ContactsContract.Groups.CONTENT_URI
        val projection = arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE)
        
        // Find groups for this specific account
        val selection = "${ContactsContract.Groups.ACCOUNT_NAME} = ? AND ${ContactsContract.Groups.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(accountName, accountType)

        context.contentResolver.query(groupUri, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndex(ContactsContract.Groups._ID)
            val titleColumn = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
            
            while (cursor.moveToNext()) {
                val title = cursor.getString(titleColumn)
                // The system name for the default Google group is "My Contacts"
                if (title != null && (title == "My Contacts" || title == "Contacts")) {
                    return cursor.getLong(idColumn)
                }
            }
        }
        return null
    }
}
