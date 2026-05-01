import { io, Socket } from 'socket.io-client';

let socket: Socket | null = null;

export function getSocket(): Socket {
  if (!socket) {
    socket = io('/', {
      transports: ['websocket', 'polling'],
      autoConnect: true,
    });
  }
  return socket;
}

export function subscribeToPortfolio(portfolioId: string) {
  getSocket().emit('subscribe:portfolio', portfolioId);
}

export function unsubscribeFromPortfolio(portfolioId: string) {
  getSocket().emit('unsubscribe:portfolio', portfolioId);
}

export function subscribeToBatch() {
  getSocket().emit('subscribe:batch');
}

export function subscribeToSystem() {
  getSocket().emit('subscribe:system');
}
