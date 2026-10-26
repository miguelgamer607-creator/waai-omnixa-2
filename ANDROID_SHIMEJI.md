# Waai Shimeji para Android

Waai Shimeji es el complemento nativo de Waai Companion. La PWA existente sigue siendo el hogar para administrar tareas, recuerdos, investigaciones y configuración; esta app Android añade la mascota superpuesta, el chat y los avisos proactivos.

## Cómo obtener el APK desde el celular

1. Abre la pestaña **Actions** del repositorio: https://github.com/miguelgamer607-creator/waai-omnixa-2/actions
2. Selecciona el flujo **Android Shimeji APK** y abre su ejecución más reciente en la rama main.
3. Confirma que haya terminado correctamente. En la parte inferior busca **Artifacts → waai-shimeji-debug-apk** y descarga el ZIP.
4. Extrae el ZIP con el gestor de archivos de Android. Dentro estará el archivo app-debug.apk.
5. Pulsa el APK para instalarlo. Android puede pedir que autorices temporalmente a tu navegador o gestor de archivos a instalar aplicaciones desconocidas. Autoriza solo la aplicación desde la que descargaste este APK, instálalo y luego puedes retirar ese permiso.
6. Esta es una compilación **debug** firmada con la clave de desarrollo del workflow: sirve para probar el proyecto. No es una publicación de Google Play ni una versión release firmada con una clave de distribución propia.

El artefacto se puede descargar desde el teléfono; no necesitas instalar Android Studio en él. El flujo de GitHub Actions compila el APK.

## Configurar el servidor

1. Abre Waai Shimeji.
2. En tu Codespace, mantén el servidor Waai activo y abre la pestaña **Ports / Puertos**.
3. Copia la URL HTTPS del puerto 8000; debe parecerse a https://NOMBRE-DEL-CODESPACE-8000.app.github.dev.
4. **Un port privado puede exigir la sesión de GitHub del navegador y no ser accesible desde la app Android.** No conviertas el port en público mientras el backend esté en modo desarrollo sin token.
5. Si el port privado no permite la conexión nativa, genera un token aleatorio en la terminal:

   ```bash
   python -c "import secrets; print(secrets.token_urlsafe(36))"
   ```

   Copia el resultado y arranca el servidor con acceso protegido, sustituyendo EL_TOKEN por ese resultado:

   ```bash
   APP_ENV=production WAAI_ACCESS_TOKEN='EL_TOKEN' WAAI_RESEARCH_ENABLED=false python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```

   Solo después de activar este control, puedes cambiar la visibilidad del puerto a **Public** si es necesario para que la app nativa acceda. La API exige el token para tareas, chat, memoria e investigación. El HTML inicial y los assets públicos pueden seguir siendo accesibles; no uses el servidor temporal para datos sensibles.
6. Pega la URL HTTPS en **URL HTTPS del backend** y el token en el campo de token de Waai Shimeji. No añadas /api al final ni compartas el token.
7. Pulsa **Guardar y probar conexión**. Esta comprobación llama a un endpoint protegido, así detecta si falta el token.

Si detienes y vuelves a crear tu Codespace, comprueba la URL del puerto y actualiza la URL guardada en Waai Shimeji. El servidor de Codespaces puede suspenderse cuando queda inactivo; durante ese tiempo, Waai no puede conversar ni investigar.

## Activar la superposición

1. Pulsa **Activar Shimeji flotante**.
2. Android abrirá la pantalla oficial de permiso para **mostrar sobre otras aplicaciones**. Busca Waai Shimeji y permite la superposición.
3. Si Android pregunta por notificaciones, concédelas para que la app pueda avisarte cuando encuentre algo interesante.
4. Vuelve a Waai Shimeji. La mascota aparecerá flotando sobre otras aplicaciones.
5. Toca la mascota para abrir el panel. Puedes moverla arrastrándola.
6. **Conversar** abre el chat nativo; **Preguntar por investigaciones** inicia una conversación sobre el trabajo de Waai.

