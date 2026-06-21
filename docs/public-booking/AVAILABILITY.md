# Nutritionist availability (#246)

Working hours and slot duration configured at **`/admin/perfil`** feed public booking slot generation (~~#248~~).

## Timezone

All availability windows are interpreted in **`America/Mexico_City`** (CST/CDT). The REST API persists this value on `nutritionist_availability_settings.timezone`; the profile UI displays it read-only until multi-timezone support is added.

Public slot APIs convert `LocalDate` + slot start `LocalTime` using the nutritionist's stored zone, not the JVM default or the visitor's browser zone.

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

Authenticated preview: `GET /rest/profile/availability/slots?date=YYYY-MM-DD` (same slot list as public booking per nutritionist).

## Public booking (~~#248~~ — shipped)

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `GET /consultas/{publicBookingId}/agendar-cita` | Public | Thymeleaf slot picker + booking form |
| `GET /rest/public/booking/{publicBookingId}/context` | Public | Display name, timezone, advance days |
| `GET /rest/public/booking/{publicBookingId}/slots?date=` | Public | Available slots (respects 2-day advance) |
| `POST /rest/public/booking/{publicBookingId}/book` | Public | Create patient (if needed) + `CalendarEvent`; reCAPTCHA + rate limit |

Opaque **`public_booking_id`** (UUID) on `nutritionist_profile` — never expose OAuth `userId` in public URLs.

## Minimum booking advance (~~#248~~)

Public self-booking requires **at least 2 calendar days of anticipation** (nutritionist timezone). The earliest bookable date is **today + 2 days** — not today or tomorrow.

| Layer | Rule |
|-------|------|
| **Public slot API** | Return no slots (or reject the date) when `date` is before `today + 2 days` in the nutritionist's timezone |
| **Public booking POST** | Reject submissions where `slotStart < today + 2 days` (server-side; do not rely on UI alone) |
| **Public UI** | Date picker disables/blocks the next 2 calendar days; Spanish copy e.g. *Las citas requieren al menos 2 días de anticipación* |

Implementation: `MIN_BOOKING_ADVANCE_DAYS = 2` in `BookingAvailabilityConstants`; `PublicBookingAdvanceRules` applies on public paths only — admin preview (`GET /rest/profile/availability/slots`) stays unrestricted.

Tests: slot API empty for D+0/D+1; booking rejected inside window; first eligible day is D+2; timezone boundary at midnight `America/Mexico_City`.

## Absence blocks (#247)

| Table | Purpose |
|-------|---------|
| `nutritionist_availability_block` | Full-day or time-range unavailability per OAuth `user_id` |

REST (authenticated, calendar UI):

- `GET /rest/calendario/blocks?start=&end=` — FullCalendar feed (merged client-side with appointments)
- `POST /rest/calendario/blocks` — create block
- `DELETE /rest/calendario/blocks/{id}` — remove block (SweetAlert confirm in UI)

Admin UI: `/admin/calendario` → **Marcar ausencia**; blocked intervals render in gray with striped styling.

## Minimum booking advance (#248)

Public self-booking requires **at least 2 calendar days of anticipation** (nutritionist timezone). The earliest bookable date is **today + 2 days** — not today or tomorrow.

| Layer | Rule |
|-------|------|
| **Public slot API** | Return no slots (or reject the date) when `date` is before `today + 2 days` in the nutritionist's timezone |
| **Public booking POST** | Reject submissions where `slotStart < today + 2 days` (server-side; do not rely on UI alone) |
| **Public UI** | Date picker disables/blocks the next 2 calendar days; Spanish copy e.g. *Las citas requieren al menos 2 días de anticipación* |

Implementation note: add `MIN_BOOKING_ADVANCE_DAYS = 2` to `BookingAvailabilityConstants` (or equivalent) and apply in the public slot path and booking validator — **not** in the nutritionist admin calendar preview (`GET /rest/profile/availability/slots` remains unrestricted for staff).

Tests: slot API empty for D+0/D+1; booking rejected inside window; first eligible day is D+2; timezone boundary at midnight `America/Mexico_City`.
