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

`BookingSlotGenerator.generateSlotStarts(intervals, slotDurationMinutes)` produces sorted slot start times that fit entirely within configured intervals. Used by unit tests now; public booking (#248) will subtract calendar events and absence blocks (#247).
