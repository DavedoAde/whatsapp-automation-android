package com.example.whatsappautomation

import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object PhoneContactManager {
    data class ContactAccount(val name: String?, val type: String?)

    // A result class to hold success status and any error message
    data class SaveResult(val isSuccess: Boolean, val errorMessage: String? = null)

    // 1. Fetch accounts reliably using AccountManager (Requires GET_ACCOUNTS permission)
    fun getAccounts(context: Context): List<ContactAccount> {
        val list = mutableListOf<ContactAccount>()
        list.add(ContactAccount(null, null)) // Phone only

        try {
            val accountManager = AccountManager.get(context)
            val googleAccounts = accountManager.getAccountsByType("com.google")
            
            for (account in googleAccounts) {
                if (list.none { it.name == account.name }) {
                    list.add(ContactAccount(account.name, account.type))
                }
            }
        } catch (e: SecurityException) {
            Log.e("PhoneContactManager", "Missing GET_ACCOUNTS permission!", e)
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
                Log.d("PhoneContactManager", "Attempting to save to: ${selectedAccount.name}")
            } else {
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                rawContactInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                Log.d("PhoneContactManager", "Attempting to save to: Local Phone Memory")
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
