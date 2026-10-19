package com.blackphantom.dondemierdaiba

import androidx.multidex.MultiDexApplication
import com.blackphantom.dondemierdaiba.util.NotificationHelper
import com.blackphantom.dondemierdaiba.work.EpisodeCheckScheduler

class DMIApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        EpisodeCheckScheduler.schedulePeriodic(this)
        EpisodeCheckScheduler.scheduleUpdateCheck(this)
    }
}
