# Hoja de ruta de ingeniería de Waai Companion

## 0.4.0 — Companion PWA
- [x] Interfaz móvil de un solo usuario.
- [x] Mascota SVG original y estados de animación.
- [x] Burbuja flotante y arrastrable dentro de la aplicación.
- [x] Retrato personal configurable y almacenado en el navegador local.
- [x] Conversación, tareas, recuerdos, investigaciones y avisos conectados al backend.
- [x] Proveedor lingüístico opcional, con acciones estructuradas controladas por FastAPI.
- [x] Investigación diaria opcional y limitada a temas configurados por el usuario.
- [x] Shell offline limitado; las respuestas de API nunca entran en caché.
- [x] Seguridad básica del navegador y pruebas de frontend/backend.
- [ ] Validación visual y funcional en el teléfono objetivo.

## Etapa 2 — Conversación y contexto
- Mejorar el prompt base, mantener contexto sin exceder límites de tokens y mostrar qué modelo está activo.
- Probar proveedores OpenAI-compatibles de forma automatizada con un mock HTTP.
- Añadir resumen de conversaciones antiguas y recuperación de conocimiento aprobado.
- Añadir una consola de ejecución con estados: pensando, investigando, necesita revisión y terminado.

## Etapa 3 — Iniciativa acotada
- Añadir un panel de propuestas de Waai con una explicación verificable de por qué propone cada acción.
- Añadir límites de tiempo, cantidad de fuentes, lista de temas y quiet hours configurables.
- Registrar el resultado, la fecha y las fuentes; no guardar memoria permanente sin aprobación.
- Añadir reintentos seguros cuando el servidor se haya detenido durante una tarea programada.

## Etapa 4 — Aplicación de escritorio Shimeji
- Empaquetar la misma interfaz como aplicación desktop nativa mediante Tauri 2.
- Crear una ventana mascota transparente, siempre visible y movible, con apertura del panel de conversación.
- Mantener la API y la memoria en el backend actual; no copiar motores ni sprites de procedencia dudosa.
- Probar builds en Windows/Linux antes de anunciar instaladores.

## Etapa 5 — Android nativo y voz
- Validar primero la PWA en Android.
- Crear una app Android con una superposición opcional, permiso explicable y control para apagarla.
- Añadir voz solo tras acordar cómo se procesarán audio y datos y si habrá proveedor local/remoto.

## Límites permanentes de diseño
- El modelo propone; el backend valida y ejecuta las acciones autorizadas.
- Sin ejecución de comandos arbitrarios sugeridos por un modelo o una página web.
- Sin auto-modificación, auto-despliegue ni auto-asignación de permisos.
- Mostrar hechos, inferencias y dudas por separado en informes de investigación.
