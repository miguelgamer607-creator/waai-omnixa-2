# Waai: modelo lingüístico e iniciativa

Waai puede usar un modelo de lenguaje compatible con la API de Chat Completions de OpenAI, pero no depende de un proveedor particular. El modelo genera texto; la API de Waai sigue siendo la única encargada de crear tareas, guardar recuerdos, gestionar permisos y registrar investigaciones.

## Conectar un modelo

Configura estas variables en el entorno donde se ejecuta el servidor:

    WAAI_LLM_BASE_URL=http://127.0.0.1:1234/v1
    WAAI_LLM_MODEL=nombre-del-modelo
    WAAI_LLM_API_KEY=

La clave es opcional para servidores locales; para proveedores remotos utiliza una clave válida como variable secreta del servicio, nunca dentro del repositorio ni del navegador. La base URL debe ser la raíz compatible con OpenAI y terminar normalmente en `/v1`; Waai añadirá `/chat/completions`.

El endpoint tiene que ser accesible **desde el servidor de Waai**. Si Waai está en Codespaces o en un hosting remoto, `127.0.0.1` apunta a esa máquina remota, no a tu teléfono. Para usar un modelo local, el servidor donde corre Waai debe poder alcanzar la API local, por ejemplo un servicio de inferencia compatible con OpenAI. Evita exponer una API local a internet sin autenticación.

Variables opcionales:

    WAAI_LLM_TIMEOUT_SECONDS=45
    WAAI_LLM_TEMPERATURE=0.7
    WAAI_LLM_MAX_TOKENS=700

Al activar un modelo externo, el servidor envía el historial reciente de la conversación y el conocimiento que el usuario aprobó. La app no guarda claves API. Revisa las condiciones de privacidad del proveedor y evita incluir datos que no quieras transmitir. Si el proveedor falla, Waai vuelve a las respuestas básicas e informa del fallo.

## Investigación diaria autorizada

El modo de investigación diaria está desactivado por defecto. Para encenderlo, configura:

    WAAI_AUTONOMY_MODE=research
    WAAI_RESEARCH_INTERESTS=IA y modelos abiertos ligeros; oportunidades de automatización para pequeñas empresas en Colombia; herramientas gratuitas para productividad
    WAAI_AUTONOMY_RESEARCH_HOUR=6
    WAAI_RESEARCH_ENABLED=true
    WAAI_TIMEZONE=America/Bogota

Los temas van separados por punto y coma. Como máximo se procesa **un tema al día**, alternando la lista por fecha local. La búsqueda se ejecuta mientras el proceso de Waai está funcionando. Los resultados iniciales son extractos, no una revisión exhaustiva de los sitios. Waai deja un aviso y fuentes en Investigar; no guarda el resumen en la memoria permanente. Revisa las fuentes y utiliza la acción de aprobación para conservar conocimiento.

Si no hay intereses configurados, el modo no hace búsquedas. Puedes apagarlo con `WAAI_AUTONOMY_MODE=off`. Si el servidor se detiene o duerme, no puede investigar ni enviar avisos mientras está inactivo; las notificaciones push al teléfono aún requieren una integración adicional.

## Qué significa “iniciativa”

El sistema puede proponer una prioridad, ejecutar investigaciones limitadas según la lista que autorizaste y preparar un resumen a partir de resultados encontrados. No tiene consciencia, voluntad independiente ni aprendizaje automático de sus experiencias. Lo que conserva a largo plazo son datos estructurados y resúmenes que el usuario decide aprobar. Una futura capa de planificación puede ampliar esas capacidades, siempre con límites explícitos y registro de acciones.


## Interfaz Waai Companion

La PWA nueva utiliza la mascota SVG local y recursos CSS/JavaScript servidos desde la propia aplicación. Desde **Ajustes** puedes usar un retrato propio. Este archivo solo se guarda en el almacenamiento del navegador, por lo que no se sincroniza con otros navegadores o dispositivos. La burbuja del personaje se puede mover dentro de la interfaz; todavía no es una ventana flotante del sistema operativo.

Consulta `THIRD_PARTY_NOTICES.md` para ver qué referencias de código abierto se revisaron y qué no se copió. El proyecto aún no define una licencia raíz.


## Shimeji Android (v0.5.0)

La app nativa usa el endpoint `POST /api/companion/turn` para conversar y devolver una propuesta de acción de una lista permitida. El endpoint no abre enlaces ni reproduce contenido; Android solo ejecuta esas acciones tras un toque en el botón correspondiente.

El endpoint `POST /api/companion/proactive` solo entrega eventos cuando el cliente manda `enabled: true`. La app Android deja esa casilla apagada por defecto, aplica horas de silencio y consulta cada 15 minutos mientras la sesión está activa. El servidor aplica un cooldown para que Waai no genere mensajes recurrentes sin límite.

Para activar la investigación diaria en un servidor, configura:
```env
WAAI_RESEARCH_ENABLED=true
WAAI_AUTONOMY_MODE=research
WAAI_RESEARCH_INTERESTS=IA y modelos pequeños; herramientas gratuitas de productividad; oportunidades de automatización para pequeños negocios en Colombia
WAAI_AUTONOMY_RESEARCH_HOUR=6
WAAI_TIMEZONE=America/Bogota
```

Para que los comentarios de Waai sean redactados por un modelo generativo, configura además `WAAI_LLM_BASE_URL`, `WAAI_LLM_MODEL` y, si el proveedor requiere autenticación, `WAAI_LLM_API_KEY`. Sin esas variables, el sistema puede mostrar hechos y fuentes ya registrados, pero no debe fingir que ejecutó razonamiento generativo. Protege estas variables en el backend; no incluyas claves en Android, HTML o GitHub.
