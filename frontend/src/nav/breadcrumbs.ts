import { labelForPath } from './navigation';

export interface Crumb {
  label: string;
  path: string;
}

/** Title-cases a raw route segment as a breadcrumb fallback label. */
function humanize(segment: string): string {
  return segment
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/** Builds the breadcrumb trail for the current location, Dashboard-rooted. */
export function buildCrumbs(pathname: string): Crumb[] {
  const crumbs: Crumb[] = [{ label: 'Dashboard', path: '/' }];
  let accumulated = '';
  for (const segment of pathname.split('/').filter(Boolean)) {
    accumulated += `/${segment}`;
    crumbs.push({
      label: labelForPath(accumulated) ?? humanize(segment),
      path: accumulated,
    });
  }
  return crumbs;
}
