# Procedencia de código y avisos de terceros

## Política de este proyecto

Waai Companion 0.4 se construyó para este repositorio con una mascota SVG original, CSS propio y JavaScript propio. No se incorporaron archivos de personajes, paquetes de sprites, binarios ni bloques de código de los proyectos de referencia listados abajo. No carga scripts, fuentes, CSS ni analítica desde CDNs.

Las referencias se consultaron para evaluar patrones de arquitectura, comportamiento y empaquetado. Una licencia abierta autoriza usos de su código conforme a sus condiciones; no convierte automáticamente en reutilizable la marca, los personajes, las ilustraciones, los modelos de IA o los recursos de terceros que el proyecto pudiera enlazar.

## Repositorios consultados

- **Desktop-Virtual-buddy — spyderweb47/Desktop-Virtual-buddy**
  - Referencia: mascota de escritorio, estados de comportamiento y separación entre personaje e integración de modelo.
  - Licencia reportada por la API de GitHub al revisar el repositorio: MIT.
  - No se copió código fuente ni se importaron sus sprites. El prototipo web de Waai implementa sus eventos y animaciones de forma independiente.
  - Repositorio: https://github.com/spyderweb47/Desktop-Virtual-buddy

- **Tauri — tauri-apps/tauri**
  - Referencia planificada para la futura etapa de aplicación de escritorio nativa y ventana flotante.
  - El proyecto Tauri ofrece componentes bajo MIT y/o Apache-2.0 según el componente y sus términos. No hay código de Tauri empaquetado en la PWA 0.4.
  - Repositorio: https://github.com/tauri-apps/tauri
  - Licencia MIT: https://github.com/tauri-apps/tauri/blob/dev/LICENSE-MIT

- **Ollama — ollama/ollama**
  - Referencia para un proveedor local de modelos; su API documenta compatibilidad con el formato de Chat Completions.
  - El backend de Waai envía peticiones mediante su propio adaptador HTTP y no contiene código de Ollama.
  - Repositorio (licencia MIT reportada por la API de GitHub): https://github.com/ollama/ollama
  - Documentación de compatibilidad OpenAI: https://github.com/ollama/ollama/blob/main/docs/api/openai-compatibility.mdx

- **Shimeji Desktop — ikum14/shimeji-desktop**
  - Se observó como referencia de funciones, no como fuente de implementación.
  - La API de GitHub no reportó un identificador SPDX de licencia en la fecha de revisión; por precaución no se reutilizó su código ni sus recursos.
  - Repositorio: https://github.com/ikum14/shimeji-desktop

## Dependencias de ejecución directas de Waai

| Dependencia | Uso |
|---|---|
| FastAPI | API HTTP |
| Uvicorn | Servidor ASGI |
| Pydantic | Validación de entrada |
| HTTPX | Cliente HTTP para el proveedor lingüístico opcional |

La interfaz no tiene dependencias JavaScript de terceros. Las dependencias transitivas se instalan según los rangos de requirements.txt; no están fijadas por hash en esta versión. La comprobación de CI no es una certificación de ausencia de vulnerabilidades.

## Alcance legal del repositorio

Al revisar el repositorio no existía un archivo LICENSE en la raíz, por lo que no se debe asumir una licencia global para redistribuir todo el producto. Antes de publicar binarios o permitir forks comerciales, el responsable del proyecto debe elegir y añadir una licencia propia, y validar las licencias de cada dependencia transitiva, recurso gráfico, voz o modelo incorporado.

## Revisión de seguridad

- No ejecutar código generado por el modelo ni por páginas web.
- No guardar claves del modelo dentro de HTML, JavaScript, capturas ni repositorio.
- Los extractos de investigación se tratan como datos no confiables.
- Las rutas /api/ no se incluyen en la caché offline.
- Las imágenes personales elegidas en Ajustes se quedan en el almacenamiento local del navegador.
