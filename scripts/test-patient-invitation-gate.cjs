#!/usr/bin/env node
'use strict';

/**
 * Interop tests for docs/auth0/actions/patient-invitation-gate.js (#140).
 * Verifies Node HS256 logic matches PatientInvitationJws (Java #133).
 *
 * Usage:
 *   node scripts/test-patient-invitation-gate.cjs verify <secret> <compact-jws>
 * Prints patientId or "null", exits 0.
 */
const path = require('path');

const gate = require(path.join(__dirname, '../docs/auth0/actions/patient-invitation-gate.js'));

const [, , command, secret, jws] = process.argv;

if (command === 'verify') {
	const result = gate.verifyOfflineJws(secret, jws);
	process.stdout.write(result === null ? 'null' : String(result));
	process.exit(0);
}

if (command === 'selftest') {
	const assert = (cond, msg) => {
		if (!cond) {
			console.error(msg);
			process.exit(1);
		}
	};
	assert(gate.isSocialConnection({ connection: { strategy: 'apple' } }) === true, 'apple strategy');
	assert(gate.isSocialConnection({ connection: { strategy: 'google-oauth2' } }) === true, 'google strategy');
	assert(gate.isSocialConnection({ connection: { strategy: 'auth0' } }) === false, 'database strategy');
	assert(
		gate.isSocialConnection({ user: { identities: [{ provider: 'apple' }] } }) === true,
		'apple identity',
	);
	assert(gate.isSocialConnection({}) === false, 'empty event');
	process.stdout.write('ok');
	process.exit(0);
}

console.error('Usage: node scripts/test-patient-invitation-gate.cjs verify <secret> <jws>');
process.exit(1);
