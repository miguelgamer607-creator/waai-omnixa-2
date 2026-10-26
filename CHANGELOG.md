# Registro de cambios

## 0.4.0 — 2026-10-25
- Creada la interfaz Waai Companion, centrada en la mascota animada y la conversación.
- Añadida una mascota SVG original con animación autónoma, estados de actividad y personalización de retrato local.
- Añadida una burbuja flotante arrastrable dentro de la aplicación para abrir el chat o pedir una sugerencia.
- Separados la hoja de estilos y el código de interfaz en archivos locales auditables, sin CDN ni dependencias JavaScript de terceros.
- Quitados los manejadores JavaScript inline; añadida CSP, cabeceras de seguridad y caché del shell PWA que excluye rutas de API.
- Añadidas comprobaciones para assets de la app, configuración defensiva y sintaxis JavaScript en CI.
- Conservados chat, tareas, memoria autorizada, investigación y notificaciones del backend.
- Documentadas las fuentes abiertas consultadas sin copiar personajes ni packs de ilustraciones de terceros.

## 0.2.0 — 2026-10-23
- Añadidos recordatorios en servidor y resúmenes diarios/semanales.
- Añadida investigación por encargo con extractos de fuentes y aprobación humana antes de guardar conocimiento permanente.
- Añadidos avisos persistentes, estado de lectura y verificación de token privado para el servidor.
- Aclarado que el comportamiento basado en reglas no equivale a un modelo neuronal entrenado.

## 0.1.0
- PWA inicial, API FastAPI, persistencia SQLite, tareas, conversación, memoria explícita e investigaciones.
