# Cómo contribuir

Este proyecto sigue un proceso formal de ingeniería de software (ver `CLAUDE.md` y `docs/`).
Antes de escribir código, ubícate en qué paso del roadmap estamos — está en el checklist de `CLAUDE.md`.

## Flujo de trabajo
1. Antes de empezar, lee el documento del paso correspondiente en `docs/`.
2. Crea una rama descriptiva: `paso-N-descripcion-corta` (ej. `paso-3-historias-usuario`) o `fix/descripcion` para correcciones.
3. Haz commits pequeños y descriptivos, **en inglés**, siguiendo Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
4. Abre un Pull Request usando la plantilla — indica a qué paso del roadmap corresponde.
5. Al menos una persona revisa antes de mergear a `main`.

## Convenciones
- **Idioma:** explicaciones y documentación en español; código, nombres de variables/funciones/clases y commits en inglés.
- **Documentación:** cada entregable del roadmap va en su carpeta numerada dentro de `docs/` 
- **Decisiones de arquitectura:** se registran como ADR en `docs/4-arquitectura/adrs/`.
- **No se salta ningún paso del roadmap** — si alguien cree que uno no aplica, se discute con el equipo antes de omitirlo.

## Reportar bugs o proponer funcionalidades
Usa las plantillas de Issues (Bug report / Feature request).

## Dudas de negocio
Si algo no está claro y no aparece en `docs/1-definicion-y-requisitos/`, pregunta al Product Owner (el administrador de la finca) antes de asumir.
