# Nutritionist availability (#246)

Working hours and slot duration configured at **`/admin/perfil`** feed future public booking slot generation (#248).

## Timezone

All availability windows are interpreted in **`America/Mexico_City`** (CST/CDT). The REST API persists this value on `nutritionist_availability_settings.timezone`; the profile UI displays it read-only until multi-timezone support is added.

Public slot APIs (#248) must convert `LocalDate` + slot start `LocalTime` using the nutritionist's stored zone, not the JVM default or the visitor's browser zone.

## Data model

| Table | Purpose |
|-------|---------|
| `nutritionist_availability_settings` | One row per OAuth `user_id`: `slot_duration_minutes` (15–120), `timezone` |
| `nutritionist_working_hours_interval` | Zero or more rows per user: ISO weekday (1=Monday … 7=Sunday), `start_time`, `end_time` |

## REST API (authenticated)

- `GET /rest/profile/availability` — load schedule for `principal.sub`
- `PUT /rest/profile/availability` — replace intervals + settings; validates overlaps and slot duration

## Slot generation

`BookingSlotGenerator.generateSlotStarts(intervals, slotDurationMinutes)` produces sorted slot start times that fit entirely within configured intervals.

`BookingAvailabilitySlotService.getAvailableSlotStarts(userId, date)` applies working hours, then subtracts:

- `nutritionist_availability_block` rows for that nutritionist (#247)
- `CalendarEvent` rows with status `SCHEDULED` for that nutritionist's patients

Authenticated preview: `GET /rest/profile/availability/slots?date=YYYY-MM-DD` (same slot list public booking #248 will expose per nutritionist).

## Absence blocks (#247)

| Table | Purpose |
|-------|---------|
| `nutritionist_availability_block` | Full-day or time-range unavailability per OAuth `user_id` |

REST (authenticated, calendar UI):

- `GET /rest/calendario/blocks?start=&end=` — FullCalendar feed (merged client-side with appointments)
- `POST /rest/calendario/blocks` — create block
- `DELETE /rest/calendario/blocks/{id}` — remove block (SweetAlert confirm in UI)

Admin UI: `/admin/calendario` → **Marcar ausencia**; blocked intervals render in gray with striped styling.
