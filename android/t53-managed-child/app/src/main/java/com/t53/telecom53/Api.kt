package com.t53.telecom53

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiException(val status: Int, message: String) : Exception(message)

/** Minimal HTTPS client for the T53 API. No third-party libraries. Call from a background thread. */
object Api {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun request(method: String, path: String, token: String?, body: JSONObject?): JSONObject {
        val url = URL(Config.API_BASE + path)
        require(url.protocol == "https") { "T53 API must use HTTPS" }
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val json = try { if (text.isBlank()) JSONObject() else JSONObject(text) } catch (_: JSONException) { JSONObject() }
            if (code !in 200..299) throw ApiException(code, json.optString("message", "").ifBlank { json.optString("error", "").replace('_', ' ') }.ifBlank { "Request failed ($code)" })
            return json
        } finally {
            conn.disconnect()
        }
    }

    fun enroll(licenseCode: String, childName: String, deviceId: String): JSONObject =
        request("POST", "/v1/devices/enroll", null, JSONObject()
            .put("licenseCode", licenseCode).put("childName", childName)
            .put("deviceId", deviceId).put("platform", "android"))

    fun status(deviceId: String, token: String, battery: Int, protectionActive: Boolean, appVersion: String, osVersion: String) =
        request("POST", "/v1/devices/${enc(deviceId)}/status", token, JSONObject()
            .put("battery", battery).put("protectionActive", protectionActive)
            .put("appVersion", appVersion).put("osVersion", osVersion))

    fun reportApps(deviceId: String, token: String, apps: Map<String, String>): JSONObject {
        val arr = JSONArray()
        for ((pkg, label) in apps) arr.put(JSONObject().put("package", pkg).put("label", label))
        return request("POST", "/v1/devices/${enc(deviceId)}/apps", token, JSONObject().put("apps", arr))
    }

    fun policy(deviceId: String, token: String): JSONObject =
        request("GET", "/v1/devices/${enc(deviceId)}/policy", token, null)

    fun decisions(deviceId: String, token: String): JSONObject =
        request("GET", "/v1/devices/${enc(deviceId)}/decisions", token, null)

    fun ackDecision(deviceId: String, token: String, requestId: String) =
        request("POST", "/v1/devices/${enc(deviceId)}/decisions/${enc(requestId)}/ack", token, JSONObject())

    fun requestApp(deviceId: String, token: String, pkg: String, reason: String, seconds: Long) =
        request("POST", "/v1/access-requests", token, JSONObject()
            .put("deviceId", deviceId).put("type", "appAccess").put("packageName", pkg)
            .put("reason", reason).put("durationSeconds", seconds))

    fun released(deviceId: String, token: String) =
        request("POST", "/v1/devices/${enc(deviceId)}/released", token, JSONObject())
}
