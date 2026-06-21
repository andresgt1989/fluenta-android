package com.alturya.fluenta.script

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

// Práctica de escritura trazo-a-trazo (estilo Skritter) con Hanzi Writer.
//
// CAMBIOS CLAVE vs la versión anterior (que "no dibujaba" y cuyos botones quedaban
// mudos):
//  1) El motor se carga EMPAQUETADO en el APK (file:///android_asset/), no del CDN.
//     Así nunca hay un ReferenceError silencioso que deje los botones sin hacer nada.
//  2) Estados VISIBLES: "Cargando…", error con botón "Reintentar". Nunca "no pasa nada".
//  3) Los datos del carácter se cargan con fallback (asset local → CDN), de modo que
//     funciona offline para los caracteres empaquetados y online para el resto.
//  4) El tamaño lo decide el llamador (modifier): esta vista se usa a PANTALLA COMPLETA
//     en HanziWriterScreen, fuera de cualquier lista que scrollee, así el gesto de
//     dibujar ya no pelea con el scroll.
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun StrokeWriter(glyph: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                // Mientras el dedo dibuja un trazo, impedir que un contenedor padre
                // robe el gesto como scroll (defensa extra; en pantalla completa ya no
                // hay scroll alrededor).
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN,
                        android.view.MotionEvent.ACTION_MOVE ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
                loadDataWithBaseURL("file:///android_asset/", strokeHtml(glyph), "text/html", "utf-8", null)
            }
        },
        update = { it.loadDataWithBaseURL("file:///android_asset/", strokeHtml(glyph), "text/html", "utf-8", null) },
    )
}

private fun strokeHtml(glyph: String): String {
    val safe = glyph.replace("\\", "\\\\").replace("'", "\\'")
    return """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<style>
  html,body{margin:0;padding:0;font-family:sans-serif;text-align:center;color:#444;background:transparent}
  #grid{margin:8px auto 0;border:1px dashed #bbb;width:300px;height:300px;position:relative;display:none}
  #grid:before,#grid:after{content:'';position:absolute;background:#eee}
  #grid:before{left:50%;top:0;bottom:0;width:1px}
  #grid:after{top:50%;left:0;right:0;height:1px}
  #target{width:300px;height:300px}
  .btn{display:inline-block;margin:10px 6px;padding:12px 18px;border-radius:12px;border:0;
       background:#3b5bfd;color:#fff;font-size:16px;font-weight:bold}
  .btn.alt{background:#eef0ff;color:#3b5bfd}
  #controls{display:none}
  #status{padding:28px;color:#666;font-size:15px}
  #big{font-size:120px;line-height:1.2;color:#222}
</style></head><body>
  <div id="status"><div id="big">$safe</div><div id="status-msg">Cargando trazos…</div>
    <button id="retry" class="btn" style="display:none" onclick="location.reload()">↻ Reintentar</button>
  </div>
  <div id="grid"><div id="target"></div></div>
  <div id="controls">
    <button class="btn" onclick="animate()">▶ Ver trazos</button>
    <button class="btn alt" onclick="practice()">✍️ Practicar</button>
  </div>
  <script src="hanzi-writer.min.js"></script>
  <script>
    var writer = null;
    function setMsg(t){ document.getElementById('status-msg').innerHTML = t; }
    function showError(t){ setMsg(t); document.getElementById('retry').style.display='inline-block'; }
    function ready(){
      document.getElementById('status').style.display='none';
      document.getElementById('grid').style.display='block';
      document.getElementById('controls').style.display='block';
    }
    // Hangul (coreano) no está en el dataset de Hanzi Writer: guía textual directa.
    var code = '$safe'.charCodeAt(0);
    var isHangul = (code >= 0xAC00 && code <= 0xD7A3) || (code >= 0x1100 && code <= 0x11FF);

    // Carga de datos con fallback: asset local empaquetado → CDN. Nunca silenciosa.
    function charDataLoader(ch, onComplete, onError){
      var local = 'hanzi-data/' + ch + '.json';
      fetch(local).then(function(r){ if(!r.ok) throw 0; return r.json(); })
        .then(onComplete)
        .catch(function(){
          fetch('https://cdn.jsdelivr.net/npm/hanzi-writer-data@2.0.1/' + encodeURIComponent(ch) + '.json')
            .then(function(r){ if(!r.ok) throw 0; return r.json(); })
            .then(onComplete)
            .catch(function(){ onError && onError(); });
        });
    }

    window.addEventListener('load', function(){
      if (isHangul) {
        showError('Carácter Hangul 한글 — traza horizontales antes que verticales, arriba→abajo, izquierda→derecha.');
        document.getElementById('retry').style.display='none';
        return;
      }
      if (typeof HanziWriter === 'undefined') {
        showError('No se pudo cargar el motor de trazos.');
        return;
      }
      try {
        writer = HanziWriter.create('target', '$safe', {
          width:300, height:300, padding:4, showOutline:true,
          strokeAnimationSpeed:1, delayBetweenStrokes:250,
          strokeColor:'#222', radicalColor:'#3b5bfd', outlineColor:'#ddd',
          charDataLoader: charDataLoader,
          onLoadCharDataSuccess: function(){ ready(); writer.animateCharacter(); },
          onLoadCharDataError: function(){ showError('Sin datos de trazo para este carácter. Revisa tu conexión.'); }
        });
      } catch(e){ showError('No se pudo iniciar la práctica de trazos.'); }
    });

    function animate(){ if(writer) writer.animateCharacter(); }
    function practice(){ if(writer) writer.quiz({ showHintAfterMisses:2 }); }
  </script>
</body></html>
""".trimIndent()
}
