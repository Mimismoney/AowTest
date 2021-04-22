package com.aowtest.myapplication

import android.util.JsonReader
import android.util.JsonToken
import org.json.JSONException
import java.io.StringReader
import java.util.regex.Pattern

class Point(var x: Int, var y: Int)
class Color(var r: Int, var g: Int, var b: Int)
class Rect(var left: Int, var top: Int, var width: Int, var height: Int) {
    constructor(rect: Rect) : this(rect.left, rect.top, rect.width, rect.height)
}

class DetectionLogic(var point: Point, var color: Color, var rect: Rect)
class PointData(var width: Int, var height: Int, var logic: Array<DetectionLogic>)
class PointDataParser {
    companion object {
        fun deserializeJson(input: String, height: Int) : PointData {
            val pattern = Pattern.compile("//.*$", Pattern.MULTILINE)
            val mather = pattern.matcher(input)
            val inputNoComment = mather.replaceAll("")
            val pointData = PointData(0, 0, arrayOf())
            val reader = JsonReader(StringReader(inputNoComment))
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "width" -> {
                        pointData.width = reader.nextInt()
                    }
                    "height" -> {
                        pointData.height = reader.nextInt()
                    }
                    "logic" -> {
                        pointData.logic = readDetectionLogic(reader, height - pointData.height)
                    }
                }
            }
            reader.endObject()
            return pointData
        }

        private fun readDetectionLogic(reader: JsonReader, heightDiff: Int): Array<DetectionLogic> {
            val arr = arrayListOf<DetectionLogic>()
            reader.beginArray()
            while (reader.hasNext()) {
                val logic = DetectionLogic(Point(0, 0), Color(0, 0, 0), Rect(0, 0, 0, 0))
                reader.beginArray()
                reader.beginArray()
                logic.point.x = reader.nextInt()
                logic.point.y = reader.nextInt()
                reader.endArray()
                reader.beginArray()
                logic.color.r = reader.nextInt()
                logic.color.g = reader.nextInt()
                logic.color.b = reader.nextInt()
                reader.endArray()
                reader.beginArray()
                logic.rect.left = reader.nextInt()
                logic.rect.top = reader.nextInt()
                logic.rect.width = reader.nextInt()
                logic.rect.height = reader.nextInt()
                reader.endArray()
                if (reader.peek() == JsonToken.NUMBER) {
                    logic.point.y = (heightDiff * reader.nextDouble() + logic.point.y).toInt()
                }
                if (reader.peek() == JsonToken.NUMBER) {
                    logic.rect.top = (heightDiff * reader.nextDouble() + logic.rect.top).toInt()
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