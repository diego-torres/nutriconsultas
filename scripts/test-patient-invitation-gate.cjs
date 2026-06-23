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

console.error('Usage: node scripts/test-patient-invitation-gate.cjs verify <secret> <jws>');
process.exit(1);
