package com.notifgame

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences(
            "settings",
            Context.MODE_PRIVATE
        )
    }

    private val sourceApps =
        mutableListOf<AppInfo>()

    private val selectedSourcePackages =
        mutableSetOf<String>()

    private var targetPackage: String? = null

    private lateinit var sourceSummary: TextView
    private lateinit var targetSummary: TextView
    private lateinit var statusSummary: TextView

    private lateinit var notificationTitleInput: EditText
    private lateinit var notificationTextInput: EditText

    companion object {

        private const val PINK = "#E0777D"
        private const val CREAM = "#FDD692"
        private const val BURGUNDY = "#8E3B46"
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        selectedSourcePackages.clear()

        prefs.getStringSet(
            "source_packages",
            emptySet()
        )?.let {
            selectedSourcePackages.addAll(it)
        }

        targetPackage =
            prefs.getString(
                "target_package",
                null
            )

        buildUi()
        updateStatus()

        loadAppsAsync()
    }

    override fun onResume() {
        super.onResume()

        if (::statusSummary.isInitialized) {
            updateStatus()
        }
    }

    // --------------------------------------------------
    // APP LOADING
    // --------------------------------------------------

    private fun loadAppsAsync() {

        Thread {

            val pm = packageManager

            val apps =
                pm.getInstalledApplications(
                    PackageManager.GET_META_DATA
                )
                    .filter { app ->
                        app.packageName != packageName
                    }
                    .mapNotNull { app ->

                        val label =
                            try {
                                app.loadLabel(
                                    pm
                                ).toString()
                            } catch (_: Exception) {
                                null
                            }

                        if (
                            label.isNullOrBlank()
                        ) {
                            null
                        } else {
                            AppInfo(
                                label = label,
                                packageName =
                                    app.packageName
                            )
                        }
                    }
                    .distinctBy {
                        it.packageName
                    }
                    .sortedBy {
                        it.label.lowercase()
                    }

            runOnUiThread {

                sourceApps.clear()
                sourceApps.addAll(apps)

                updateSelectorTexts()
            }

        }.start()
    }

    // --------------------------------------------------
    // UI
    // --------------------------------------------------

    private fun buildUi() {

        val scrollView =
            ScrollView(this).apply {

                setBackgroundColor(
                    Color.parseColor(CREAM)
                )
            }

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(22),
                    dp(22),
                    dp(22),
                    dp(30)
                )
            }

        scrollView.addView(root)

        // HEADER

        val title =
            TextView(this).apply {

                text = "kNotification"

                textSize = 30f

                setTextColor(
                    Color.parseColor(BURGUNDY)
                )

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )

                setPadding(
                    0,
                    0,
                    0,
                    dp(5)
                )
            }

        root.addView(title)

        val subtitle =
            TextView(this).apply {

                text =
                    "Bildirimlerini istediğin gibi yönlendir."

                textSize = 15f

                setTextColor(
                    Color.parseColor(BURGUNDY)
                )

                setPadding(
                    0,
                    0,
                    0,
                    dp(22)
                )
            }

        root.addView(subtitle)

        // --------------------------------------------------
        // SERVICE STATUS
        // --------------------------------------------------

        val statusCard =
            createCard()

        statusCard.addView(
            createSectionLabel(
                "SERVİS DURUMU"
            )
        )

        statusSummary =
            TextView(this).apply {

                textSize = 15f

                setPadding(
                    0,
                    dp(7),
                    0,
                    dp(8)
                )

                setTextColor(
                    Color.parseColor(BURGUNDY)
                )
            }

        statusCard.addView(statusSummary)

        val notificationAccessButton =
            createButton(
                "Bildirim erişimini aç"
            )

        notificationAccessButton.setOnClickListener {

            try {

                startActivity(
                    Intent(
                        Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                    )
                )

            } catch (_: Exception) {

                Toast.makeText(
                    this,
                    "Ayarlar açılamadı.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        statusCard.addView(
            notificationAccessButton,
            buttonParams()
        )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val notificationPermissionButton =
                createSecondaryButton(
                    "Bildirim iznini aç"
                )

            notificationPermissionButton
                .setOnClickListener {

                    requestPermissions(
                        arrayOf(
                            Manifest.permission.POST_NOTIFICATIONS
                        ),
                        100
                    )
                }

            statusCard.addView(
                notificationPermissionButton,
                buttonParams()
            )
        }

        // Kullanım erişimi
        val usageAccessButton =
            createSecondaryButton(
                "Uygulama kullanım erişimini aç"
            )

        usageAccessButton.setOnClickListener {
            openUsageAccessSettings()
        }

        statusCard.addView(
            usageAccessButton,
            buttonParams()
        )

        val usageDescription =
            createDescription(
                "Önde açık olan kaynak uygulamayı algılamak için gereklidir."
            )

        statusCard.addView(
            usageDescription
        )

        root.addView(
            statusCard,
            cardParams()
        )

        // --------------------------------------------------
        // SOURCE APPS
        // --------------------------------------------------

        val sourceCard =
            createCard()

        sourceCard.addView(
            createSectionLabel(
                "KAYNAK UYGULAMALAR"
            )
        )

        sourceCard.addView(
            createDescription(
                "Bildirimlerini değiştirmek istediğin uygulamaları seç."
            )
        )

        sourceSummary =
            createSelectorSummary()

        sourceSummary.setOnClickListener {
            showSourcePicker()
        }

        sourceCard.addView(
            sourceSummary,
            selectorParams()
        )

        root.addView(
            sourceCard,
            cardParams()
        )

        // --------------------------------------------------
        // TARGET APP
        // --------------------------------------------------

        val targetCard =
            createCard()

        targetCard.addView(
            createSectionLabel(
                "AÇILACAK UYGULAMA"
            )
        )

        targetCard.addView(
            createDescription(
                "Değiştirilen bildirime dokunulduğunda açılacak uygulamayı belirle."
            )
        )

        targetSummary =
            createSelectorSummary()

        targetSummary.setOnClickListener {
            showTargetPicker()
        }

        targetCard.addView(
            targetSummary,
            selectorParams()
        )

        root.addView(
            targetCard,
            cardParams()
        )

        // --------------------------------------------------
        // NOTIFICATION
        // --------------------------------------------------

        val notificationCard =
            createCard()

        notificationCard.addView(
            createSectionLabel(
                "YENİ BİLDİRİM"
            )
        )

        notificationCard.addView(
            createDescription(
                "Kaynak uygulamanın yerine gösterilecek bildirimin içeriğini belirle."
            )
        )

        notificationTitleInput =
            createInput(
                "Başlık",
                prefs.getString(
                    "notification_title",
                    "Bildirim"
                )
            )

        notificationCard.addView(
            notificationTitleInput,
            inputParams()
        )

        notificationTextInput =
            createInput(
                "Açıklama",
                prefs.getString(
                    "notification_text",
                    "Yeni bildirim var."
                )
            )

        notificationTextInput.minLines = 2

        notificationCard.addView(
            notificationTextInput,
            inputParams()
        )

        root.addView(
            notificationCard,
            cardParams()
        )

        // --------------------------------------------------
        // SAVE
        // --------------------------------------------------

        val saveButton =
            createButton(
                "Ayarları kaydet"
            )

        saveButton.setOnClickListener {
            saveSettings()
        }

        root.addView(
            saveButton,
            largeButtonParams()
        )

        // --------------------------------------------------
        // TEST
        // --------------------------------------------------

        val testButton =
            createSecondaryButton(
                "Test bildirimi gönder"
            )

        testButton.setOnClickListener {

            NotificationListener
                .showReplacementNotification(
                    this
                )
        }

        root.addView(
            testButton,
            largeButtonParams()
        )

        setContentView(scrollView)

        updateSelectorTexts()
    }

    // --------------------------------------------------
    // USAGE ACCESS
    // --------------------------------------------------

    private fun openUsageAccessSettings() {

        try {

            startActivity(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Kullanım erişimi ayarları açılamadı.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --------------------------------------------------
    // SOURCE PICKER
    // --------------------------------------------------

    private fun showSourcePicker() {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(8),
                    dp(18),
                    dp(8)
                )
            }

        val search =
            createInput(
                "Uygulama ara",
                ""
            )

        container.addView(
            search,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )

        val listScroll =
            ScrollView(this)

        val list =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        listScroll.addView(list)

        container.addView(
            listScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(440)
            )
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Kaynak uygulamalar"
                )
                .setView(container)
                .setPositiveButton(
                    "Tamam"
                ) { d, _ ->

                    updateSelectorTexts()

                    d.dismiss()
                }
                .create()

        fun refreshList(
            query: String
        ) {

            list.removeAllViews()

            val filtered =
                sourceApps.filter {
                    it.label.contains(
                        query.trim(),
                        ignoreCase = true
                    )
                }

            for (app in filtered) {

                val checkBox =
                    CheckBox(this).apply {

                        text = app.label

                        textSize = 16f

                        setTextColor(
                            Color.parseColor(
                                BURGUNDY
                            )
                        )

                        buttonTintList =
                            ColorStateList.valueOf(
                                Color.parseColor(
                                    BURGUNDY
                                )
                            )

                        isChecked =
                            selectedSourcePackages
                                .contains(
                                    app.packageName
                                )

                        setPadding(
                            0,
                            dp(7),
                            0,
                            dp(7)
                        )

                        setOnCheckedChangeListener {
                                _,
                                checked ->

                            if (checked) {

                                selectedSourcePackages
                                    .add(
                                        app.packageName
                                    )

                            } else {

                                selectedSourcePackages
                                    .remove(
                                        app.packageName
                                    )
                            }
                        }
                    }

                list.addView(
                    checkBox,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(52)
                    )
                )
            }
        }

        search.addTextChangedListener(
            SimpleTextWatcher {
                refreshList(it)
            }
        )

        refreshList("")

        dialog.show()

        dialog.getButton(
            AlertDialog.BUTTON_POSITIVE
        )?.setTextColor(
            Color.parseColor(BURGUNDY)
        )
    }

    // --------------------------------------------------
    // TARGET PICKER
    // --------------------------------------------------

    private fun showTargetPicker() {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(8),
                    dp(18),
                    dp(8)
                )
            }

        val search =
            createInput(
                "Uygulama ara",
                ""
            )

        container.addView(
            search,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )

        val listScroll =
            ScrollView(this)

        val list =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        listScroll.addView(list)

        container.addView(
            listScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(440)
            )
        )

        var newTarget =
            targetPackage

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Açılacak uygulama"
                )
                .setView(container)
                .setPositiveButton(
                    "Seç"
                ) { d, _ ->

                    targetPackage =
                        newTarget

                    updateSelectorTexts()

                    d.dismiss()
                }
                .setNegativeButton(
                    "Vazgeç",
                    null
                )
                .create()

        fun refreshList(
            query: String
        ) {

            list.removeAllViews()

            val filtered =
                sourceApps.filter {
                    it.label.contains(
                        query.trim(),
                        ignoreCase = true
                    )
                }

            for (app in filtered) {

                val row =
                    TextView(this).apply {

                        text = app.label

                        textSize = 16f

                        gravity =
                            Gravity.CENTER_VERTICAL

                        setTextColor(
                            Color.parseColor(
                                BURGUNDY
                            )
                        )

                        setPadding(
                            dp(14),
                            0,
                            dp(14),
                            0
                        )

                        background =
                            createRowBackground()

                        alpha =
                            if (
                                app.packageName ==
                                newTarget
                            ) {
                                1f
                            } else {
                                0.55f
                            }

                        setOnClickListener {

                            newTarget =
                                app.packageName

                            for (
                                i in
                                0 until list.childCount
                            ) {

                                list.getChildAt(
                                    i
                                ).alpha = 0.55f
                            }

                            alpha = 1f
                        }
                    }

                list.addView(
                    row,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(54)
                    ).apply {

                        setMargins(
                            0,
                            dp(4),
                            0,
                            dp(4)
                        )
                    }
                )
            }
        }

        search.addTextChangedListener(
            SimpleTextWatcher {
                refreshList(it)
            }
        )

        refreshList("")

        dialog.show()

        dialog.getButton(
            AlertDialog.BUTTON_POSITIVE
        )?.setTextColor(
            Color.parseColor(BURGUNDY)
        )

        dialog.getButton(
            AlertDialog.BUTTON_NEGATIVE
        )?.setTextColor(
            Color.parseColor(BURGUNDY)
        )
    }

    // --------------------------------------------------
    // SAVE
    // --------------------------------------------------

    private fun saveSettings() {

        if (
            selectedSourcePackages.isEmpty()
        ) {

            Toast.makeText(
                this,
                "En az bir kaynak uygulama seç.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            targetPackage.isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                "Açılacak uygulamayı seç.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        prefs.edit()
            .putStringSet(
                "source_packages",
                selectedSourcePackages
            )
            .putString(
                "target_package",
                targetPackage
            )
            .putString(
                "notification_title",
                notificationTitleInput
                    .text
                    .toString()
                    .trim()
            )
            .putString(
                "notification_text",
                notificationTextInput
                    .text
                    .toString()
                    .trim()
            )
            .apply()

        Toast.makeText(
            this,
            "Ayarlar kaydedildi.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // --------------------------------------------------
    // UI HELPERS
    // --------------------------------------------------

    private fun createCard():
            LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
            )

            background =
                GradientDrawable().apply {

                    setColor(
                        Color.parseColor(PINK)
                    )

                    setStroke(
                        dp(2),
                        Color.parseColor(BURGUNDY)
                    )

                    cornerRadius =
                        dp(18).toFloat()
                }
        }
    }

    private fun createSectionLabel(
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text = text

            textSize = 13f

            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

            setTextColor(
                Color.parseColor(BURGUNDY)
            )

            letterSpacing = 0.08f
        }
    }

    private fun createDescription(
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text = text

            textSize = 14f

            setTextColor(
                Color.parseColor(BURGUNDY)
            )

            setPadding(
                0,
                dp(7),
                0,
                dp(13)
            )
        }
    }

    private fun createSelectorSummary():
            TextView {

        return TextView(this).apply {

            textSize = 16f

            gravity =
                Gravity.CENTER_VERTICAL

            setTextColor(
                Color.parseColor(BURGUNDY)
            )

            background =
                createRowBackground()

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )

            isClickable = true
        }
    }

    private fun createInput(
        hint: String,
        value: String?
    ): EditText {

        return EditText(this).apply {

            this.hint = hint

            setText(
                value ?: ""
            )

            textSize = 15f

            setSingleLine(true)

            setTextColor(
                Color.parseColor(BURGUNDY)
            )

            setHintTextColor(
                Color.parseColor(BURGUNDY)
            )

            background =
                createRowBackground()

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )
        }
    }

    private fun createButton(
        text: String
    ): Button {

        return Button(this).apply {

            this.text = text

            textSize = 14f

            isAllCaps = false

            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

            setTextColor(
                Color.parseColor(CREAM)
            )

            background =
                GradientDrawable().apply {

                    setColor(
                        Color.parseColor(BURGUNDY)
                    )

                    cornerRadius =
                        dp(13).toFloat()
                }
        }
    }

    private fun createSecondaryButton(
        text: String
    ): Button {

        return Button(this).apply {

            this.text = text

            textSize = 14f

            isAllCaps = false

            setTextColor(
                Color.parseColor(BURGUNDY)
            )

            background =
                GradientDrawable().apply {

                    setColor(
                        Color.parseColor(CREAM)
                    )

                    setStroke(
                        dp(2),
                        Color.parseColor(BURGUNDY)
                    )

                    cornerRadius =
                        dp(13).toFloat()
                }
        }
    }

    private fun createRowBackground():
            GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                Color.parseColor(CREAM)
            )

            setStroke(
                dp(1),
                Color.parseColor(BURGUNDY)
            )

            cornerRadius =
                dp(12).toFloat()
        }
    }

    private fun buttonParams():
            LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(52)
        ).apply {

            topMargin = dp(10)
        }
    }

    private fun largeButtonParams():
            LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56)
        ).apply {

            topMargin = dp(12)
        }
    }

    private fun cardParams():
            LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {

            bottomMargin = dp(14)
        }
    }

    private fun selectorParams():
            LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56)
        )
    }

    private fun inputParams():
            LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56)
        ).apply {

            topMargin = dp(8)
        }
    }

    // --------------------------------------------------
    // STATE
    // --------------------------------------------------

    private fun updateSelectorTexts() {

        if (!::sourceSummary.isInitialized) {
            return
        }

        val count =
            selectedSourcePackages.size

        sourceSummary.text =
            when (count) {

                0 ->
                    "Uygulamaları seç"

                1 ->
                    "1 uygulama seçildi"

                else ->
                    "$count uygulama seçildi"
            }

        val target =
            sourceApps.firstOrNull {
                it.packageName ==
                    targetPackage
            }

        targetSummary.text =
            target?.label
                ?: "Uygulama seç"
    }

    private fun updateStatus() {

        if (!::statusSummary.isInitialized) {
            return
        }

        statusSummary.text =
            if (
                isNotificationListenerEnabled()
            ) {
                "Bildirim erişimi aktif."
            } else {
                "Bildirim erişimi kapalı."
            }
    }

    private fun isNotificationListenerEnabled():
            Boolean {

        val component =
            ComponentName(
                this,
                NotificationListener::class.java
            )

        val enabled =
            Settings.Secure.getString(
                contentResolver,
                "enabled_notification_listeners"
            )

        return enabled
            ?.split(":")
            ?.contains(
                component.flattenToString()
            ) == true
    }

    // --------------------------------------------------
    // UTIL
    // --------------------------------------------------

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    private class SimpleTextWatcher(
        private val callback:
            (String) -> Unit
    ) :
        android.text.TextWatcher {

        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {

            callback(
                s?.toString() ?: ""
            )
        }

        override fun afterTextChanged(
            s: android.text.Editable?
        ) {
        }
    }
}
