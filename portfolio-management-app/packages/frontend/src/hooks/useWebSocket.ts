import { useEffect, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getSocket } from '../lib/socket';
import toast from 'react-hot-toast';
import type { Transaction } from '../types';

export function useWebSocket() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const socket = getSocket();

    socket.on('transaction:created', (txn: Transaction) => {
      toast.success(`New transaction: ${txn.transactionType} ${txn.investmentId.trim()}`);
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
    });

    socket.on('position:updated', () => {
      queryClient.invalidateQueries({ queryKey: ['positions'] });
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
    });

    socket.on('batch:progress', (data: { step: string; progress: number }) => {
      toast.loading(`Batch: ${data.step} (${data.progress}%)`, { id: 'batch-progress' });
    });

    socket.on('batch:completed', () => {
      toast.success('Batch processing completed', { id: 'batch-progress' });
      queryClient.invalidateQueries({ queryKey: ['batch'] });
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    });

    socket.on('batch:failed', (data: { error: string }) => {
      toast.error(`Batch failed: ${data.error}`, { id: 'batch-progress' });
      queryClient.invalidateQueries({ queryKey: ['batch'] });
    });

    socket.on('system:alert', (data: { message: string }) => {
      toast(data.message, { icon: 'warning' });
    });

    return () => {
      socket.off('transaction:created');
      socket.off('position:updated');
      socket.off('batch:progress');
      socket.off('batch:completed');
      socket.off('batch:failed');
      socket.off('system:alert');
    };
  }, [queryClient]);

  const subscribePortfolio = useCallback((id: string) => {
    getSocket().emit('subscribe:portfolio', id);
  }, []);

  const unsubscribePortfolio = useCallback((id: string) => {
    getSocket().emit('unsubscribe:portfolio', id);
  }, []);

  return { subscribePortfolio, unsubscribePortfolio };
}
