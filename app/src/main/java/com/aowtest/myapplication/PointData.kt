package com.aowtest.myapplication

import android.util.JsonReader
import android.util.JsonToken
import org.json.JSONException
import java.io.StringReader
import java.util.regex.Pattern
import kotlin.math.log

enum class PointRelation {
    TOP,
    MIDDLE,
    BOTTOM
}

data class Point(var x: UShort, var y: UShort)
data class Color(var r: UByte, var g: UByte, var b: UByte)
data class Rect(var left: UShort, var top: UShort, var width: UShort, var height: UShort) {
    constructor(rect: Rect) : this(rect.left, rect.top, rect.width, rect.height)
}

data class DetectionLogic(var point: Point, var color: Color, var rect: Rect)
data class PointData(var width: UShort, var height: UShort, var logic: Array<DetectionLogic>)
class PointDataParser {
    companion object {
        fun deserializeJson(input: String, height: Int) : PointData {
            var pattern = Pattern.compile("//.*$", Pattern.MULTILINE)
            var mather = pattern.matcher(input)
            var inputNoComment = mather.replaceAll("")
            var pointData = PointData(0u, 0u, arrayOf())
            var reader = JsonReader(StringReader(inputNoComment))
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "width" -> {
                        pointData.width = reader.nextInt().toUShort()
                    }
                    "height" -> {
                        pointData.height = reader.nextInt().toUShort()
                    }
                    "logic" -> {
                        pointData.logic = readDetectionLogic(reader, height - pointData.height.toInt() )
                    }
                }
            }
            reader.endObject()
            return pointData
        }
        private fun readDetectionLogic(reader: JsonReader, heightDiff: Int): Array<DetectionLogic> {
            var arr = arrayListOf<DetectionLogic>()
            reader.beginArray()
            while (reader.hasNext()) {
                var logic = DetectionLogic(Point(0u, 0u), Color(0u, 0u, 0u), Rect(0u, 0u, 0u, 0u))
                reader.beginArray()
                reader.beginArray()
                logic.point.x = reader.nextInt().toUShort()
                logic.point.y = reader.nextInt().toUShort()
                reader.endArray()
                reader.beginArray()
                logic.color.r = reader.nextInt().toUByte()
                logic.color.g = reader.nextInt().toUByte()
                logic.color.b = reader.nextInt().toUByte()
                reader.endArray()
                reader.beginArray()
                logic.rect.left = reader.nextInt().toUShort()
                logic.rect.top = reader.nextInt().toUShort()
                logic.rect.width = reader.nextInt().toUShort()
                logic.rect.height = reader.nextInt().toUShort()
                reader.endArray()
                if (reader.peek() == JsonToken.NUMBER) {
                    logic.point.y = ((heightDiff.toDouble() * reader.nextDouble()) + logic.point.y.toInt()).toShort().toUShort()
                }
                if (reader.peek() == JsonToken.NUMBER) {
                    logic.rect.top = ((heightDiff.toDouble() * reader.nextDouble()) + logic.rect.top.toInt()).toShort().toUShort()
                }
                reader.endArray()
                arr.add(logic)
            }
            if (arr.size != 23) {
                throw JSONException("")
            }
            reader.endArray()
            return arr.toTypedArray()
        }
    }
}