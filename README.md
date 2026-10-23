# Waai · Omnixa

**Foundation v0.2.0** · Aplicación web instalable (PWA) · Python/FastAPI · SQLite · Español

Waai empieza como secretaria digital con conversación básica, tareas, recordatorios, informes, memoria autorizada e investigaciones asignadas explícitamente. Este repositorio es la única base de desarrollo del proyecto.

## Funciones actuales

- Interfaz móvil instalable en navegadores compatibles.
- API Python con persistencia SQLite para tareas, conversación, recuerdos, encargos de investigación, conocimiento aprobado y notificaciones.
- Reglas conversacionales en español, con proveedor opcional compatible con OpenAI para conversación generativa y síntesis preliminar de investigaciones.
- Interfaz móvil con retrato de personaje configurable desde el teléfono; la imagen se queda en ese navegador.
- Modo opt-in de iniciativa: una investigación diaria de temas preautorizados, con borradores y fuentes revisables, sin guardado permanente automático.
- Memoria personal guardada solo tras una autorización expresa.
- Investigaciones iniciadas por encargo. Los extractos se conservan como fuentes revisables; el conocimiento permanente necesita aprobación humana.
- Recordatorios de tareas vencidas e informes diarios/semanales mientras el servidor está activo.
- Protección básica con token en modo producción; nunca se incluyen credenciales en el código.

## Límites importantes

- **No se incluye un modelo neuronal ni autoentrenamiento.** Para habilitar conversación generativa debes configurar un endpoint compatible; sin ello, Waai responde mediante reglas.
- La búsqueda web puede depender de la disponibilidad del proveedor y debe probarse después del despliegue.
- La comprobación de seguridad es limitada; las tres leyes son principios de diseño, no una garantía formal.
- Las notificaciones quedan guardadas en la base de datos. Las notificaciones push fiables con la app cerrada requieren una integración adicional.
- Es una base para uso individual, no un sistema multiusuario endurecido.

## Ejecutar en local

Requiere Python 3.11 o posterior (se recomienda 3.12).

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
APP_ENV=development WAAI_RESEARCH_ENABLED=false uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

En Windows PowerShell activa con `.venv\Scripts\Activate.ps1` y configura las variables con `$env:NOMBRE="valor"`. Abre http://127.0.0.1:8000.

## Modelo lingüístico e iniciativa

Consulta [AUTONOMY.md](AUTONOMY.md) para conectar un modelo local o remoto compatible y activar investigaciones diarias por temas autorizados. Ambos sistemas están apagados o sin configurar por defecto; la memoria permanente siempre requiere aprobación.

## Despliegue

- [Guía de despliegue](DEPLOYMENT.md)
- [Guía de GitHub](GITHUB_SETUP.md)
- [Hoja de ruta](ROADMAP.md)
- [Política de seguridad](SECURITY.md)

Antes del despliegue, configura `WAAI_ACCESS_TOKEN` con un valor aleatorio de al menos 32 caracteres y usa almacenamiento persistente para `/data`. No publiques archivos `.env`, bases de datos ni exportaciones privadas. Usa HTTPS y copias de seguridad protegidas.

## Siguientes hitos

1. Validar un despliegue persistente y las funciones de investigación.
2. Reforzar autenticación, recuperación y copias de seguridad.
3. Integrar un modelo lingüístico local intercambiable, sin API corporativa por defecto.
4. Mantener permisos, memoria y reglas de seguridad fuera del modelo.
5. Desarrollar una integración Android después de estabilizar la PWA y la API.
