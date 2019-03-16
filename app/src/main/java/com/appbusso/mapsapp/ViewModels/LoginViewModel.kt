package com.appbusso.mapsapp.ViewModels

import android.app.Activity
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount


class LoginViewModel: ViewModel()  {
    private var auth: FirebaseAuth= FirebaseAuth.getInstance()



    fun handleFacebookAccessToken(token: AccessToken, activity: Activity) {
        Log.d("TAG", "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.e("TAG", "signInWithCredential:success")
                        val user = auth.currentUser
                        //updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.e("TAG", "signInWithCredential:failure", task.exception)

                        //updateUI(null)
                    }

                    // ...
                }
    }


    fun firebaseAuthWithGoogle(acct: GoogleSignInAccount, activity: Activity) {
        Log.d(" TAG", "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(activity){ task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.e("TAG", "signInWithCredential:success")
                        val user = auth.currentUser
                        //updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.e("TAG", "signInWithCredential:failure", task.exception)

                        //updateUI(null)
                    }

                    // ...
                }
    }

}