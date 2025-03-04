package com.darcy.vpndemo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.darcy.vpndemo.MainActivity


class TestVPNService : VpnService() {
    private var vpn: ParcelFileDescriptor? = null
    private val context = this

    companion object {
        private val TAG = TestVPNService::class.java.simpleName
        private const val NOTIFICATION_ID: Int = 1

        private const val EXTRA_COMMAND = "Command"
        const val START: String = "start_service"
        const val OPEN_VPN: String = "open_vpn"
        const val CLOSE_VPN: String = "close_vpn"
        const val STOP: String = "stop_service"
        const val DEFAULT: String = "default"
    }

    override fun onCreate() {
        // Listen for connectivity updates
        val ifConnectivity = IntentFilter()
        ifConnectivity.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Get command
        val cmd = intent.action ?: DEFAULT
        Log.d(TAG, "执行cmd：$cmd")
        when (cmd) {
            START -> {
                setOrUpdateForegroundService(cmd)
            }

            OPEN_VPN -> {
                setOrUpdateForegroundService(cmd)
                initVPN()
            }

            CLOSE_VPN -> {
                setOrUpdateForegroundService(cmd)
                if (vpn != null) {
                    vpn!!.close()
                    vpn = null
                }
                return START_NOT_STICKY
            }

            STOP -> {
                vpn = null
                stopSelf()
            }

            else -> {
                Log.d(TAG, "未知命令：$cmd")
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        super.onRevoke()
        Log.e(TAG, "VPN结束 onRevoke")
    }

    private fun initVPN() {
        if (vpn != null) {
            Log.w(TAG, "VPN已开启")
            return
        }
        val builder: Builder = Builder().apply {
            setSession(getString(com.darcy.vpndemo.R.string.app_name))
            // Create a local TUN interface using predetermined addresses
            addAddress("192.168.2.2", 24)
            addDnsServer("192.168.1.1")
//            addAddress("10.1.10.1", 32)
//            addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
            addRoute("0.0.0.0", 0)
            addRoute("0:0:0:0:0:0:0:0", 0)
        }

        /**
         * builder.setMtu(int mut)//设置读写操作时最大缓存
         * .setSession(String session)//设置该次服务名称，服务启动后可在手机设置界面查看
         * .addAddress(String address, int port)//设置虚拟主机地址和端口
         * .addRoute(String address, int port)//设置允许通过的路由
         * .addDnsServer(String address)//添加域名服务器
         * .addAllowedApplication(String name)//添加允许访问连接的程序
         * .setConfigureIntent(PendingIntent intent);//设置配置启动项
         */
        val appPackages = arrayOf(context.packageName)
        val packageManager = packageManager
        for (item in appPackages) {
            try {
                Log.d(TAG, "添加允许的VPN应用：$item")
                packageManager.getPackageInfo(item, 0)
                builder.addAllowedApplication(item)
            } catch (e: PackageManager.NameNotFoundException) {
                // The app isn't installed.
                Log.e(TAG, "添加允许的VPN应用出错，未找到")
                e.printStackTrace()
            }
        }
        vpn = builder.establish()
    }

    private fun setOrUpdateForegroundService(action: String) {
        // 创建通知渠道（适用于 Android 8.0 及以上）
        createNotificationChannel()
        // 创建点击通知后打开的 Activity 的 Intent
        val intentClick = Intent(context, MainActivity::class.java).apply {
            putExtra("title", "VPN通知标题")
            putExtra("message", "VPN通知内容")
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 1, intentClick, PendingIntent.FLAG_IMMUTABLE)
        // 创建通知
        val builder = NotificationCompat.Builder(this, "my_channel_id")
            .setContentTitle("VPN服务正在运行")
            .setContentText(action)
            .setDeleteIntent(pendingIntent)
            .setSmallIcon(com.darcy.vpndemo.R.mipmap.ic_launcher)
        // 将服务设置为前台服务
        startForeground(NOTIFICATION_ID, builder.build())

    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "my_channel_id",
            "前台服务通道",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
