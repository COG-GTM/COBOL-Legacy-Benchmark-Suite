/**
 * Seeded generator for the volume golden fixture.
 *
 * TSTGEN00.cbl drives its generation from a RANDSEED file; the equivalent here
 * is golden/config/golden-config.json "randomSeed".  The generator is a plain
 * 32 bit linear congruential generator so the fixture is reproducible from the
 * seed alone, with no dependence on the host RNG.
 */

const MODULUS = 2147483648n; // 2**31
const MULTIPLIER = 1103515245n;
const INCREMENT = 12345n;

export function createRandom(seed) {
  let state = BigInt(seed) % MODULUS;
  return () => {
    state = (MULTIPLIER * state + INCREMENT) % MODULUS;
    return Number(state);
  };
}

const CLIENT_TYPES = ['I', 'C', 'T'];
const NAME_PREFIX = ['ALPHA', 'BRAVO', 'DELTA', 'GAMMA', 'OMEGA', 'SIGMA'];
const NAME_SUFFIX = ['GROWTH FUND', 'INCOME TRUST', 'BALANCED FUND', 'RESERVE ACCOUNT'];

function money(next, wholeDigits) {
  const whole = next() % 10 ** wholeDigits;
  const cents = next() % 100;
  return `${whole}.${String(cents).padStart(2, '0')}`;
}

/**
 * @param {{randomSeed:string, volumeRecordCount:number, captureUser:string}} config
 * @returns {{seed:Array<{alias:string, values:object}>, input:Array<{alias:string, values:object}>}}
 */
export function volumeAdd(config) {
  const next = createRandom(config.randomSeed);
  const input = [];
  const seed = [];

  for (let index = 0; index < config.volumeRecordCount; index += 1) {
    const sequence = String(index + 1).padStart(4, '0');
    const values = {
      portId: `PORT${sequence}`,
      accountNo: String(9000000000 + index),
      clientName: `${NAME_PREFIX[next() % NAME_PREFIX.length]} ${
        NAME_SUFFIX[next() % NAME_SUFFIX.length]
      }`,
      clientType: CLIENT_TYPES[next() % CLIENT_TYPES.length],
      createDate: '00000000',
      lastMaint: '00000000',
      status: 'A',
      totalValue: money(next, 7),
      cashBalance: money(next, 5),
      lastUser: config.captureUser,
      lastTrans: '00000000',
      filler: '',
    };
    input.push({ alias: `GEN${sequence}`, values });

    // Every fifth key is pre-loaded so the volume run exercises the duplicate
    // key path (VSAM status 22) as well as the happy path.
    if (index % 5 === 0) {
      seed.push({
        alias: `GEN${sequence}_SEEDED`,
        values: {
          ...values,
          // COBOL MOVE into PIC X(30) truncates on the right; mirror that here.
          clientName: `${values.clientName} PRELOADED`.slice(0, 30),
          createDate: '20240101',
          lastMaint: '20240101',
          lastTrans: '20240101',
        },
      });
    }
  }

  return { seed, input };
}

export const GENERATORS = { volumeAdd };