Para cerrarla, pulsa **Cerrar Waai** en el panel flotante o **Detener y quitar la mascota** en la app principal. No se inicia automáticamente cuando reinicias el teléfono; debes activarla tú.

## Iniciativa y límites

- La iniciativa comienza **desactivada**. Marca *Permitir que Waai inicie interacciones* si aceptas avisos proactivos.
- Por defecto se silencian los avisos entre 22:00 y 08:00 según la hora local del teléfono.
- Mientras el servicio está activo, revisa el backend aproximadamente cada 15 minutos cuando el teléfono está interactivo y fuera del horario silencioso.
- Los mensajes espontáneos generados por el modelo tienen un enfriamiento de servidor predeterminado de cuatro horas. Las investigaciones nuevas pueden producir un aviso propio cuando hay fuentes disponibles.
- Si el servidor tiene modelo configurado, Waai puede preparar una observación contextual y una pregunta breve a partir de tareas o investigaciones guardadas. Sin modelo, solo puede emitir un aviso sencillo sobre un resultado real disponible; no simula que ha razonado.
- Para buscar imágenes, videos, música o páginas, Waai crea una acción con un enlace HTTPS permitido. El enlace solo se abre cuando pulsas **Abrir**; no reproduce música ni videos automáticamente.
- La mascota no lee la pantalla, no captura otras apps y no usa un servicio de accesibilidad. Puede abrir enlaces en el navegador o la aplicación compatible. Esta versión no incrusta todavía un reproductor de video/música dentro de la propia ventana flotante.
- Puedes descartar un mensaje proactivo para evitar volver a verlo.
- Si el servidor se apaga, la conversación e investigación remotas no estarán disponibles. La mascota nativa no necesita ejecutar un modelo lingüístico local.

## Permisos, privacidad y compatibilidad

Esta app utiliza el permiso Android SYSTEM_ALERT_WINDOW para mostrar su propia ventana y un servicio en primer plano con un aviso persistente mientras la mascota está activa. No inicia el servicio automáticamente en el arranque. La consulta de iniciativa es opt-in y solo se llama cuando la casilla está activada.

La URL HTTPS y el token se guardan en el almacenamiento privado de preferencias de la app. El token no se incluye en el APK, el código fuente ni los artefactos del repositorio. El almacenamiento local de preferencias de esta primera versión no está cifrado con una biblioteca adicional; protege el acceso a través del aislamiento normal de la app, pero no debe tratarse como una bóveda criptográfica.

Android exige autorización explícita para superposiciones. Android 14 y versiones posteriores también requieren declarar el tipo de servicio en primer plano y el permiso correspondiente. El manifiesto declara el uso especial como servicio de compañera interactiva. Para una publicación en Google Play habría que revisar las políticas y justificar el caso de uso del tipo specialUse; este prototipo está pensado para pruebas instaladas manualmente.

## Diagnóstico rápido

- **El botón no abre la mascota:** comprueba el permiso de superposición en los ajustes de Android y activa de nuevo el servicio.
- **La app indica que no conecta:** verifica que Codespaces esté ejecutándose, que el puerto 8000 siga abierto y que la URL guardada use HTTPS.
- **Error 401:** el servidor exige token; actualiza el valor que pegaste en Ajustes.
- **Waai no inicia conversaciones:** activa la casilla de iniciativa, verifica que el backend tenga una investigación con fuentes o tareas útiles y configura un modelo lingüístico compatible en el servidor. La iniciativa se limita de forma intencional para no generar interrupciones constantes.
- **La búsqueda se abre en un navegador/app externo:** es el comportamiento previsto en esta versión. Insertar miniaturas o un reproductor dentro del overlay es una mejora posterior; no hay reproducción automática.
- **El APK de Actions falla:** abre el registro del trabajo Build debug APK; un workflow fallido no produce un artefacto instalable.

Consulta también [AUTONOMY.md](AUTONOMY.md) y [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
