package com.ejemplo.visorhtml

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ejemplo.visorhtml.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: android.content.SharedPreferences
    private val historial: MutableList<Pair<String, String>> = mutableListOf()

    private val abrirArchivo =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                abrirHtml(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("visor", MODE_PRIVATE)
        cargarHistorial()
        configurarWebView()

        binding.btnAbrir.setOnClickListener {
            abrirArchivo.launch(arrayOf("text/html", "application/xhtml+xml", "*/*"))
        }
        binding.btnHistorial.setOnClickListener { mostrarHistorial() }
        binding.btnLimpiar.setOnClickListener {
            prefs.edit().remove("historial").apply()
            historial.clear()
            binding.webView.loadUrl("about:blank")
            Toast.makeText(this, "Historial borrado", Toast.LENGTH_SHORT).show()
        }

        intent?.data?.let { abrirHtml(it) }
    }

    private fun configurarWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        binding.webView.webViewClient = WebViewClient()
    }

    private fun abrirHtml(uri: Uri) {
        val nombre = obtenerNombre(uri)
        binding.webView.loadUrl(uri.toString())
        agregarAlHistorial(nombre, uri.toString())
        title = nombre
    }

    private fun obtenerNombre(uri: Uri): String {
        var nombre = uri.lastPathSegment ?: "archivo.html"
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) nombre = c.getString(i) ?: nombre
        }
        return nombre
    }

    private fun agregarAlHistorial(nombre: String, uriStr: String) {
        historial.removeAll { it.second == uriStr }
        historial.add(0, nombre to uriStr)
        if (historial.size > 50) historial.removeAt(historial.size - 1)
        guardarHistorial()
    }

    private fun mostrarHistorial() {
        if (historial.isEmpty()) {
            Toast.makeText(this, "Historial vacío", Toast.LENGTH_SHORT).show()
            return
        }
        val nombres = historial.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Historial")
            .setItems(nombres) { _, i ->
                try {
                    abrirHtml(Uri.parse(historial[i].second))
                } catch (e: SecurityException) {
                    Toast.makeText(this, "Sin permiso, abre el archivo de nuevo", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun guardarHistorial() {
        prefs.edit().putString("historial", historial.joinToString(";;") { "${it.first}|${it.second}" }).apply()
    }

    private fun cargarHistorial() {
        prefs.getString("historial", "")?.split(";;")?.forEach { item ->
            val partes = item.split("|")
            if (partes.size == 2) historial.add(partes[0] to partes[1])
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) binding.webView.goBack()
        else super.onBackPressed()
    }
}
