package com.quickmsg.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class WhatsAppAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val retryCount = AtomicInteger(0)

    companion object {
        val pendingSend = AtomicBoolean(false)
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val MAX_RETRIES = 30
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageNames = arrayOf(WHATSAPP_PACKAGE)
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!pendingSend.get()) return
        if (event?.packageName != WHATSAPP_PACKAGE) return

        val rootNode = rootInActiveWindow ?: return

        // Check if we're on the actual conversation screen (has text input field)
        val textInput = rootNode.findAccessibilityNodeInfosByViewId(
            "com.whatsapp:id/entry"
        )
        val textInputAlt = rootNode.findAccessibilityNodeInfosByViewId(
            "com.whatsapp:id/text_entry_box"
        )

        val isConversationScreen = textInput.isNotEmpty() || textInputAlt.isNotEmpty()

        if (!isConversationScreen) {
            // Still loading/searching — wait for conversation to open
            if (retryCount.incrementAndGet() > MAX_RETRIES) {
                pendingSend.set(false)
                retryCount.set(0)
            }
            return
        }

        if (findAndClickSend(rootNode)) {
            pendingSend.set(false)
            retryCount.set(0)
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_BACK)
                handler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }, 150)
            }, 300)
        }
    }

    private fun findAndClickSend(root: AccessibilityNodeInfo): Boolean {
        val sendButtons = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (sendButtons.isNotEmpty()) {
            sendButtons[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }

        return findNodeByContentDescription(root, "Send")
    }

    private fun findNodeByContentDescription(
        node: AccessibilityNodeInfo,
        description: String
    ): Boolean {
        if (node.contentDescription?.toString()?.equals(description, ignoreCase = true) == true) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findNodeByContentDescription(child, description)) return true
        }
        return false
    }

    override fun onInterrupt() {}
}
