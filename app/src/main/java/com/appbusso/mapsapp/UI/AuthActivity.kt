package com.appbusso.mapsapp.UI

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.appbusso.mapsapp.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.login.LoginManager
import kotlinx.android.synthetic.main.activity_auth.*
import java.util.*
import android.widget.Toast
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import android.content.Intent
import android.os.Build
import android.support.design.widget.Snackbar
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.appbusso.mapsapp.MapsActivity
import com.appbusso.mapsapp.ViewModels.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module


class AuthActivity : AppCompatActivity() {
    private val loginViewModel: LoginViewModel by viewModel()

    private lateinit var callbackManager: CallbackManager
    private lateinit var gso: GoogleSignInOptions
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN: Int= 148

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        setuView()

    }

    private fun showProgress(){
        login_progress.visibility= View.VISIBLE
    }
    private fun hideProgress(){
        login_progress.visibility= View.GONE
    }
    private fun setuView() {
        setupFacebookLogin()
        setupGoogleLogin()
    }

    private fun showMainActivity(){
        val intent = Intent(this, MapsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun showMssage(texto: String) {
        val mSnackBar = Snackbar.make(constraintLogin, texto, Snackbar.LENGTH_LONG)
        val mainTextView = mSnackBar.view.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mainTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        } else {
            mainTextView.gravity = Gravity.CENTER_HORIZONTAL
        }
        mainTextView.gravity = Gravity.CENTER_HORIZONTAL
        mainTextView.setTextColor(resources.getColor(R.color.colorPrimary))
        mSnackBar.show()
    }

    private fun setupGoogleLogin() {
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        login_google.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

    }

    private fun setupFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        loginViewModel.handleFacebookAccessToken(loginResult.accessToken, this@AuthActivity)
                    }

                    override fun onCancel() {
                        Toast.makeText(this@AuthActivity, "Login Cancel", Toast.LENGTH_LONG).show()
                    }

                    override fun onError(exception: FacebookException) {
                        Toast.makeText(this@AuthActivity, exception.message, Toast.LENGTH_LONG).show()
                    }
                })



        login_facebook.setOnClickListener {
            showProgress()
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"))

        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {

                val account = task.getResult(ApiException::class.java)
                loginViewModel.firebaseAuthWithGoogle(account!!, this)

            } catch (e: ApiException) {
                Log.e("TAG", "Google sign in failed", e)
            }

        }else
            callbackManager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)

    }

}
val moduleMain= module{
    viewModel{ LoginViewModel() }
}