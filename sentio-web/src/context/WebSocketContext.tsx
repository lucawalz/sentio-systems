import React, { createContext, useContext, useEffect, useState, useCallback, useRef } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from './auth';

// API base URL for WebSocket connection
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
const WS_URL = `${API_BASE_URL}/ws`;

// Message types from backend
export type WebSocketMessageType =
    | 'DEVICE_REGISTERED'
    | 'DEVICE_UNREGISTERED'
    | 'WEATHER_UPDATED'
    | 'ALERTS_UPDATED';

export interface WebSocketMessage {
    type: WebSocketMessageType;
    timestamp: string;
    payload: Record<string, unknown>;
}

// Context value shape
interface WebSocketContextValue {
    connected: boolean;
    lastMessage: WebSocketMessage | null;
    // Subscribe to specific message types
    subscribe: (type: WebSocketMessageType, callback: (message: WebSocketMessage) => void) => () => void;
}

const WebSocketContext = createContext<WebSocketContextValue | undefined>(undefined);

export const WebSocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { loggedIn, user } = useAuth();
    const [connected, setConnected] = useState(false);
    const [lastMessage, setLastMessage] = useState<WebSocketMessage | null>(null);
    const clientRef = useRef<Client | null>(null);
    const subscribersRef = useRef<Map<WebSocketMessageType, Set<(message: WebSocketMessage) => void>>>(new Map());

    // Subscribe to specific message types
    const subscribe = useCallback((type: WebSocketMessageType, callback: (message: WebSocketMessage) => void) => {
        if (!subscribersRef.current.has(type)) {
            subscribersRef.current.set(type, new Set());
        }
        subscribersRef.current.get(type)!.add(callback);

        // Return unsubscribe function
        return () => {
            subscribersRef.current.get(type)?.delete(callback);
        };
    }, []);

    // Handle incoming messages
    const handleMessage = useCallback((message: IMessage) => {
        try {
            const wsMessage: WebSocketMessage = JSON.parse(message.body);
            console.log('[WebSocket] Received:', wsMessage.type, wsMessage.payload);
            setLastMessage(wsMessage);

            // Notify all subscribers for this message type
            const callbacks = subscribersRef.current.get(wsMessage.type);
            if (callbacks) {
                callbacks.forEach(cb => cb(wsMessage));
            }
        } catch (error) {
            console.error('[WebSocket] Failed to parse message:', error);
        }
    }, []);

    // Connect/disconnect based on auth state
    useEffect(() => {
        if (!loggedIn) {
            // Disconnect if not logged in
            if (clientRef.current?.active) {
                console.log('[WebSocket] Disconnecting (logged out)');
                clientRef.current.deactivate();
            }
            setConnected(false);
            return;
        }

        // Create STOMP client
        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            debug: (str) => {
                // Only log in development
                if (import.meta.env.DEV) {
                    console.log('[STOMP]', str);
                }
            },
        });

        client.onConnect = () => {
            console.log('[WebSocket] Connected');
            setConnected(true);

            // Subscribe to broadcast topics
            client.subscribe('/topic/weather', handleMessage);
            client.subscribe('/topic/alerts', handleMessage);

            // Subscribe to user-specific queue for device events
            if (user?.username) {
                client.subscribe(`/user/${user.username}/queue/devices`, handleMessage);
            }
        };

        client.onDisconnect = () => {
            console.log('[WebSocket] Disconnected');
            setConnected(false);
        };

        client.onStompError = (frame) => {
            console.error('[WebSocket] STOMP error:', frame.headers.message);
        };

        client.activate();
        clientRef.current = client;

        return () => {
            if (client.active) {
                client.deactivate();
            }
        };
    }, [loggedIn, user?.username, handleMessage]);

    return (
        <WebSocketContext.Provider value={{ connected, lastMessage, subscribe }}>
            {children}
        </WebSocketContext.Provider>
    );
};

export function useWebSocket() {
    const ctx = useContext(WebSocketContext);
    if (!ctx) {
        throw new Error('useWebSocket must be used within WebSocketProvider');
    }
    return ctx;
}

// Hook to subscribe to specific message types
export function useWebSocketSubscription(
    type: WebSocketMessageType,
    callback: (message: WebSocketMessage) => void
) {
    const { subscribe } = useWebSocket();

    useEffect(() => {
        const unsubscribe = subscribe(type, callback);
        return unsubscribe;
    }, [type, callback, subscribe]);
}

export default WebSocketProvider;
