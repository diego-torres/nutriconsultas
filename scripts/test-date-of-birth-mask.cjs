'use strict';

const assert = require('assert');
const path = require('path');

const mask = require(path.join(
  __dirname,
  '..',
  'src/main/resources/static/sbadmin/js/date-of-birth-mask.js'
));

assert.strictEqual(mask.format(''), '');
assert.strictEqual(mask.format('1'), '1');
assert.strictEqual(mask.format('15'), '15');
assert.strictEqual(mask.format('150'), '15/0');
assert.strictEqual(mask.format('1503'), '15/03');
assert.strictEqual(mask.format('15031990'), '15/03/1990');
assert.strictEqual(mask.format('15/03/1990'), '15/03/1990');
assert.strictEqual(mask.format('15a03b1990'), '15/03/1990');
assert.strictEqual(mask.format('150319901234'), '15/03/1990');
assert.strictEqual(mask.format(null), '');
assert.strictEqual(mask.format(undefined), '');

process.stdout.write('ok\n');
