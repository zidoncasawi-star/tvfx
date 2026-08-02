package com.example.data

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Resolver يحاول DNS النظام أولاً، وإن فشل أو رجع فارغاً يلجأ إلى Cloudflare DNS-over-HTTPS.
 * هذا يتجاوز حظر/تسميم DNS من طرف الراوتر أو مزوّد الإنترنت (وهو النمط الأشيع لحظر نطاقات IPTV)
 * دون الحاجة لأي VPN، لأن طلب DoH يمر عبر HTTPS العادي إلى 1.1.1.1 الذي نادراً ما يُحظر بمفرده.
 */
object DohDns : Dns {

    private val dohClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun lookup(hostname: String): List<InetAddress> {
        try {
            val system = Dns.SYSTEM.lookup(hostname)
            if (system.isNotEmpty()) return system
        } catch (_: Throwable) {
            // النظام فشل بالحل (غالباً DNS محظور/مسمّم) — نكمل لمحاولة DoH
        }

        return try {
            resolveViaDoh(hostname)
        } catch (_: Throwable) {
            throw java.net.UnknownHostException(hostname)
        }
    }

    private fun resolveViaDoh(hostname: String): List<InetAddress> {
        val request = Request.Builder()
            .url("https://cloudflare-dns.com/dns-query?name=$hostname&type=A")
            .header("Accept", "application/dns-json")
            .build()

        dohClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val json = org.json.JSONObject(body)
            val answers = json.optJSONArray("Answer") ?: return emptyList()
            val addresses = mutableListOf<InetAddress>()
            for (i in 0 until answers.length()) {
                val entry = answers.getJSONObject(i)
                if (entry.optInt("type") == 1) { // A record
                    val ip = entry.optString("data")
                    if (ip.isNotBlank()) {
                        try {
                            addresses.add(InetAddress.getByName(ip))
                        } catch (_: Throwable) { }
                    }
                }
            }
            return addresses
        }
    }
}
