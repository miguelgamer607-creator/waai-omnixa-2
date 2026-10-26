# Pruebas desde celular (Android)

Esta es la guía principal para probar Waai Companion desde un teléfono. No presupone que tengas un ordenador físico disponible.

## 1. Abrir el Codespace

1. Abre el repositorio en GitHub y entra en **Code → Codespaces**.
2. Abre el Codespace que ya creaste. En el navegador móvil, gira el teléfono a horizontal solo si necesitas más espacio para usar el editor; para probar Waai, vuelve a vertical.
3. La pestaña Terminal suele quedar muy estrecha. En el panel de terminal/editor, abre el menú de tres puntos si no encuentras Terminal o Puertos.
4. Mantén el Codespace y el servidor bajo tu control. Codespaces puede detenerse por inactividad y sus cuotas o cargos dependen de tu cuenta.

## 2. Actualizar y arrancar el servidor

En la terminal integrada del Codespace, ejecuta estas líneas una por una:

```bash
git pull --ff-only origin main
python -m pip install -r requirements.txt
```

Si el servidor ya está ejecutándose, pulsa dentro de esa terminal y usa **Ctrl+C**. Si el teclado móvil no permite enviar esa combinación con facilidad, puedes dejar el servidor actual funcionando y abrir su URL desde Puertos. Solo reinícialo si necesitas aplicar cambios del backend; en ese caso, usa los controles de la terminal de Codespaces o identifica el proceso antes de terminarlo, para evitar cerrar la sesión equivocada.

Después inicia el servidor si está detenido:

```bash
APP_ENV=development WAAI_RESEARCH_ENABLED=false python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Deja esta terminal abierta mientras pruebas Waai. No compartas el Codespace ni habilites la búsqueda automática durante la primera prueba.

## 3. Abrir el puerto en el teléfono

1. En Codespaces, busca la pestaña **Ports / Puertos** del panel inferior. Si está oculta, abre el menú `...` del panel.
2. Busca el puerto `8000`.
3. Usa **Open in Browser / Abrir en el navegador**. Mantén el puerto en modo **Private**.
4. La dirección debe terminar en algo parecido a `-8000.app.github.dev`.
5. Si no aparece el puerto, verifica que la terminal no muestre errores y que el servidor indique que está escuchando en `0.0.0.0:8000`.

No uses el panel Chat de Codespaces para hablar con Waai: ese panel es una herramienta del editor, no el chat de la aplicación.

## 4. Evitar que el navegador muestre archivos viejos

La PWA cambia sus archivos estáticos entre versiones. Tras actualizar el código:

- Recarga la página del navegador.
- Si ves la versión anterior, abre el menú de Brave y usa **Recargar**; como último recurso, entra a la configuración del sitio y borra los datos/caché de ese sitio, luego vuelve a abrir el puerto.
- No borres los datos del servidor o la base SQLite como método para refrescar la interfaz: eso podría borrar tus tareas y recuerdos.

## 5. Lista de pruebas táctiles

Marca cada prueba conforme la completes:

- [ ] La página inicial muestra la ilustración de Waai y el indicador de conexión.
- [ ] La navegación superior se puede desplazar horizontalmente; la navegación inferior no tapa el contenido.
- [ ] La mascota flotante se puede tocar, abre su burbuja y se puede arrastrar sin cambiar de pantalla.
- [ ] Entrar en **Conversar** oculta la mascota flotante para no tapar el campo de texto.
- [ ] Enviar “Hola, Waai” muestra el mensaje y una respuesta del servidor.
- [ ] Crear una tarea, actualizar la pantalla y comprobar que sigue guardada.
- [ ] Completar una tarea y comprobar que desaparece de las pendientes.
- [ ] Guardar un recuerdo con la casilla de autorización marcada y comprobar que aparece en Memoria.
- [ ] Asignar un tema de investigación con `WAAI_RESEARCH_ENABLED=false`: Waai debe decir honestamente que el encargo quedó registrado, pero no que está buscando activamente.
- [ ] Abrir Ajustes y cargar una imagen desde la galería; debe mostrarse en la interfaz sin subirla al backend.
- [ ] Volver a cargar la aplicación y comprobar que la imagen personal sigue en ese navegador.

## 6. Qué hacer si algo falla

Envíame una captura que incluya el mensaje de error y dime qué tocaste justo antes. Si el fallo ocurre en Terminal, copia las últimas 10–20 líneas del registro. No publiques claves API, tokens ni contraseñas.

Los checks de GitHub Actions comprueban pruebas automáticas, sintaxis y dependencias; no simulan un dedo, el teclado Android ni todas las particularidades de Brave. La lista anterior sirve para validar la experiencia real del teléfono.
