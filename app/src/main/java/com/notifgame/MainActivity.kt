package com.notifgame

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private lateinit var sourceContainer: LinearLayout
    private lateinit var targetSpinner: Spinner

    private val sourceApps = mutableListOf<AppInfo>()
    private val sourceChecks = mutableMapOf<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun buildUi() {

        val scroll = ScrollView(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        scroll.addView(root)

        val title = TextView(this).apply {
            text = "NotifGame"
            textSize = 30f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 12)
        }

        root.addView(title)

        val description = TextView(this).apply {
            text =
                "Seçtiğin uygulamaların bildirimlerini yakalar, " +
                "bildirimi kaldırır ve yerine kendi oyun bildiriminizi gösterir."
            textSize = 16f
            setPadding(0, 0, 0, 24)
        }

        root.addView(description)

        val permissionButton = Button(this).apply {
            text = "Bildirim Erişimini Aç"
            setOnClickListener {
                try {
                    startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    )
                } catch (_: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Bildirim ayarları açılamadı.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        root.addView(permissionButton)

        val notificationPermissionButton =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(this).apply {
                    text = "Bildirim Gönderme İznini Ver"

                    setOnClickListener {
                        requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            100
                        )
                    }
                }
            } else {
                null
            }

        notificationPermissionButton?.let {
            root.addView(it)
        }

        val status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 12, 0, 24)
            tag = "status"
        }

        root.addView(status)

        val sourceTitle = TextView(this).apply {
            text = "Kaynak uygulamalar"
            textSize = 21f
            setTextColor(Color.BLACK)
            setPadding(0, 8, 0, 12)
        }

        root.addView(sourceTitle)

        sourceContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(sourceContainer)

        val targetTitle = TextView(this).apply {
            text = "Bildirime basınca açılacak uygulama"
            textSize = 21f
            setTextColor(Color.BLACK)
            setPadding(0, 24, 0, 12)
        }

        root.addView(targetTitle)

        targetSpinner = Spinner(this)

        root.addView(
            targetSpinner,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val customTitle = TextView(this).apply {
            text = "Gösterilecek bildirim"
            textSize = 21f
            setTextColor(Color.BLACK)
            setPadding(0, 24, 0, 12)
        }

        root.addView(customTitle)

        val notificationTitle = EditText(this).apply {
            hint = "Bildirim başlığı"
            setSingleLine(true)
            setText(
                prefs.getString(
                    "notification_title",
                    "🎮 OYUN ZAMANI"
                )
            )
        }

        root.addView(notificationTitle)

        val notificationText = EditText(this).apply {
            hint = "Bildirim metni"
            setSingleLine(false)
            setText(
                prefs.getString(
                    "notification_text",
                    "Yeni görev hazır! Devam etmek için dokun."
                )
            )
        }

        root.addView(notificationText)

        val saveButton = Button(this).apply {
            text = "AYARLARI KAYDET"

            setOnClickListener {

                val selectedPackages = sourceChecks
                    .filterValues { it.isChecked }
                    .keys
                    .toSet()

                val targetPosition = targetSpinner.selectedItemPosition

                if (selectedPackages.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "En az bir kaynak uygulama seç.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                if (targetPosition < 0 ||
                    targetPosition >= sourceApps.size
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        "Hedef uygulama seç.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val targetPackage =
                    sourceApps[targetPosition].packageName

                prefs.edit()
                    .putStringSet(
                        "source_packages",
                        selectedPackages
                    )
                    .putString(
                        "target_package",
                        targetPackage
                    )
                    .putString(
                        "notification_title",
                        notificationTitle.text.toString()
                    )
                    .putString(
                        "notification_text",
                        notificationText.text.toString()
                    )
                    .apply()

                Toast.makeText(
                    this@MainActivity,
                    "Ayarlar kaydedildi.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        root.addView(saveButton)

        val testButton = Button(this).apply {
            text = "TEST BİLDİRİMİ"

            setOnClickListener {
                NotificationListener.showReplacementNotification(this@MainActivity)
            }
        }

        root.addView(testButton)

        val openTargetButton = Button(this).apply {
            text = "SEÇİLİ UYGULAMAYI AÇ"

            setOnClickListener {
                val packageName = prefs.getString(
                    "target_package",
                    null
                )

                if (packageName == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Önce hedef uygulama seç.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                openApp(packageName)
            }
        }

        root.addView(openTargetButton)

        setContentView(scroll)

        updateStatus()
    }

    private fun updateStatus() {

        val statusView =
            findViewByTag<View>("status") as? TextView
                ?: return

        val enabled = isNotificationListenerEnabled()

        statusView.text =
            if (enabled) {
                "🟢 Bildirim erişimi: AKTİF"
            } else {
                "🔴 Bildirim erişimi: KAPALI"
            }
    }

    private fun loadApps() {

        val pm = packageManager

        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            PackageManager.MATCH_ALL
        )
            .map {
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .filter {
                it.packageName != packageName
            }
            .distinctBy {
                it.packageName
            }
            .sortedBy {
                it.label.lowercase()
            }

        sourceApps.clear()
        sourceApps.addAll(apps)

        sourceContainer.removeAllViews()
        sourceChecks.clear()

        val savedSources =
            prefs.getStringSet(
                "source_packages",
                emptySet()
            )

        for (app in sourceApps) {

            val checkBox = CheckBox(this).apply {

                text = "${app.label}\n${app.packageName}"

                textSize = 15f

                isChecked =
                    savedSources?.contains(app.packageName) == true

                setPadding(0, 8, 0, 8)
            }

            sourceChecks[app.packageName] = checkBox

            sourceContainer.addView(checkBox)
        }

        val labels = sourceApps.map {
            it.label
        }

        targetSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                labels
            )

        val savedTarget =
            prefs.getString(
                "target_package",
                null
            )

        if (savedTarget != null) {

            val index =
                sourceApps.indexOfFirst {
                    it.packageName == savedTarget
                }

            if (index >= 0) {
                targetSpinner.setSelection(index)
            }
        }
    }

    private fun openApp(packageName: String) {

        val launchIntent =
            packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent == null) {
            Toast.makeText(
                this,
                "Uygulama açılamadı.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
        )

        startActivity(launchIntent)
    }

    private fun isNotificationListenerEnabled(): Boolean {

        val cn = ComponentName(
            this,
            NotificationListener::class.java
        )

        val enabledPackages =
            Settings.Secure.getString(
                contentResolver,
                "enabled_notification_listeners"
            )

        return enabledPackages
            ?.contains(cn.flattenToString()) == true
    }

    private fun View.findViewByTag(tagName: String): View? {

        if (tag == tagName) {
            return this
        }

        if (this is android.view.ViewGroup) {
            for (i in 0 until childCount) {
                val result =
                    getChildAt(i).findViewByTag(tagName)

                if (result != null) {
                    return result
                }
            }
        }

        return null
    }
}
