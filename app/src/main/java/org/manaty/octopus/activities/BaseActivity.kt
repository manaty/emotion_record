package org.manaty.octopus.activities

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity(){

    fun toast(message : String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}