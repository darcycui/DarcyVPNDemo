package com.darcy.vpndemo

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.darcy.vpndemo.databinding.ActivityMainBinding
import com.darcy.vpndemo.service.TestVPNService

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val context: Context by lazy {
        this
    }
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val intentService: Intent by lazy {
        Intent(this, TestVPNService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkPermission()
        initView()
    }

    private fun readSP() {
        val sp = getSharedPreferences("vpn", MODE_PRIVATE)
        val vpn = sp.getString("vpn", "")
        binding.tvInfo.text = vpn
        Toast.makeText(this, vpn, Toast.LENGTH_SHORT).show()
    }

    private fun writeSP() {
        val sp = getSharedPreferences("vpn", MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString("vpn", "vpn").apply()
    }

    private fun checkPermission() {
        // 检查 POST_NOTIFICATIONS 权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户同意了权限请求
                Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                // 用户拒绝了权限请求
                Toast.makeText(this, "通知权限未授予,请在设置中授予", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                openVPN()
            } else {
                Toast.makeText(this, "resultCode not OK", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "requestCode not 100", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initView() {
        binding.apply {
            tvInfo.text = "open vpn" 
            openVPN.setOnClickListener {
                val intentVPN = VpnService.prepare(context)
                if (intentVPN != null) {
                    startActivityForResult(intentVPN, 100);
                } else {
                    onActivityResult(100, RESULT_OK, null);
                }
            }
            closeVPN.setOnClickListener {
                tvInfo.text = "close vpn"
                closeVPN()
            }
            startService.setOnClickListener {
                tvInfo.text = "start service"
                startVpnService()
            }
            stopService.setOnClickListener {
                tvInfo.text = "stop service"
                stopVpnService()
            }
            writeSP.setOnClickListener {
                writeSP()
            }
            readSP.setOnClickListener {
                readSP()
            }
        }
    }


    private fun startVpnService() {
        startService(intentService.also { it.action = TestVPNService.START })
    }

    private fun stopVpnService() {
        stopService(intentService.also { it.action = TestVPNService.STOP })
    }

    private fun openVPN() {
        startService(intentService.also { it.action = TestVPNService.OPEN_VPN })
    }

    private fun closeVPN() {
        startService(intentService.also { it.action = TestVPNService.CLOSE_VPN })
    }
}