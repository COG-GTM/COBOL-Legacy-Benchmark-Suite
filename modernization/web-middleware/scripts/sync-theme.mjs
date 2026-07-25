#!/usr/bin/env node
/**
 * Attempts to derive the OCBC palette from the brand source at
 * https://api.ocbc.com/store by scanning its stylesheets for the dominant
 * hex colours. Prints the findings so src/theme/ocbc.ts can be updated.
 * If the source is unreachable the documented fallback (brand red #d8232a)
 * stays in place.
 */
const SOURCE = 'https://api.ocbc.com/store';

const fetchText = async (url) => {
  const res = await fetch(url, { signal: AbortSignal.timeout(20000) });
  if (!res.ok) throw new Error(`${url} -> HTTP ${res.status}`);
  return res.text();
};

try {
  const html = await fetchText(SOURCE);
  const hrefs = [...html.matchAll(/href="([^"]+\.css)"/g)].map((m) => m[1]);
  const counts = new Map();
  for (const href of hrefs) {
    const url = new URL(href.replace(/^\//, ''), 'https://api.ocbc.com/').toString();
    const css = await fetchText(url);
    for (const [hex] of css.matchAll(/#[0-9a-fA-F]{6}/g)) {
      const key = hex.toLowerCase();
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
  }
  const top = [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 15);
  console.log(`Derived from ${SOURCE}:`);
  top.forEach(([hex, n]) => console.log(`  ${hex}  x${n}`));
} catch (err) {
  console.warn(`Could not reach ${SOURCE} (${err.message}).`);
  console.warn('Keeping documented OCBC fallback palette (primary #d8232a).');
}
