export type SortState<K extends string> = { key: K; dir: 'asc' | 'desc' };

export function nextSort<K extends string>(sort: SortState<K>, key: K, textKeys: K[]) {
  if (sort.key === key) return { key, dir: sort.dir === 'asc' ? 'desc' : 'asc' } as const;
  return { key, dir: textKeys.includes(key) ? 'asc' : 'desc' } as const;
}
