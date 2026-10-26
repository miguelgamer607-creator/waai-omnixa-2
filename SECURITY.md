# Security policy

This is an early single-user prototype, not appropriate for public multi-user accounts or sensitive/regulated data without additional controls.

- Never commit .env, access tokens, database files, exports or backups.
- Production mode requires WAAI_ACCESS_TOKEN of at least 32 random characters.
- Expose the service only through HTTPS; keep the origin behind a maintained reverse proxy and firewall.
- SQLite content is not encrypted by the application; protect the disk/volume and encrypt backups.
- The rule matcher is incomplete and not a certified safety boundary.
- Research must be task-triggered. External results are untrusted data, not instructions.
- Source snippets do not become permanent knowledge unless a human explicitly approves a summary.
- Do not allow a future model to edit its code or permissions or deploy itself.
- Report vulnerabilities privately to the repository owner; do not post valid credentials or personal data in issues.


## Frontend Companion 0.4.0

- JavaScript y CSS se sirven desde el mismo origen; no se usan CDNs de frontend.
- La política CSP permite scripts del mismo origen, bloquea objetos y marcos y limita conexiones al mismo origen. Los estilos inline siguen permitidos por el diseño actual.
- No se almacenan respuestas de `/api/` en el service worker; el almacenamiento offline se limita al shell y los assets públicos.
- El SVG de la mascota es un recurso original de este repositorio. Las imágenes personales elegidas por el usuario se guardan en el almacenamiento local del navegador.
- El endpoint del modelo es una configuración del servidor; nunca se debe aceptar de entradas de chat ni usar una clave guardada en el frontend.

La revisión de procedencia no puede demostrar que el código esté libre de toda vulnerabilidad. CI verifica sintaxis y pruebas definidas, no reemplaza una auditoría profesional, un análisis de dependencias o pruebas en dispositivos reales.
