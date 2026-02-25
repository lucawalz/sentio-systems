import { get, post, del } from './api';

export interface Device {
    id: string;
    name: string;
    ownerId: string;
    createdAt: string;
    ipAddress?: string;
    lastSeen?: string;
}

const DEVICES_ENDPOINT = '/api/devices';

export const deviceService = {
    // Register a device
    registerDevice: (deviceId: string, name: string): Promise<Device> =>
        post<Device>(`${DEVICES_ENDPOINT}/register?deviceId=${deviceId}&name=${encodeURIComponent(name)}`, {}),

    // Get my devices
    getMyDevices: (): Promise<Device[]> =>
        get<Device[]>(`${DEVICES_ENDPOINT}`),

    // Check if user has any registered devices
    hasAnyDevices: (): Promise<boolean> =>
        get<boolean>(`${DEVICES_ENDPOINT}/has-any`),

    // Unregister a device
    unregisterDevice: (deviceId: string): Promise<void> =>
        del(`${DEVICES_ENDPOINT}/${deviceId}`),
};
