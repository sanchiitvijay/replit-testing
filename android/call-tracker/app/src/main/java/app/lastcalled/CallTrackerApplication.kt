package app.lastcalled

import android.app.Application
import app.lastcalled.data.CallRepository
import app.lastcalled.data.local.CallTrackerDatabase
import app.lastcalled.data.platform.CallLogDataSource
import app.lastcalled.data.platform.ContactsDataSource

class CallTrackerApplication : Application() {
    val database by lazy { CallTrackerDatabase.create(this) }

    val repository by lazy {
        CallRepository(
            database = database,
            callLogDataSource = CallLogDataSource(this),
            contactsDataSource = ContactsDataSource(this),
            context = this,
        )
    }
}
