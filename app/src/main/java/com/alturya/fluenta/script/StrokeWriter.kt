package com.alturya.fluenta.script

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

// Práctica de escritura trazo-a-trazo (estilo Skritter), la forma en que aprenden
// los nativos. Usa Hanzi Writer (CDN) dentro de un WebView: muestra el orden de
// trazos animado y un modo "practicar" que valida cada trazo. Para glifos sin
// datos (p.ej. kana), Hanzi Writer dispara onLoadCharDataError → mostramos un
// fallback amable en vez de romper la pantalla.
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StrokeWriter(glyph: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadDataWithBaseURL("https://cdn.jsdelivr.net", strokeHtml(glyph), "text/html", "utf-8", null)
            }
        },
        update = { it.loadDataWithBaseURL("https://cdn.jsdelivr.net", strokeHtml(glyph), "text/html", "utf-8", null) },
    )
}

private fun strokeHtml(glyph: String): String {
    val safe = glyph.replace("\\", "\\\\").replace("'", "\\'")
    return """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<style>
  html,body{margin:0;padding:0;font-family:sans-serif;text-align:center;color:#444;background:transparent}
  #grid{margin:8px auto 0;border:1px dashed #bbb;width:300px;height:300px;position:relative}
  #grid:before,#grid:after{content:'';position:absolute;background:#eee}
  #grid:before{left:50%;top:0;bottom:0;width:1px}
  #grid:after{top:50%;left:0;right:0;height:1px}
  #target{width:300px;height:300px}
  .btn{display:inline-block;margin:10px 6px;padding:10px 16px;border-radius:10px;border:0;
       background:#3b5bfd;color:#fff;font-size:15px;font-weight:bold}
  .btn.alt{background:#eef0ff;color:#3b5bfd}
  #fallback{display:none;padding:24px;color:#666;font-size:15px}
  #big{font-size:120px;line-height:1.3}
</style></head><body>
  <div id="grid"><div id="target"></div></div>
  <div id="controls">
    <button class="btn" onclick="animate()">▶ Ver trazos</button>
    <button class="btn alt" onclick="practice()">✍️ Practicar</button>
  </div>
  <div id="fallback">
    <div id="big">$safe</div>
    La animación de trazos para este carácter llegará pronto.<br>Cópialo respetando el orden: arriba→abajo, izquierda→derecha.
  </div>
  <script src="https://cdn.jsdelivr.net/npm/hanzi-writer@3.5/dist/hanzi-writer.min.js"></script>
  <script>
    var writer = null;
    function showFallback(){ document.getElementById('grid').style.display='none';
      document.getElementById('controls').style.display='none';
      document.getElementById('fallback').style.display='block'; }
    try {
      writer = HanziWriter.create('target', '$safe', {
        width:300, height:300, padding:4, showOutline:true,
        strokeAnimationSpeed:1, delayBetweenStrokes:250,
        strokeColor:'#222', radicalColor:'#3b5bfd', outlineColor:'#ddd',
        onLoadCharDataError: function(){ showFallback(); }
      });
      writer.animateCharacter();
    } catch(e){ showFallback(); }
    function animate(){ if(writer) writer.animateCharacter(); }
    function practice(){ if(writer) writer.quiz({ showHintAfterMisses:2 }); }
  </script>
</body></html>
""".trimIndent()
}
