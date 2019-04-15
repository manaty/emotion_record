package org.manaty.octopus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_splash.*
import org.manaty.octopus.viewModels.SplashViewModel

class SplashActivity : AppCompatActivity() {

    lateinit var viewModel : SplashViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        viewModel = ViewModelProviders.of(this as FragmentActivity).get(SplashViewModel::class.java)

        val handler = Handler()
        handler.postDelayed(Runnable(){
            moveToMain()
        }, 3000)

        textview_app_version.text = "v" + BuildConfig.VERSION_NAME
    }

    fun moveToMain(){
        startActivity(Intent(this, MainActivity::class.java))
    }
}
