import { useState, useEffect, useCallback, useRef } from 'react';
import type { MarketTick, Position } from '../types';
import { initialMarketTicks, positions as basePositions } from '../data/mockData';

export function useLiveMarketTicks(interval = 3000) {
  const [ticks, setTicks] = useState<MarketTick[]>(initialMarketTicks);

  useEffect(() => {
    const timer = setInterval(() => {
      setTicks(prev =>
        prev.map(tick => {
          const drift = (Math.random() - 0.5) * 0.4;
          const newPrice = Math.round((tick.price + drift) * 100) / 100;
          const change = Math.round((newPrice - (tick.price - tick.change)) * 100) / 100;
          const changePercent = Math.round((change / (newPrice - change)) * 10000) / 100;

          return {
            ...tick,
            price: newPrice,
            change,
            changePercent,
          };
        })
      );
    }, interval);

    return () => clearInterval(timer);
  }, [interval]);

  return ticks;
}

export function useLivePositions(portfolioId?: string, interval = 5000) {
  const [livePositions, setLivePositions] = useState<Position[]>(
    portfolioId
      ? basePositions.filter(p => p.portfolioId === portfolioId)
      : basePositions
  );

  const prevPrices = useRef<Record<string, number>>({});

  useEffect(() => {
    const filtered = portfolioId
      ? basePositions.filter(p => p.portfolioId === portfolioId)
      : basePositions;
    setLivePositions(filtered);
    filtered.forEach(p => {
      prevPrices.current[p.investmentId] = p.currentPrice;
    });
  }, [portfolioId]);

  useEffect(() => {
    const timer = setInterval(() => {
      setLivePositions(prev =>
        prev.map(pos => {
          const drift = (Math.random() - 0.48) * pos.currentPrice * 0.003;
          const newPrice = Math.round((pos.currentPrice + drift) * 100) / 100;
          const newMarketValue = Math.round(newPrice * pos.quantity * 100) / 100;
          const previousPrice = prevPrices.current[pos.investmentId] ?? pos.currentPrice;

          prevPrices.current[pos.investmentId] = pos.currentPrice;

          return {
            ...pos,
            previousPrice,
            currentPrice: newPrice,
            marketValue: newMarketValue,
            lastUpdated: new Date().toISOString(),
          };
        })
      );
    }, interval);

    return () => clearInterval(timer);
  }, [interval, portfolioId]);

  return livePositions;
}

export function useTotalPortfolioValue(livePositions: Position[]) {
  return livePositions.reduce((sum, p) => sum + p.marketValue, 0);
}

export function useAnimatedValue(targetValue: number, duration = 600) {
  const [displayValue, setDisplayValue] = useState(targetValue);
  const animationRef = useRef<number | null>(null);
  const startValueRef = useRef(targetValue);
  const startTimeRef = useRef<number | null>(null);

  const animate = useCallback((timestamp: number) => {
    if (startTimeRef.current === null) {
      startTimeRef.current = timestamp;
    }

    const elapsed = timestamp - startTimeRef.current;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);

    const current = startValueRef.current + (targetValue - startValueRef.current) * eased;
    setDisplayValue(current);

    if (progress < 1) {
      animationRef.current = requestAnimationFrame(animate);
    }
  }, [targetValue, duration]);

  useEffect(() => {
    startValueRef.current = displayValue;
    startTimeRef.current = null;

    if (animationRef.current) {
      cancelAnimationFrame(animationRef.current);
    }

    animationRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [targetValue, animate]);

  return displayValue;
}
