# Guía para nutriólogos — Asistente de IA (#407)

**Issue:** [#407](https://github.com/diego-torres/nutriconsultas/issues/407) · Epic [#404](https://github.com/diego-torres/nutriconsultas/issues/404)  
**Related:** [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) (#361) · [`NUTRITION-GOLDEN-PROMPTS.md`](NUTRITION-GOLDEN-PROMPTS.md) (#401)

Orientación en **español (es-MX)** para nutriólogos que usan el asistente en **`/admin/ai`**. La misma guía aparece de forma resumida en la pantalla de chat (panel «Cómo usar el asistente»).

---

## Borradores — siempre revisión profesional

| Regla | Detalle |
|-------|---------|
| **Todo es borrador** | Platillos, menús y planes que genera la IA se guardan como **borrador IA**, no en tu catálogo ni en pacientes. |
| **Etiqueta visible** | *«Borrador IA — revisión del nutriólogo requerida»* en borradores y respuestas. |
| **Tu criterio manda** | Verifica porciones, alergias, objetivos clínicos y datos del catálogo antes de aceptar. |
| **Sin asignación automática** | La IA **no** asigna dietas a pacientes; eso sigue en el flujo habitual de la plataforma. |

---

## Ejemplos de buenos prompts

Escribe en español, con el mayor contexto posible (kcal, ingestas, restricciones).

### Platillo / receta

- *Genera un desayuno alto en proteína usando alimentos de mi catálogo.*
- *Arma una receta con pechuga de pollo, arroz y verduras; calcula nutrientes por porción.*
- *Necesito un platillo bajo en sodio para la cena, 2 porciones.*

### Menú de un día

- *Menú de hoy: 1800 kcal, desayuno, comida, cena y dos colaciones, bajo en sodio.*
- *Menú de un día amigable para diabetes, 1800 kcal, bajo índice glucémico.*

### Plan de varios días

- *Plan semanal de 7 días, 1600 kcal, para pérdida de peso.*
- *Plan de 7 días sin huevo* (con paciente vinculado en el chat si aplica).

### Consultas puntuales

- *¿Cuántas kcal tiene 150 g de pechuga de pollo del catálogo?*
- *Busca avena en mi catálogo.*

Si falta el objetivo calórico o las comidas del día, el asistente puede **pedir aclaración** antes de generar un borrador grande.

---

## Limitaciones

| No hace la IA | Qué hacer en su lugar |
|---------------|------------------------|
| Diagnosticar o prescribir tratamiento | Usar tu juicio clínico; la IA solo apoya borradores nutricionales. |
| Asignar plan a un paciente | Revisa el borrador → **Aceptar** → asigna la dieta en la UI de pacientes. |
| Inventar nutrientes sin catálogo | Pide alimentos del catálogo; si no existen, agrégalos manualmente y vuelve a pedir. |
| Generar decenas de platillos o planes en un mensaje | Pide **un borrador a la vez**; luego pide variaciones en mensajes nuevos. |
| Tareas fuera de nutrición | Correos, marketing, etc. — fuera de alcance. |
| Ver datos de otros nutriólogos | Solo tu catálogo y tus conversaciones. |

Emergencias médicas: la IA debe redirigir a atención profesional presencial; **no** sustituye urgencias.

---

## Aceptar o descartar borradores

1. Envía tu mensaje en el chat; el asistente puede crear uno o más borradores en el panel **Borradores IA** (derecha).
2. Selecciona un borrador para ver el detalle (ingredientes, ingestas, nutrientes, advertencias).
3. **Aceptar** — confirmación con SweetAlert → se crea un **platillo** o **dieta** en **tu** catálogo. Puedes editarlo después como cualquier otro registro.
4. **Descartar** — el borrador queda descartado; no modifica el catálogo.

Si editas un mensaje anterior en la conversación, los borradores ligados a respuestas posteriores pueden invalidarse (comportamiento documentado en la UI de chat).

---

## Consejos rápidos

- Vincula **paciente** o **dieta** al iniciar la conversación cuando el contexto clínico importe (widget flotante en ficha de paciente, dieta o platillo).
- Prefiere **un objetivo por mensaje** cuando estés probando (un menú, un platillo, un plan corto).
- Revisa **advertencias** del borrador (sodio, alergias, datos faltantes en catálogo).
- Si ves *límite de mensajes*, espera unos minutos (límite por hora por nutriólogo).

---

## Documentación técnica

| Doc | Tema |
|-----|------|
| [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) | Alcance v1 y flujos soportados |
| [`LOCAL-AI-SETUP.md`](LOCAL-AI-SETUP.md) | Configuración en desarrollo |
| [`PRODUCTION-AI-SETUP.md`](PRODUCTION-AI-SETUP.md) | Producción y desactivación |
