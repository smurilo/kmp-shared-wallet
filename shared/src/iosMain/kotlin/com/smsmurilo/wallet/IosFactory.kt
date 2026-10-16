package com.smsmurilo.wallet

import com.smsmurilo.wallet.data.WalletApi
import com.smsmurilo.wallet.data.WalletRepository
import com.smsmurilo.wallet.presentation.WalletStore
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSUUID

/** Entry point called from Swift: `IosFactoryKt.makeWalletStore(baseUrl: ...)`. */
fun makeWalletStore(baseUrl: String): WalletStore {
    val formatter = NSDateFormatter().apply { dateFormat = "yyyy-MM" }
    return WalletStore(
        repository = WalletRepository(WalletApi.create(baseUrl)),
        month = formatter.stringFromDate(NSDate()),
        idGenerator = { NSUUID().UUIDString() },
    )
}
