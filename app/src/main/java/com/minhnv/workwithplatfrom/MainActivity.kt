package com.minhnv.workwithplatfrom

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.api.services.youtube.YouTubeRequestInitializer

import com.google.api.client.http.HttpRequestInitializer

import com.google.api.client.json.jackson2.JacksonFactory

import com.google.api.client.http.javanet.NetHttpTransport

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import kotlinx.coroutines.*
import java.text.NumberFormat
import android.content.pm.PackageManager

import android.content.pm.PackageInfo
import android.util.Base64
import android.util.Log
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.ParsedRequestListener
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import com.facebook.login.LoginManager
import com.facebook.AccessToken








class MainActivity : AppCompatActivity() {
    val apiKey = "AIzaSyDj4TofFoJghOnM54sbS_pyiJEclUJlSp8"
    val callbackManager = CallbackManager.Factory.create()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        val btnGetSub = findViewById<Button>(R.id.btnGetSubCount)
        val edChannel = findViewById<EditText>(R.id.edChannel)
        val tvSubCount = findViewById<TextView>(R.id.tvSubCount)

        btnGetSub.setOnClickListener {
            val handlerException = CoroutineExceptionHandler { coroutineContext, throwable ->
                MaterialAlertDialogBuilder(this)
                    .setMessage("Error: ${throwable.localizedMessage}")
                    .setPositiveButton("Ok", null)
                    .show()
            }
            CoroutineScope(Dispatchers.Main + handlerException).launch {
                if (edChannel.text.toString().isEmpty()) {
                    Toast.makeText(this@MainActivity, "Nhập tên kênh", Toast.LENGTH_SHORT).show();
                    return@launch;
                }
                val youtube = YouTube.Builder(
                    NetHttpTransport(),
                    JacksonFactory()
                ) { }
                    .setApplicationName("youtube-cmdline-search-sample")
                    .setYouTubeRequestInitializer(YouTubeRequestInitializer(apiKey))
                    .build()

                val search = youtube.search().list("snippet")
                search.q = edChannel.text.toString()
                search.type = "channel"
                val channels = withContext(Dispatchers.IO + handlerException) {
                    val response = search.execute()
                    val result = response.items
                    result?.let { searchListResults ->
                        val searchResultFirst = searchListResults.firstOrNull()
                        val channelId = searchResultFirst?.snippet?.channelId
                        val channels = youtube.channels().list("snippet, statistics")
                        channels.id = channelId
                        return@withContext channels
                    }
                }
                val result = withContext(Dispatchers.IO + handlerException) {
                   return@withContext channels?.execute()?.items
                }
                withContext(Dispatchers.Main + handlerException) {
                    val subCount = result?.first()?.statistics?.subscriberCount
                    val subFormat = NumberFormat.getInstance().format(subCount)
                    tvSubCount.text = "Sub: $subFormat"
                }
            }


        }



        val btnLogin = findViewById<LoginButton>(R.id.login_button)
        val btnGetFollowers = findViewById<Button>(R.id.btnGetFollowers)
        var tokenLoginFb = "";
        btnLogin.setReadPermissions("email")
        btnLogin.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                print("AccessToken: ${result?.accessToken}")
            }

            override fun onCancel() {
                print("Login cancel")
            }

            override fun onError(error: FacebookException?) {
                print("Login error")
            }

        } )

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult?> {
                override fun onSuccess(loginResult: LoginResult?) {
                    print("AccessToken: ${loginResult?.accessToken?.token}")
                    val accessToken = AccessToken.getCurrentAccessToken()
                    val isLoggedIn = accessToken != null && !accessToken.isExpired
                    if (isLoggedIn) {
                        tokenLoginFb = "${loginResult?.accessToken?.token}"
                        btnGetFollowers.isEnabled = true
                    }
                }

                override fun onCancel() {
                    // App code
                }

                override fun onError(exception: FacebookException) {
                    // App code
                }
            })

        AndroidNetworking.initialize(applicationContext);

        btnGetFollowers.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                AndroidNetworking.get("https://graph.facebook.com/v12.0/me/accounts?access_token={access-token}")
                    .addPathParameter("access-token", tokenLoginFb)
                    .build()
                    .getAsObject(Data::class.java, object : ParsedRequestListener<Data> {
                        override fun onResponse(response: Data?) {
                           print("idPage: ${response?.data?.id}")
                        }

                        override fun onError(anError: ANError?) {
                            print("idPage Error: ${anError?.message}")
                        }
                    })
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}