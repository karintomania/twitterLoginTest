package com.bedroomcomputing.logintest

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            getRequestToken();
        }
    }

    lateinit var twitter: Twitter
    private fun getRequestToken(){
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default){

            // twitterインスタンスにConsumer keyとConsumer Secretを設定
            val builder = ConfigurationBuilder()
                .setDebugEnabled(true)
                .setOAuthConsumerKey(TwitterConstants.CONSUMER_KEY)
                .setOAuthConsumerSecret(TwitterConstants.CONSUMER_SECRET)

            val config = builder.build()
            val factory = TwitterFactory(config)

            // twitter インスタンスを生成
            twitter = factory.instance

            try{
                // リクエストトークンを取得
                val requestToken = twitter.oAuthRequestToken
                withContext(Dispatchers.Main){
                    setupTwitterWebviewDialog(requestToken.authorizationURL)
                }
            } catch (e: IllegalStateException) {
                Log.e("ERROR: ", e.toString())
            }

        }
    }

    lateinit var twitterDialog: Dialog
    var accToken: AccessToken? = null

    // Dialogの設定
    private suspend fun setupTwitterWebviewDialog(url: String){
        twitterDialog = Dialog(requireContext())
        val webView = WebView(requireContext())

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webViewClient = TwitterWebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        twitterDialog.setContentView(webView)
        twitterDialog.show()

    }

    // WebViewの設定
    inner class TwitterWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request?.url.toString().startsWith(TwitterConstants.CALLBACK_URL)) {
                Log.d("Authorization URL: ", request?.url.toString())
                handleUrl(request?.url.toString())

                // 認証が完了したらダイアログを閉じる
                if (request?.url.toString().contains(TwitterConstants.CALLBACK_URL)) {
                    twitterDialog.dismiss()
                }
                return true
            }
            return false
        }

        // OAuthのトークン取得
        private fun handleUrl(url: String) {
            val uri = Uri.parse(url)
            val oauthVerifier = uri.getQueryParameter("oauth_verifier") ?: ""
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val accToken = withContext(Dispatchers.IO) {
                    twitter.getOAuthAccessToken(oauthVerifier)
                }
                Log.i("token", accToken.token)
                Log.i("token secret", accToken.tokenSecret)
                getUserProfile()
            }
        }

        // ユーザ情報取得
        private suspend fun getUserProfile(){
            val usr = withContext(Dispatchers.IO){ twitter.verifyCredentials() }

            Log.i("twitter", usr.name)
            Log.i("twitter", usr.screenName)
        }
    }


}