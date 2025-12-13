import { get, post, del } from './api';

export interface Device {
    id: string;
    name: string;
    owners: string[];
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

    // Unregister a device
    unregisterDevice: (deviceId: string): Promise<void> =>
        del(`${DEVICES_ENDPOINT}/${deviceId}`),
};
