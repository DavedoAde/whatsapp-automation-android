package com.example.whatsappautomation

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.RawContacts.DefaultAccount
import android.provider.ContactsContract.RawContacts.DefaultAccount.DefaultAccountAndState
import android.util.Log

object PhoneContactManager {
    data class ContactAccount(val name: String?, val type: String?)

    // A result class to hold success status and any error message
    data class SaveResult(val isSuccess: Boolean, val errorMessage: String? = null)

    // 1. Fetch accounts reliably
    fun getAccounts(context: Context): List<ContactAccount> {
        val list = mutableListOf<ContactAccount>()
        list.add(ContactAccount(null, null)) // Phone only

        val defaultAccountAndState = DefaultAccount.getDefaultAccountForNewContacts(context.contentResolver)
        if (defaultAccountAndState.state == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD) {
            val account = defaultAccountAndState.account
            if (account != null && list.none { it.name == account.name }) {
                list.add(ContactAccount(account.name, account.type))
            }
        }

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.Settings.CONTENT_URI,
                arrayOf(ContactsContract.Settings.ACCOUNT_NAME, ContactsContract.Settings.ACCOUNT_TYPE),
                null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(0)
                    val type = it.getString(1)
                    if (name != null && type == "com.google" && list.none { acc -> acc.name == name }) {
                        list.add(ContactAccount(name, type))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneContactManager", "Error fetching accounts", e)
        }
        return list
    }

    // 2. Save contact and return a detailed result
    fun saveContactToPhone(context: Context, name: String, phoneNumber: String, selectedAccount: ContactAccount): SaveResult {
        return try {
            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = ops.size

            val rawContactInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            
            if (selectedAccount.name != null && selectedAccount.type != null) {
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, selectedAccount.type)
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, selectedAccount.name)
            } else {
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            }
            ops.add(rawContactInsert.build())

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            SaveResult(isSuccess = true)
        } catch (e: Exception) {
            Log.e("PhoneContactManager", "Failed to save contact: ${e.message}", e)
            SaveResult(isSuccess = false, errorMessage = e.localizedMessage ?: "Unknown error")
        }
    }
}
