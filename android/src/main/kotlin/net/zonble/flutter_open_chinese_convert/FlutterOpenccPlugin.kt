package net.zonble.flutter_open_chinese_convert

import androidx.annotation.NonNull
import android.content.Context
import com.zqc.opencc.android.lib.ChineseConverter
import com.zqc.opencc.android.lib.ConversionType
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.embedding.engine.plugins.FlutterPlugin
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO

class FlutterOpenccPlugin: FlutterPlugin, MethodCallHandler {
  private lateinit var channel : MethodChannel
  private lateinit var context: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_open_chinese_convert")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private var job: Job? = null

  private fun typeOf(option: String): ConversionType? = when (option) {
    "s2t" -> ConversionType.S2T
    "t2s" -> ConversionType.T2S
    "s2hk" -> ConversionType.S2HK
    "hk2s" -> ConversionType.HK2S
    "s2tw" -> ConversionType.S2TW
    "tw2s" -> ConversionType.TW2S
    "s2twp" -> ConversionType.S2TWP
    "tw2sp" -> ConversionType.TW2SP
    "t2tw" -> ConversionType.T2TW
    "t2hk" -> ConversionType.T2HK
    else -> null
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "convert" -> {
        val arguments = call.arguments as? ArrayList<*>
        arguments?.also {
          val text = it[0] as String
          val typeString = it[1] as String
          val type = typeOf(typeString)
          val inBackground = it[2] as Boolean
          type?.let { option ->
            if (inBackground) {
              CoroutineScope(IO).launch {
                convert(text, type, result)
              }
            } else {
              val converted = ChineseConverter.convert(text, option, context)
              result.success(converted)
            }
          } ?: result.error("Not supported", null, null)
        }
      }
      else -> result.notImplemented()
    }
  }

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val context = registrar.activeContext()
      val channel = MethodChannel(registrar.messenger(), "flutter_open_chinese_convert")
      channel.setMethodCallHandler(FlutterOpenccPlugin())
    }
  }

  private suspend fun convert(text: String, option: ConversionType, result: Result) {
    withContext(IO) {
      val converted = ChineseConverter.convert(text, option, context)
      withContext(Dispatchers.Main) {
        result.success(converted)
      }
    }
  }
}