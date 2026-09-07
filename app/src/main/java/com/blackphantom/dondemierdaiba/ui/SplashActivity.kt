package com.blackphantom.dondemierdaiba.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stylizeTagline()
        animateLogo()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1900)
    }

    // "mierda" en naranja (misma paleta que el logo), el resto en el color de texto primario
    private fun stylizeTagline() {
        val text = getString(R.string.splash_tagline)
        val spannable = SpannableString(text)
        val start = text.indexOf("mierda")
        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.accent_orange)),
                start,
                start + "mierda".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.txtAppName.text = spannable
    }

    private fun animateLogo() {
        val logoScaleX = ObjectAnimator.ofFloat(binding.imgLogo, "scaleX", 0.7f, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(binding.imgLogo, "scaleY", 0.7f, 1f)
        val logoAlpha = ObjectAnimator.ofFloat(binding.imgLogo, "alpha", 0f, 1f)
        val nameAlpha = ObjectAnimator.ofFloat(binding.txtAppName, "alpha", 0f, 1f)
        val nameTranslate = ObjectAnimator.ofFloat(binding.txtAppName, "translationY", 24f, 0f)
        val signAlpha = ObjectAnimator.ofFloat(binding.signatureRow, "alpha", 0f, 0.55f)

        val logoSet = AnimatorSet().apply {
            playTogether(logoScaleX, logoScaleY, logoAlpha)
            duration = 520
        }
        val nameSet = AnimatorSet().apply {
            playTogether(nameAlpha, nameTranslate)
            duration = 420
            startDelay = 180
        }
        signAlpha.apply {
            duration = 500
            startDelay = 700
        }

        AnimatorSet().apply {
            playTogether(logoSet, nameSet, signAlpha)
            start()
        }
    }
}
