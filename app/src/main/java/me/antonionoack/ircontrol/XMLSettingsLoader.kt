package me.antonionoack.ircontrol

import android.content.SharedPreferences
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/**
 * Preferences are stored as XML.
 * When changing from one compiling PC to another, Android forces you to delete all data.
 * Therefore, we save the data by hand, and load it using this class.
 * */
object XMLSettingsLoader {
    fun loadNewSettings(data: String, dst: SharedPreferences) {
        val parser = Xml.newPullParser()
        parser.setInput(data.byteInputStream(), null)
        var eventType = parser.eventType
        var key: String? = null
        var value: String? = null
        val edit = dst.edit()
        while (true) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "string") {
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i) == "name") {
                                key = parser.getAttributeValue(i)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "string" && key != null && value != null) {
                        edit.putString(key, value)
                    }
                    key = null
                    value = null
                }
                XmlPullParser.TEXT -> value = parser.text
                XmlPullParser.END_DOCUMENT -> break
            }
            eventType = parser.next()
        }
        edit.apply()
    }
}