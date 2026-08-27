package com.blackphantom.dondemierdaiba

import android.app.Application
import com.blackphantom.dondemierdaiba.util.NotificationHelper
import com.blackphantom.dondemierdaiba.work.EpisodeCheckScheduler

class DMIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        EpisodeCheckScheduler.schedulePeriodic(this)
    }
}
