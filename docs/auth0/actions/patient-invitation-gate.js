/**
 * Auth0 Post-Login Action — patient invitation gate (#140).
 *
 * Deploy: Auth0 Dashboard → Actions → Library → Build Custom → Post Login.
 * Paste this file, add secrets (see docs/auth0/PATIENT-POST-LOGIN-GATE.md), attach to Login flow.
 *
 * Runtime: Node 18 (Auth0 Actions). Uses crypto + fetch (preview fallback).
 */
'use strict';

const crypto = require('crypto');

const COMPACT_JWS_PATTERN = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/;

const DENY_CODE = 'invitation_required';

const DENY_MESSAGE = 'Se requiere una invitación válida para registrarse.';

/**
 * Verifies HS256 offline JWS issued by PatientInvitationJws (Java #133).
 * Returns patientId on success, null otherwise. Does not check single-use (DB redeem #136).
 */
function verifyOfflineJws(secret, compactJws) {
	if (!secret || !compactJws) {
		return null;
	}
	const parts = compactJws.split('.');
	if (parts.length !== 3) {
		return null;
	}
	let header;
	try {
		header = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
	}
	catch (error) {
		return null;
	}
	if (header.alg !== 'HS256') {
		return null;
	}
	const signingInput = parts[0] + '.' + parts[1];
	const expected = crypto.createHmac('sha256', secret).update(signingInput).digest();
	let actual;
	try {
		actual = Buffer.from(parts[2], 'base64url');
	}
	catch (error) {
		return null;
	}
	if (expected.length !== actual.length || !crypto.timingSafeEqual(expected, actual)) {
		return null;
	}
	let payload;
	try {
		payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));
	}
	catch (error) {
		return null;
	}
	const now = Math.floor(Date.now() / 1000);
	if (!payload.exp || payload.exp <= now) {
		return null;
	}
	if (!payload.patientId || payload.patientId < 1) {
		return null;
	}
	return payload.patientId;
}

function isCompactJws(value) {
	return typeof value === 'string' && COMPACT_JWS_PATTERN.test(value);
}

/**
 * Validates raw URL token against public preview endpoint (#135). Authoritative for
 * revocation/expiry at login time; rate-limited server-side.
 */
async function validateViaPreview(apiBaseUrl, rawUrlToken) {
	if (!apiBaseUrl || !rawUrlToken) {
		return false;
	}
	const base = apiBaseUrl.replace(/\/$/, '');
	const url = base + '/rest/mobile/invitations/' + encodeURIComponent(rawUrlToken) + '/preview';
	const response = await fetch(url, {
		method: 'GET',
		headers: { Accept: 'application/json' },
	});
	return response.ok;
}

async function validateViaHumanCodePreview(apiBaseUrl, humanCode) {
	if (!apiBaseUrl || !humanCode) {
		return false;
	}
	const base = apiBaseUrl.replace(/\/$/, '');
	const url = base + '/rest/mobile/invitations/by-code/' + encodeURIComponent(humanCode.trim()) + '/preview';
	const response = await fetch(url, {
		method: 'GET',
		headers: { Accept: 'application/json' },
	});
	return response.ok;
}

function isHumanCode(value) {
	if (!value || typeof value !== 'string') {
		return false;
	}
	const trimmed = value.trim().toUpperCase();
	return /^[A-Z0-9]+-[0-9A-Z]{4}-[0-9A-Z]{4}$/.test(trimmed);
}

function readInvitationToken(event) {
	const queryToken = event.request?.query?.invitation_token;
	if (queryToken && typeof queryToken === 'string' && queryToken.trim() !== '') {
		return queryToken.trim();
	}
	const bodyToken = event.request?.body?.invitation_token;
	if (bodyToken && typeof bodyToken === 'string' && bodyToken.trim() !== '') {
		return bodyToken.trim();
	}
	return null;
}

async function validateInvitationToken(event) {
	const token = readInvitationToken(event);
	if (!token) {
		return false;
	}
	const trimmed = token;
	const jwsSecret = event.secrets?.PATIENT_INVITATION_JWS_SECRET;
	if (isCompactJws(trimmed) && jwsSecret) {
		return verifyOfflineJws(jwsSecret, trimmed) !== null;
	}
	const apiBaseUrl = event.secrets?.API_BASE_URL;
	if (apiBaseUrl && isHumanCode(trimmed)) {
		return validateViaHumanCodePreview(apiBaseUrl, trimmed);
	}
	if (apiBaseUrl) {
		return validateViaPreview(apiBaseUrl, trimmed);
	}
	return false;
}

/**
 * @param {Event} event
 * @param {PostLoginAPI} api
 */
exports.onExecutePostLogin = async (event, api) => {
	if (event.user.app_metadata?.invited === true) {
		return;
	}
	const loginsCount = event.stats?.logins_count ?? 0;
	if (loginsCount !== 1) {
		return;
	}
	const valid = await validateInvitationToken(event);
	if (!valid) {
		api.access.deny(DENY_CODE, DENY_MESSAGE);
		return;
	}
	api.user.setAppMetadata('invited', true);
};

// Exported for Node interop tests (scripts/test-patient-invitation-gate.mjs).
if (typeof module !== 'undefined') {
	module.exports.verifyOfflineJws = verifyOfflineJws;
	module.exports.isCompactJws = isCompactJws;
}
