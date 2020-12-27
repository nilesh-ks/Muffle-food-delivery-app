package com.project.muffle.util

import android.util.Patterns



object Validations {
    fun validateMobile(mobile: String): Boolean {
        return mobile.length == 10
    }

    fun validatePasswordLength(password: String): Boolean {
        return password.length >= 4
    }

    fun validateNameLength(name: String): Boolean {
        return name.length >= 3
    }

    fun matchPassword(pass: String, confirmPass: String): Boolean {
        return pass == confirmPass
    }

    fun validateEmail(email: String): Boolean {
        return (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches())
    }
}