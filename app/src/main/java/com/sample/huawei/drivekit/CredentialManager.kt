package com.sample.huawei.drivekit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.huawei.cloud.base.auth.DriveCredential
import com.huawei.cloud.client.exception.DriveCode
import com.huawei.cloud.services.drive.DriveScopes
import com.huawei.hms.support.account.AccountAuthManager
import com.huawei.hms.support.account.request.AccountAuthParams
import com.huawei.hms.support.account.request.AccountAuthParamsHelper
import com.huawei.hms.support.api.entity.auth.Scope
import com.huawei.hms.support.hwid.HuaweiIdAuthAPIManager
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.result.AuthHuaweiId
import com.sample.huawei.drivekit.DriveActivity.Companion.TAG
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object CredentialManager {
    private var account: AuthHuaweiId? = null
    var credential: DriveCredential? = null
    private val scopes = listOf(
        Scope(DriveScopes.SCOPE_DRIVE_FILE),
        Scope(DriveScopes.SCOPE_DRIVE_APPDATA)
    )


    fun getSignInIntent(context: Context): Intent? {
        val authParams = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .setAccessToken()
            .setIdToken()
            .setScopeList(scopes)
            .createParams()

        return HuaweiIdAuthManager.getService(context, authParams).signInIntent
    }

    fun getSignInResult(data: Intent?): Boolean {
        val result = HuaweiIdAuthAPIManager
            .HuaweiIdAuthAPIService
            .parseHuaweiIdFromIntent(data)
        if (!result.isSuccess) {
            Log.d(TAG, "Couldn't authorize")
        } else {
            account = result.huaweiId
            init()
        }
        return permissionsGranted
    }

    private val permissionsGranted: Boolean
        get() = (account != null && HuaweiIdAuthManager.containScopes(account, scopes))

    private suspend fun refreshAccessToken(activity: Activity)
    = suspendCoroutine<String> { continuation ->
        val authParams : AccountAuthParams =  AccountAuthParamsHelper(
            AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .createParams()
        val service = AccountAuthManager.getService(activity, authParams)
        val task = service.silentSignIn()
        task.addOnSuccessListener { authAccount ->
            continuation.resume(authAccount.accessToken)
        }
        .addOnFailureListener { e ->
            continuation.resumeWithException(e)
        }
    }

    private fun init(
        unionID: String? = account?.unionId,
        accessToken: String? = account?.accessToken
    ): Int {
        if(unionID.isNullOrEmpty() || accessToken.isNullOrEmpty()) {
            return DriveCode.ERROR
        }
        val builder = DriveCredential.Builder(unionID) {
            account?.accessToken
        }
        credential = builder.build().setAccessToken(accessToken)
        return DriveCode.SUCCESS
    }
}