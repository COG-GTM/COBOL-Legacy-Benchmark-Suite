import { handler as PORTADD } from './portadd.js';
import { handler as PORTREAD } from './portread.js';
import { handler as PORTUPDT } from './portupdt.js';
import { handler as PORTDEL } from './portdel.js';
import { handler as PORTTRAN } from './porttran.js';
import { handler as PORTVALD } from './portvald.js';

export const HANDLERS = { PORTADD, PORTREAD, PORTUPDT, PORTDEL, PORTTRAN, PORTVALD };
