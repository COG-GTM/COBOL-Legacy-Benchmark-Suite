import { useMemo } from 'react';

interface MiniSparklineProps {
  data: number[];
  width?: number;
  height?: number;
  color?: string;
}

export default function MiniSparkline({
  data,
  width = 80,
  height = 28,
  color,
}: MiniSparklineProps) {
  const pathD = useMemo(() => {
    if (data.length < 2) return '';

    const min = Math.min(...data);
    const max = Math.max(...data);
    const range = max - min || 1;
    const padding = 2;
    const effectiveHeight = height - padding * 2;
    const stepX = width / (data.length - 1);

    return data
      .map((val, i) => {
        const x = i * stepX;
        const y = padding + effectiveHeight - ((val - min) / range) * effectiveHeight;
        return `${i === 0 ? 'M' : 'L'}${x},${y}`;
      })
      .join(' ');
  }, [data, width, height]);

  const isPositive = data.length >= 2 && data[data.length - 1] >= data[0];
  const strokeColor = color ?? (isPositive ? '#10b981' : '#ef4444');

  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
      <path
        d={pathD}
        fill="none"
        stroke={strokeColor}
        strokeWidth={1.5}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
