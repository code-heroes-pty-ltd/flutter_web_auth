package com.linusu.flutter_web_auth

import android.app.Activity
import android.content.Intent
import android.net.Uri

import androidx.browser.customtabs.CustomTabsIntent

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterWebAuthPlugin() : MethodCallHandler, FlutterPlugin, ActivityAware {
  companion object {
    val callbacks = mutableMapOf<String, Result>()

    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val plugin = FlutterWebAuthPlugin()
      plugin.initInstance(registrar.messenger())
    }

  }

  private var activity: Activity? = null
  private var channel: MethodChannel? = null

  override fun onDetachedFromActivity() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  fun initInstance(messenger: BinaryMessenger) {
    channel = MethodChannel(messenger, "flutter_web_auth")
    channel?.setMethodCallHandler(this)
  }

  override public fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    initInstance(binding.getBinaryMessenger())
  }

  override public fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel = null
  }

  override fun onMethodCall(call: MethodCall, resultCallback: Result) {
    when (call.method) {
      "authenticate" -> {
        val url = Uri.parse(call.argument("url"))
        val callbackUrlScheme = call.argument<String>("callbackUrlScheme")!!
        val preferEphemeral = call.argument<Boolean>("preferEphemeral")!!

        callbacks[callbackUrlScheme] = resultCallback

        val intent = CustomTabsIntent.Builder().build()
        val keepAliveIntent = Intent(activity!!, KeepAliveService::class.java)

        if (preferEphemeral) {
          intent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        intent.intent.putExtra("android.support.customtabs.extra.KEEP_ALIVE", keepAliveIntent)

        intent.launchUrl(activity!!, url)
      }
      "cleanUpDanglingCalls" -> {
        callbacks.forEach { (_, danglingResultCallback) ->
          danglingResultCallback.error("CANCELED", "User canceled login", null)
        }
        callbacks.clear()
        resultCallback.success(null)
      }
      else -> resultCallback.notImplemented()
    }
  }
}
